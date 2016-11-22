package com.winterwell.datalog;

import java.io.Closeable;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Pattern;

import winterwell.maths.stats.distributions.d1.MeanVar1D;
import winterwell.maths.timeseries.Datum;
import winterwell.maths.timeseries.ExtraDimensionsDataStream;
import winterwell.maths.timeseries.ExtraDimensionsDataStream.KMatchPolicy;
import winterwell.maths.timeseries.IDataStream;
import com.winterwell.utils.ReflectionUtils;
import winterwell.utils.Utils;
import winterwell.utils.containers.Containers;
import winterwell.utils.containers.Pair2;
import winterwell.utils.io.SqlUtils;
import winterwell.utils.reporting.Log;
import winterwell.utils.reporting.Log.KErrorPolicy;
import winterwell.utils.time.Dt;
import winterwell.utils.time.Period;
import winterwell.utils.time.TUnit;
import winterwell.utils.time.Time;
import winterwell.utils.time.TimeUtils;

import com.winterwell.datalog.Stat.KInterpolate;
import com.winterwell.utils.Printer;
import com.winterwell.utils.threads.FixedFuture;
import com.winterwell.utils.threads.IFuture;

/**
 * Vital Statistics: A data-logging & charting service for arbitrary realtime stats.
 *
 * This implementation is backed by a single .csv file per week, holding all the stats.
 * Depot is used to give slice-of-time getData features.
 *
 * TODO an SQL backed version
 * TODO a datacube is what really makes sense, but I couldn't find a nice Java-friendly one.
 *
 * TODO possibly have an index-based version as well as tag-based
 * for extra speed.
 *
 * TODO calculate correlations & other stats for the top 100 tags??
 *
 * @testedby {@link StatImplTest}
 * @author daniel
 * @see Stat
 */
public class StatImpl implements Closeable, IStat {

	@Override
	public StatConfig getConfig() {
		return config;
	}

	@Override
	public synchronized void setListener(IListenStat listener, String... tagBits) {
		String tag = Stat.tag(tagBits);
		HashMap<String , IListenStat> map = new HashMap<String , IListenStat>(listeners);
		map.put(tag, listener);
		listeners = map;
	}

	public Map<String, IListenStat> getListeners() {
		return Collections.unmodifiableMap(listeners);
	}

	@Override
	public synchronized void removeListener(String... tagBits) {
		String tag = Stat.tag(tagBits);
		HashMap<String , IListenStat> map = new HashMap<String , IListenStat>(listeners);
		map.remove(tag);
		listeners = map;
	}

	HashMap<String,IListenStat> listeners = new HashMap<String , IListenStat>(0);


	@Deprecated // A test only method??
	public void save(Period period, Map<String, Double> _tag2count, Map<String, MeanVar1D> tag2mean) {
		storage.save(period, _tag2count, tag2mean);
	}

	@Override
	public IFuture<IDataStream> getData(Pattern id, Time start, Time end) {
		return storage.getData(id, start, end);
	}
	
	@Override
	public IFuture<MeanRate> getMean(Time start, Time end, String... tagBits) {
		String tag = Stat.tag(tagBits);
		return new FixedFuture(getMean(tagBits)); // Wrong! But returns plausible lies
//		FIXME return storage.getMean(start, end, tag);
	}

	@Override
	public StatReq<IDataStream> getData(Time start, Time end, KInterpolate fn, Dt bucketSize, String... tagBits) {
		String tag = Stat.tag(tagBits);
		return storage.getData(tag, start, end, fn, bucketSize);
	}
	
	@Override
	public StatReq<IDataStream> getMeanData(Time start, Time end, KInterpolate fn, Dt bucketSize, String... tagBits) {
		String tag = Stat.tag(tagBits);
		return storage.getMeanData(tag, start, end, fn, bucketSize);
	}

	@Override
	public IStatReq<Double> getTotal(Time start, Time end, String... tagBits) {
		String tag = Stat.tag(tagBits);
		return storage.getTotal(tag, start, end);
	}

	/**
	 *
	 * @param tag
	 * @param end Can be null (which does mean "include the current bucket")
	 * @return null if end doesn't imply "include the current bucket"
	 */
	Datum currentBucket(String tag, Time end) {
		Period bucket = getCurrentBucket();
		if (end != null && ! end.isAfter(bucket.first)) return null;
		// TODO What if the bucket overlaps with the loaded data? This can happen if flush is used
		Rate x = Stat.get(tag);
		Time t = doSave3_time(bucket);
		return new Datum(t, x.x, tag);
	}

	/**
	 * This was causing crashes, because datum list wasn't ordered by date.
	 * @param tag
	 * @param start can be null (which means include all historic)
	 * @param end can be null (which means include all historic)
	 * @return a list of historic data, currently stored in {@link #tagTime2count} map.
	 */
	List<Datum> currentHistoric(String tag, Time start, Time end) {
		List<Datum> data = new ArrayList<Datum>();
		// thread safety: grab a reference to the now
		final ConcurrentMap<Pair2<String, Time>, Double> _tag2time2count = tagTime2count;
		for (Pair2<String, Time> key : _tag2time2count.keySet()) {
			assert key != null;
			if ( ! key.first.equals(tag)) continue;
			// filter on time
			if (start != null && key.second.isBefore(start)) continue;
			if (end != null && key.second.isAfter(end)) continue;

			Datum datum = new Datum(key.second, _tag2time2count.get(key), tag);
			data.add(datum);
		}
		// Sorting is important because ListDataStream may get data out of order. see #4988
		Collections.sort(data);
		return data;
	}


	static Time doSave3_time(Period period) {
		return period.getMiddle();
	}

	KErrorPolicy onError = KErrorPolicy.REPORT;

	@Override
	public  Set<String> getLive() {
		Set<String> keys = tag2count.keySet();
		Set<String> keys2 = tag2dist.keySet();
		HashSet<String> set = new HashSet<String>(keys);
		set.addAll(keys2);
		return set;
	}

	/**
	 * What's happening now?
	 */
	ConcurrentMap<String,Double> tag2count = newMap();

	ConcurrentMap<String,MeanVar1D> tag2dist = newMap();

	ConcurrentMap<String,String> tag2event = new ConcurrentHashMap<String, String>(4, 0.75f, 2);

	/**
	 * Map for holding historical data of the form tag+time -> count 
	 * key: (tag,time)
	 * value: a delta to apply to stored value for this time-bucket.
	 */
	ConcurrentMap<Pair2<String, Time>, Double> tagTime2count = newMap();
	
	ConcurrentMap<Pair2<String, Time>, Double> tagTime2set = newMap();

	private boolean init;

	static Timer saveThread;

	final StatConfig config;
	final IStatStorage storage;

	/**
	 * IStatStorage defaults to SQLStorage.
	 *
	 * WARNING: StatReq uses Stat.dflt, so be careful when using this constructor to avoid current bucket data loss.
	 * Better use Stat.dflt.
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 */
	public StatImpl() {
		this(new StatConfig());
	}

	public StatImpl(StatConfig config) {
		this.config = config;
		// Create the storage
		if (config.storageClass == null) {
			config.storageClass = SQLStorage.class;
		}
		try {
			Constructor<?> cons = config.storageClass.getConstructor(StatConfig.class);
			storage = (IStatStorage) cons.newInstance(config);
		} catch (Exception ex) {
			throw Utils.runtime(ex);
		}
		// init
		init();
	}

	private void init() {
		if (init) return;
		init = true;
		// give nice slicing
		Time now = new Time().plus(10, TUnit.SECOND); // allow for a slow-ish init
		// Note: this is also the soonest that the first save can occur.
		Time first = new Time(now.getYear(), now.getMonth(), now.getDayOfMonth(), now.getHour()+1, 0, 0);
		Time first2 = first;
		while(first2.isAfter(now)) {
			first = first2;
			first2 = first.minus(config.interval);
		}
		// shutdown the old
		if (saveThread!=null) {
			Log.e(Stat.LOGTAG, "WTF reinitialising stat?");
			saveThread.cancel();
		}
		// start-up the new
		saveThread = new Timer("DataLog.save", true);
		saveThread.scheduleAtFixedRate(new SystemStatsTask(), first.getDate(), config.interval.getMillisecs());
		Log.i(Stat.LOGTAG, "1st save at "+first+" ("+TimeUtils.toString(new Time().dt(first))+")");
	}

	/**
	 * Bucket start
	 */
	Time start = new Time();

	@Override
	public String label(String label, String... tagBits) {
		String tag = Stat.tag(tagBits);
		// TODO Auto-generated method stub
		return null;
	}

	protected synchronized void doSave() {
		Map<String, Double> old = tag2count;		
		Map<String, MeanVar1D> oldMean = tag2dist;
		Map<Pair2<String, Time>, Double> oldTagTimeCount = tagTime2count;
		Map<Pair2<String, Time>, Double> oldTagTimeSet = tagTime2set;
		old.put("stat_bucket_count", 1.0*old.size());
		old.put("stat_bucket_dist", 1.0*oldMean.size());
		old.put("stat_bucket_retro_count", 1.0*oldTagTimeCount.size());
		old.put("stat_bucket_retro_set", 1.0*oldTagTimeSet.size());
		Period period = getCurrentBucket();

		// new buckets
		tag2count = newMap();
		tag2dist = newMap();
		tag2event =  new ConcurrentHashMap<String, String>(4, 0.75f, 2);
		tagTime2count = newMap();
		tagTime2set = newMap();
		// Advance the bucket time?
		Time now = new Time();
		if (period.second.isAfter(now)) {
			// Nope -- this is a premature flush
			Log.d(Stat.LOGTAG, "save but no advance.");
		} else {
			start = period.second;
		}
		// save the counters!
		Log.d(Stat.LOGTAG, "Saving "+old.size()+" simple + "+oldMean.size()+" dist + "+oldTagTimeCount.size()+" historical "+oldTagTimeSet.size()+"...");
		storage.save(period, old, oldMean);
		storage.saveHistory(oldTagTimeCount);
		storage.setHistory(oldTagTimeSet);
	}

	Period getCurrentBucket() {
		Dt dt = getPeriod();
		return new Period(start, start.plus(dt));
	}

	private boolean closed;

	private <X, Y> ConcurrentMap<X, Y> newMap() {
		ConcurrentHashMap<X, Y> cc = new ConcurrentHashMap<X, Y>();
		return cc;
	}

	public static final class ClosedException extends IllegalStateException {
		private static final long serialVersionUID = 1L;
	}

	public boolean isClosed() {
		return closed;
	}

	@Override
	public  void set(double x, Object... tagBits) {
		if (closed) throw new ClosedException();
//		init();
		String tag = Stat.tag(tagBits);
		tag2count.put(tag, x);

		// track our own size
		if ( ! tag.startsWith("Stat")) {
			mean(tag2count.size(), "Stat", "counts");
		}
	}

	@Override
	public void count(double dx, Object... tagBits) {
		assert ! closed;
		assert tagBits.length != 0 : dx;
		if (dx==0) return;
		// loop over tag.heiriarchy
		StringBuilder tag = new StringBuilder();
		for(Object tg : tagBits) {
			if (tg == null) throw new IllegalArgumentException(Printer.toString(tagBits));

			String stag = parseTag(tg, tag);
			assert tag2count != null : stag;

			double x = Containers.plus(tag2count, stag, dx);
			// alerts?
			IListenStat listener = listeners.get(stag);
			if (listener!=null) {
				try {
					listener.handleCount(x, dx, stag);
				} catch(Throwable ex) {
					// don't throw an exception here
					Log.e(Stat.LOGTAG, ex);
				}
			}
		}
		// track our own size
		if ( ! "Stat".equals(tagBits[0])) {
			mean(tag2count.size(), "Stat", "counts");
		}
//		return new Rate(x, getPeriod(stag), stag);
	}

	@Override
	public  void mean(double x, Object... tagBits) {
//		init();
		assert ! closed;
		// loop over tag.hieriarchy
		StringBuilder tag = new StringBuilder();
		for(Object tg : tagBits) {
			String stag = parseTag(tg, tag);

			MeanVar1D dist = tag2dist.get(stag);
			if (dist==null) {
				dist = new MeanVar1D(); // use mean-var
				tag2dist.put(stag, dist);
			}
			dist.train1(x);
		}

		// track our own size
		if ( ! "Stat".equals(tagBits[0])) {
			mean(tag2dist.size(), "Stat", "means");
		}
	}

	@Override
	public MeanRate getMean(String... tagBits) {
		String tag = Stat.tag(tagBits);
		return new MeanRate(tag2dist.get(tag), getPeriod());
	}

	@Override
	public Rate get(String... tags) {
		// make the tag
		String stag = Stat.tag(tags);
		Double v = tag2count.get(stag);
		if (v != null) {
			return new Rate(v, getPeriod(), stag);
		}
		// Perhaps we track the mean/var? -- use the mean
		MeanVar1D mv = tag2dist.get(stag);
		return mv==null? Rate.ZERO(stag) : new Rate(mv.getMean(), getPeriod(), stag);
	}

	@Override
	public  Dt getPeriod() {
		return config.interval;
	}


	@Override
	public final void flush() {
		doSave();
	}

	@Deprecated
	@Override
	public final void close() {
		Log.i(Stat.LOGTAG, "CLOSE!");
		flush();
		closed = true;
		tag2count = null;
		saveThread.cancel();
		saveThread = null;
	}

	@Override
	public Collection<String> getActiveLabels() {
		ArrayList<String> labels = new ArrayList<String>();
		labels.addAll(tag2count.keySet());
		labels.addAll(tag2dist.keySet());
		return labels;
	}


	@Override
	public void setEvent(String event, String... tagBits) {
		if (closed) throw new ClosedException();
		if (Utils.isBlank(event)) {
			throw new IllegalArgumentException(event+" for "+Stat.tag(tagBits));
		}
		// loop over tag.heiriarchy
		StringBuilder tag = new StringBuilder();
		for(Object tg : tagBits) {
			String stag = parseTag(tg, tag);
			tag2event.put(stag, event);
		}
	}

	/**
	 * Convenience method for converting a list of Future data-streams into one data-stream in the
	 * here & now.
	 * Uses KMatchPolicy.USE_PREVIOUS_VALUE_ON_MISMATCH
	 * @param datas
	 * @return
	 */
	public static IDataStream exec(List<? extends IFuture<? extends Iterable>> datas) {
		List<IDataStream> strms = new ArrayList(datas.size());
		for (IFuture<? extends Iterable> iFuture : datas) {
			IDataStream data = (IDataStream) iFuture.get();
			strms.add(data);
		}
		ExtraDimensionsDataStream all = new ExtraDimensionsDataStream(KMatchPolicy.USE_PREVIOUS_VALUE_ON_MISMATCH, strms);
		return all;
	}


	/**
	 * This writes synchronously in the database. It first removes pairs of <tag, time> from
	 * the cache if they exist.
	 *
	 * TODO batch and save like other stats
	 */
	@Override
	public void set(Time at, double x, Object... tags) {
		if (closed) throw new ClosedException();
		// TODO In the current bucket? But this has possible bugs of repeated sets creating two entries due to time-bucketing.
//		if (getCurrentBucket().contains(at)) {
//			set(x, tags);
//			return;
//		}

		String stag = Stat.tag(tags);					
		Pair2<String, Time> tag2time = new Pair2<String, Time>(stag, at);
		tagTime2count.remove(tag2time);
		tagTime2set.put(tag2time, x);
	}

	@Override
	public void count(Time at, double dx, Object... tags) {
		assert ! closed;
		assert tags.length != 0 : dx;

		if (dx == 0) return;

		// loop over tag.hierarchy
		StringBuilder tag = new StringBuilder();
		for(Object tg : tags) {
			if (tg == null) throw new IllegalArgumentException(Printer.toString(tags));

			String stag = parseTag(tg, tag);

			assert tagTime2count != null : stag;

			Pair2<String, Time> key = new Pair2<String, Time>(stag, at);
			Containers.plus(tagTime2count, key, dx);
		}
	}

	/**
	 * Helper method to construct tag based on tag hierarchy convention
	 * @param tg
	 * @param tag
	 * @return tag.toString(), after appending tg
	 */
	private String parseTag(Object tg, StringBuilder tag) {
		if (tag.length() != 0) {
			tag.append(Stat.HIERARCHY_CHAR);
		}
		
		// TODO Defence against likely bug after v14c branches off
//		if (tg instanceof String[]) {
//			TODO throw new IllegalArgumentException(Printer.toString(tg)+" onto "+tag);
//		}
		
		tg = Stat.tag2_escape(tg.toString());
		tag.append(tg);

		String stag = tag.toString();
		assert ! stag.isEmpty();

		return stag;
	}
	

	class SystemStatsTask extends TimerTask {
		@Override
		public void run() {
			try {
				if (closed) cancel();
				else doSave();
				// heart beat: check things are working by storing some useful stats
				Stat.set(ReflectionUtils.getUsedMemory(), STAT_MEM_USED);
				Stat.set(ReflectionUtils.getAvailableMemory(), "mem_free");
				
				int[] info = SqlUtils.getPostgresThreadInfo("sodash");
				Stat.set(info[0], "postgres_sodash_processes");
				Stat.set(info[1], "postgres_sodash_idle");
			} catch(Throwable t) {
				try {
					Log.e(Stat.LOGTAG, t);
				}
				catch(Throwable ex) {
					ex.printStackTrace(); // Yeah, this is ridiculous, but we're still seeing outages
				}
			}
		}
	}

}
