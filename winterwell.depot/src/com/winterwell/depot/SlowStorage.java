package com.winterwell.depot;

import java.io.Closeable;
import java.io.File;
import java.io.Flushable;
import java.io.IOException;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.winterwell.datalog.DataLog;
import com.winterwell.utils.ReflectionUtils;
import com.winterwell.utils.Utils;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.threads.Actor;
import com.winterwell.utils.threads.SlowActor;
import com.winterwell.utils.time.Dt;
import com.winterwell.utils.time.TUnit;
import com.winterwell.utils.time.TimeUtils;

/**
 * A write-behind system -- shove stuff in, & it doesn't get saved for a while.
 * <p>
 * NB: Often, an artifact will be repeatedly modified. Such artifacts will get saved
 * once every {@link #delay}
 * <p>
 * Use-case: efficient database, network, & file-system access. 
 * @author daniel
 *
 * @param <K>
 * @param <V>
 */
public class SlowStorage extends SlowActor<Desc> 
implements IStore , Flushable, Closeable 
{
	
	@Override
	public String getRaw(Desc desc) {
		return base.getRaw(desc);
	}

	@Override
	public String toString() {
		return "SlowStorage[base=" + base + ", delay=" + delay + "]";
	}

	public SlowStorage(IStore base, Dt delay, Depot depot) {
		this.base = base;
		this.delay = delay;
		this.depot = depot;
	}

	final IStore base;
	
	@Override
	public File getLocalPath(Desc desc) throws UnsupportedOperationException {	
		return base.getLocalPath(desc);
	}
	
	@Override
	public void close() throws IOException {
		flush();
	}
	
	Dt delay;

	/**
	 * marker for "yes it should be null"
	 */
	static final Object NULL = new Object();

	private static final String LOGTAG = "SlowStorage";

	private static final int MB = (int) Math.pow(2, 20); // 1k * 1k -- a big megabyte
	
	final ConcurrentHashMap<Desc, Object> map = new ConcurrentHashMap();
	
	@Override
	public void flush() throws IOException {
		// send it all through receive
		Queue<Packet<Desc>> _q = getQ();
		for (Packet<Desc> packet : _q) {
			receive(packet.msg, packet.from);
		}
	}
	
	@Override
	public final void remove(Desc desc) {
		// put NULL into map
		put(desc, NULL);
	}

	@Override
	public boolean contains(Desc desc) {
		Object v = map.get(desc);
		if (v!=null) {
			return v==NULL? false : true;
		}
		return base.contains(desc);
	}

	@Override
	public final <X> void put(Desc<X> desc, X artifact) {
		map.put(desc, artifact);
		// Note: Races don't matter here -- the message is just "do something with desc"
		
		Dt dt = delay;
		// Can we afford a long delay? 
		// A crude defence against out-of-memory issues.
		// TODO a SoftReference based map (with save-on-evict) might be better. 
		long freeMem = ReflectionUtils.getAvailableMemory();		
		if (freeMem < 20 * MB) { // 20mb
			Runtime rt = Runtime.getRuntime();
			long totalMem = rt.totalMemory();
//			int s = getQ().size();
//			int m = map.size();
			Log.w(LOGTAG, "Instant storage with "+(freeMem/MB)+"mb of "+(totalMem/MB)+"mb");
			dt = TimeUtils.NO_TIME_AT_ALL; //TUnit.MILLISECOND.dt;
		} if (freeMem < 100 * MB) { // 100mb
//			int s = getQ().size();
//			int m = map.size();
			Runtime rt = Runtime.getRuntime();
			long totalMem = rt.totalMemory();
//			Log.w(LOGTAG, "Fast storage with "+(freeMem/MB)+"mb of "+(totalMem/MB)+"mb");
			dt = TUnit.SECOND.dt;
		}
		// jitter
		if (delayJitter!=0) {
			double jitter = 1 + ((Utils.getRandom().nextDouble() - 0.5) * delayJitter);
			assert jitter > 0 && jitter < 2 : jitter;
			dt = dt.multiply(jitter);
		}
		// post ourselves a note to deal with it
		sendDelayed(desc, null, dt);		
	}

	@Override
	public final <X> X get(Desc<X> desc) {
		Object v = map.get(desc);
		if (v!=null) {
			DataLog.count(1,"Depot","cache_hit","SlowStorage");
			return v==NULL? null : (X) v;
		}
		return base.get(desc);
	}

	@Override
	public Set<Desc> loadKeys(Desc partialDesc) {
//		// TODO scan the q for matches
//		HashSet matches = new HashSet();
//		for(Desc s : map.keySet()) {
//			if (partialDesc.isMatch)
//		}
		Set<Desc> matches = base.loadKeys(partialDesc);
		return matches;
	}

	@Override
	public MetaData getMetaData(Desc desc) {
		return base.getMetaData(desc);
	}

	@Override
	protected void receive(Desc desc, Actor sender) {
		// The messages slowly sent are the Descs for the items to save, whilst the items themselves are stashed in map.
		// Remove any other requests for msg
		for(Packet p : getQ().toArray(new Packet[0])) {
			if (desc.equals(p.msg)) {
				getQ().remove(p);
			}
		}
		// Save or remove?
		Object v = map.get(desc); 
		// NB: We only modify the map at the end, and only if it stays the same
		if (v==null) {
			Log.w(LOGTAG, "null?! Artifact already saved in race? "+desc);
			// nothing to do
		} else if (v==NULL) {
			base.remove(desc);
		} else {
			// merge? NB: dec.before is normally null
			if (desc.getBefore() != null) {
				Object latest = base.get(desc);
				if (latest!=null) {
					Object vMerge = depot.merger.doMerge(desc.getBefore(), v, latest);
					v = vMerge;
					// update the binding
					desc.bind(vMerge);
				}
			}
			
			// Store it!
			base.put(desc, v);
			
			// take a new snapshot for further updates?
			if (desc.getBefore() != null) {
				desc.remarkForMerge();
			}
		}
		// Modify the map if as expected. 
		// Do nothing to the map if someone has just reset a fresh value.
		map.remove(desc, v);
	}

	Depot depot;

	/**
	 * Add some randomness to the delay -- to help avoid clashes between servers/processes.
	 */
	private double delayJitter;
	
	public IStore getBase() {
		return base;
	}

	public void setDelay(Dt dt) {
		this.delay = dt;
	}

	public void setDelayJitter(double writeBehindJitter) {
		this.delayJitter = writeBehindJitter;
	}

}
