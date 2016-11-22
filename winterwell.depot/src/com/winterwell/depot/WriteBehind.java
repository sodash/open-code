package com.winterwell.depot;

import java.io.Closeable;
import java.io.Flushable;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;

import com.winterwell.utils.containers.Pair2;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.time.Dt;
import com.winterwell.utils.time.StopWatch;
import com.winterwell.utils.time.Time;

import com.winterwell.datalog.Stat;

/**
 * A {@link WriteBehind} system -- shove stuff in, & it doesn't get saved
 * for a while.
 * 
 * TODO this saves & removes based on the insertion time.
 * It'd be better to save based on insertion (so risk of data loss is limited)
 * but remove based on modification time (so it works good as a cache)
 * <p>
 * Use-case: efficient database, network, & file-system access. 
 * @author daniel
 *
 * @param <K>
 * @param <V>
 * @deprecated Use {@link SlowStorage} instead.
 */
public abstract class WriteBehind<K, V> implements Flushable, Closeable {

	@Override
	public void close() {
		flush();
	}
	
	V getFromQueue(K key) {
		Pair2<V, Time> vt = queue.get(key);
		if (vt!=null) return vt.first;
		return null;
	}
	
	
	/**
	 * Save all edits in the queue.
	 * <p>
	 * Keeps going until the queue is truly empty.
	 * This means that if a store() action leads to further
	 * items getting queued, then these too will be flushed.
	 * However, a given object will only be saved once per flush.
	 * <p>
	 * If another thread keeps inserting
	 * edits, we may never finish!
	 */
	public final void flush() {
		synchronized (queue) {
			Map<K,V> flushed = new HashMap();
			while( ! queue.isEmpty()) {
				Map<K, V> edits = new HashMap();				
				for(Map.Entry<K,Pair2<V,Time>> e : queue.entrySet()) {					
					V v = e.getValue().first;
					if (v==null) continue;
					edits.put(e.getKey(), v);
				}		
				for(K k : edits.keySet()) {
					queue.remove(k);
				}
				
				// don't repeat storage
				for(K k : flushed.keySet()) {
					edits.remove(k);	
				}				
				flushed.putAll(edits);
				
				store(edits);
			}
		}	
	}
	
	private final Map<K,Pair2<V,Time>> queue = new HashMap();
	
	Timer timer;

	private Dt period;
	
	@Deprecated // For test use only for now
	public void setPeriod(Dt period) {
		this.period = period;
		if (timer!=null) timer.cancel();
		timer = new Timer("Timer-"+this, true);
		timer.schedule(new TimerTask() {			
			@Override
			public void run() {
				writeOld();
			}
		}, 1000, period.getMillisecs()/2);		
	}
	
	public final V get(K key) {
		synchronized (queue) {
			Pair2<V, Time> vt = queue.get(key);
			// Note: if remove was called, we may (correctly) return null here
			if (vt!=null) {
				Stat.count(1, "WriteBehind_hit");
				return vt.first;
			}
		}
		Stat.count(1, "WriteBehind_miss");
		return getFromStorage(key);
	}
	
	protected V getFromStorage(K key) {
		return null;
	}

	/**
	 * @param period target time to wait before storing an item.
	 * Actual delays can be 50% longer.
	 * The checking interval is period/2
	 */
	public WriteBehind(Dt period) {
		Log.d("writebehind", "Starting with period "+period);
		assert period.getMillisecs() > 100;
		this.period = period;
		timer = new Timer("Timer-"+this, true);
		timer.schedule(new TimerTask() {			
			@Override
			public void run() {
				writeOld();
			}
		}, 1000, period.getMillisecs()/2);
	}

	protected final void writeOld() {
		StopWatch sw = new StopWatch();
		Time stillYoung = new Time().minus(period);
		Map<K,V> old = new HashMap();
		synchronized (queue) {
			Object[] keys = queue.keySet().toArray();			
			
			for (Object key : keys) {
				Pair2<V, Time> vt = queue.get(key);				
				if (vt.second.isBefore(stillYoung)) {
					// paranoia
					if (vt.first==null) continue;
					// save it		
					old.put((K)key, vt.first);					
					queue.remove(key);
				}				
			}
		}
		
		if (old.isEmpty()) {
			Log.v("writebehind", "store:0? keep:"+size());
			return;
		}
		try {
			Log.d("writebehind", "storing "+old.size()+"...");
			store(old);
			Log.d("writebehind", "stored:"+old.size()+" keep:"+size()+"\t"+sw);
		} catch(Throwable ex) {			
			Log.report("writebehind", ex, Level.SEVERE);
		}
	}
	
	/**
	 * @param edits May contain null values for "remove this key"
	 */
	// Do we have a threading issue wrt timer-driven calls versus flush()?
	protected abstract void store(Map<K,V> edits);
	
	/**
	 * This puts a null value into the queue, 
	 * which should lead to a remove in due course when {@link #store(Map)} is called.
	 * @param key
	 * @see 
	 */
	public final void remove(K key) {
		put(key, null);
	}
	
	/**
	 * Remove the key/value mapping from the in-memory queue.
	 * <p>
	 * WARNING: This will also remove null values (ie. delayed delete commands)
	 * @param key
	 * @return the old in-queue value
	 */
	V removeFromQueue(K key) {
		Pair2<V, Time> vt = queue.remove(key);
		if (vt==null) return null;
		if (vt.first==null) { 
			Log.w("WriteBehind", "Removing del op for "+key);
		}
		return vt.first;
	}
	
	/**
	 * @param key
	 * @param value Can be null
	 */
	public final void put(K key, V value) {
		assert key != null : value;
		synchronized (queue) {
			// preserve the old timestamp if already queued
			//  -- so that a repeatedly edited value does get saved regularly
			Pair2<V, Time> old = queue.get(key);
			Time t = old==null? new Time() : old.second;
			
			Pair2<V, Time> vt = new Pair2(value, t);				
			queue.put(key, vt);
		}
		
		// TODO delete memory leak hunt
		Stat.set(queue.size(),"mem", "WriteBehind", toString());
	}

	/**
	 * @return Queue size
	 */
	public int size() {
		return queue.size();
	}

	public Dt getPeriod() {
		return period;
	}
	
}
