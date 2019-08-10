package com.winterwell.datalog;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import com.winterwell.datalog.DataLog.KInterpolate;
import com.winterwell.depot.Desc;
import com.winterwell.maths.timeseries.Datum;
import com.winterwell.utils.Printer;
import com.winterwell.utils.Utils;
import com.winterwell.utils.containers.Cache;
import com.winterwell.utils.containers.ListMap;
import com.winterwell.utils.io.CSVReader;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.time.Dt;
import com.winterwell.utils.time.Period;
import com.winterwell.utils.time.TUnit;
import com.winterwell.utils.time.Time;
import com.winterwell.utils.time.TimeUtils;

/**
 * An CSV implementation of the StatReq.
 * @testedby {@link StatReqCSVTest}
 * @author daniel
 * @param <X>
 */
public class StatReqCSV<X> extends StatReq<X> {
	
	/**
	 * value can be Double or ListDataStream
	 */
	protected static Cache<StatReq, Object> cache = new Cache(200).setStats("Stat");

	
	/**
	 * Keep fetches single-threaded. It's easier & avoids a big fetch holding up a small one.
	 * Within a thread, all the fetches do need to finish anyway.
	 */
	static final ThreadLocal<List<StatReq>> todo = new ThreadLocal<List<StatReq>>() {
		@Override
		protected List<StatReq> initialValue() {
			return new ArrayList();
		}
	}; 

	public StatReqCSV(String cmd, Pattern tagMatcher, Time start, Time end) {
		super(cmd, tagMatcher, start, end);
		// add it to the todos
		todo.get().add(this);
	}

	public StatReqCSV(String cmd, String tag, Time start, Time end, KInterpolate interpolate, Dt bucketSize) {
		super(cmd, tag, start, end, interpolate, bucketSize);
		// add it to the todos
		todo.get().add(this);
	}
	
	@Override
	protected void run() {
		runBatch();
	}
	
	private static void runBatch() {
		stat = (DataLogImpl) DataLog.dflt;
		List<StatReq> _batch = todo.get();
		// This can happen with repeated calls to the same get()
		if (_batch.isEmpty()) return;
		// copy & clear out (otherwise we get threading issues from the toString / debugger interaction)
		ArrayList<StatReq> batch = new ArrayList(_batch);
		_batch.clear();
		Log.d(DataLog.LOGTAG, "runBatch "+batch.size());
		
		// Do from cache
		List<StatReq> done = runBatch2_fromCache(batch);
		Log.d(DataLog.LOGTAG, "runBatch cache-hit: "+done.size()+" of "+batch.size());
		batch.removeAll(done);
		if (batch.isEmpty()) {
			return; // all done from cache :)
		}
		
		// Sort by server
		ListMap<String,StatReq> server2req = new ListMap();
		for (StatReq r : batch) {
			String s = r.server == null? Desc.MY_SERVER() : r.server; // MY_SERVER() not LOCAL!
			server2req.add(s, r);
		}
		
		for(String _server : server2req.keySet()) {
			List<StatReq> batch2 = server2req.get(_server);
			try {
				runBatch2_(_server, batch2);
			} catch(Throwable ex) {
				Log.w(DataLog.LOGTAG, ex);
				// "give" this error to all affected
				for (StatReq statReq : batch2) {
					statReq.error = ex;
				}
			}
		}
	}
	
	private static void runBatch2_(String _server, List<StatReq> batch) {
		assert _server != null;
		// What's the period? Take the max.
		Time s = TimeUtils.DISTANT_FUTURE;
		Time e = TimeUtils.AD;
		for (StatReq batchGet : batch) {
			if (batchGet.start==null) s = new Time(0); // from 1970, which covers all our data
			else if (batchGet.start.isBefore(s)) s = batchGet.start;
			
			if (batchGet.end == null) e = new Time().plus(TUnit.HOUR); // include all the data, even stuff being written right now
			else if (batchGet.end.isAfter(e)) e = batchGet.end;
		}
		// OK - get the data!
		if (s.getTime() < 0) s = new Time(0); // from 1970, which covers all our data				
		assert e.isAfter(s) : s+" "+e;
		
		for(StatReq bg : batch) {
			initV(bg);
		}
		
		// Gather the data
		CSVReader r = (CSVReader) stat.storage.getReader(_server, s, e, null, null);
		if (r!=null) {
			try {
				runBatch2_fromCSV(batch, r);
			} finally {
				r.close();	
			}
		}	
										
		// save it -- before we add in the last bucket
		for(StatReq bg : batch) {
			if (bg.key!=null) cache.put(bg.key, bg.v);
		}

		// Add in the last bucket
		if (Desc.LOCAL_SERVER.equals(_server) || Desc.MY_SERVER().equals(_server)) {
			// Only if it's local -- otherwise we don't have access TODO a servlet giving remote acccess
			for(StatReq bg : batch) {
				addLatestBucket(bg);
			}		
		}
	}
	
	/**
	 * Try to complete a BatchGet from cache.
	 * 
	 * TODO what if we've just had a bucket switch? And the cache has most of the answer?		
	 * TODO We could partially fill in v
	 * 
	 * @param batch
	 * @return the ones which are now complete
	 */
	private static List<StatReq> runBatch2_fromCache(List<StatReq> batch) {
		List<StatReq> dones = new ArrayList();
		for (StatReq batchGet : batch) {
			boolean done = run_fromCache(batchGet);
			if ( ! done) continue;
			dones.add(batchGet);
		}
		return dones;
	}
	
	private static boolean run_fromCache(StatReq batchGet) {
		// TODO handle patterns 
		if (batchGet.tagMatcher!=null) return false;
		// Round off the end?
		// We do this to use cached-totals (whose end time shouldn't overlap the current bucket & can't be null).		
		Period bucket = stat.getCurrentBucket();
		Time shortEnd = batchGet.end==null? new Time() : batchGet.end;
		Time s = batchGet.start==null? new Time(0) : batchGet.start;
		if (shortEnd.isAfter(bucket.first)) {
			shortEnd = bucket.first;
			if (s.isAfter(shortEnd)) {
				Log.w(DataLog.LOGTAG, "wtf? "+shortEnd+"? ["+s+" - "+batchGet.end+"]");
				s = shortEnd;
			}
		}
		StatReq key = new StatReq(batchGet.cmd, batchGet.tag, batchGet.tagMatcher, s, shortEnd, batchGet.interpolate, batchGet.bucketSize);
		batchGet.key = key;
		Object v = cache.get(key);
		if (v==null) {
			return false;
		}
		// set the result
		batchGet.v = v;
		// Add in the latest bit?
		addLatestBucket(batchGet);
		return true;
	}
	
	private static void runBatch2_fromCSV(List<StatReq> batch, CSVReader r) {
		// timestamp, tag, value
		for (String[] row : r) {
			if (row.length < 3) {
				Log.w(DataLog.LOGTAG, "getData bogus row: "+Printer.toString(row));
				continue; // an error??
			}
			// Which if any claim it?
			// TODO minor efficiency: test 1st on tag to avoid parsing that Time & Double??
			try {
				Datum datum = new Datum(new Time(row[0]), Double.valueOf(row[2]), row[1]);
				whoClaims(batch, datum);				
			} catch (Throwable e) {
				switch(stat.onError) {
				case ASK:
				case ACCEPT:
				case DIE:
				case THROW_EXCEPTION: throw Utils.runtime(e);
				case IGNORE: continue;				
				}
				// report the error, but carry on
				// FIXME: we currently can get ordering errors around Stat flush & restart 
				Log.e(DataLog.LOGTAG, "row: "+Printer.toString(row)+": "+e);
			}
		}
	}

	/**
	 * Add this data-point to all relevant requests.
	 * @param batch
	 * @param datum
	 */
	private static void whoClaims(List<StatReq> batch, Datum datum) {
		String lbl = datum.getLabel().toString();
		for (StatReq bg : batch) {
			// Does it match the tag?
			if (bg.tag != null && ! lbl.startsWith(bg.tag)) continue;
			if (bg.tagMatcher != null && ! bg.tagMatcher.matcher(lbl).matches()) continue;
			// And the date?
			// Start is inclusive, end is exclusive
			if (bg.start!=null && bg.start.isAfter(datum.time)) continue;
			if (bg.end!=null && ! bg.end.isAfter(datum.time)) continue;
			// add it then
			add(bg, datum);
		}
	}
}
