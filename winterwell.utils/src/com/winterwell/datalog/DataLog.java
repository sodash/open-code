package com.winterwell.datalog;

import java.lang.reflect.Constructor;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import com.winterwell.utils.FailureException;
import com.winterwell.utils.Printer;
import com.winterwell.utils.StrUtils;
import com.winterwell.utils.Utils;
import com.winterwell.utils.Warning;
import com.winterwell.utils.containers.Containers;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.threads.IFuture;
import com.winterwell.utils.time.Dt;
import com.winterwell.utils.time.TUnit;
import com.winterwell.utils.time.Time;

/**
 * Vital Statistics: A data-logging & charting service for arbitrary realtime
 * stats.
 * <p>
 * The code behind this is in com.winterwell.datalog.StatImpl, but loaded
 * dynamically & used via the IStat interface to avoid a hard dependency.
 * <p>
 * Requirements: 1. Writes must be very cheap. 2. The keys aren't known in
 * advance. 3. Granularity: doesn't need to go finer than, say, 20 minutes. 4.
 * Cross-cluster is feasible, although latency can be high.
 *
 * TODO auto-calculate correlations & other stats for top 100 tags??
 *
 * <h3>Documenting Stat tags
 * <h3>
 * The code which specifies the tag(s) SHOULD include a comment STATID: tag
 * documentation or STATID: tag/subtag documentation to allow coders to find &
 * understand the definitions using a file-search.
 *
 * @see StatImpl
 * @author daniel
 *         <p>
 *         <b>Copyright & license</b>: (c) Winterwell Associates Ltd, all rights
 *         reserved. This class is NOT formally a part of the com.winterwell.utils
 *         library. In particular, licenses for the com.winterwell.utils library do
 *         not apply to this file.
 */
public class DataLog {

	/**
	 *
	 * @param tag
	 * @param listener
	 *            Probably an {@link Alert}
	 */
	public static void setListener(IListenDataLog listener, String... tagBits) {
		tagBits = check(tagBits);
		dflt.setListener(listener, tagBits);
	}

	public static Map<String, IListenDataLog> getListeners() {
		return dflt.getListeners();
	}
	/**
	 * What interpolation function to use. null (none) is the default.
	 */
	public static enum KInterpolate {
		/**
		 * Create ersatz start/end points using linear interpolation, & going
		 * back upto 1 day to find a previous start/end.
		 */
		LINEAR_1DAY,
		
		/**
		 * Not an interpolation function really. But skip any zero entries.
		 */
		SKIP_ZEROS
	}

	public static final char HIERARCHY_CHAR = '/';
	static final String LOGTAG = "DataLog";

	/**
	 * @return the tags with active counts in the current time-window.
	 */
	public static Set<String> getLive() {
		return dflt.getLive();
	}

	/**
	 * Flush: save state to disk / database.
	 * @deprecated As this may lead to anomalies in the stored data. Useful in testing though.
	 */
	public static void flush() {
		dflt.flush();
	}

	static IDataLog dflt = initDflt();

	private static IDataLog initDflt() {
		try {
			return (IDataLog) Class.forName("com.winterwell.datalog.StatImpl")
					.newInstance();
		} catch (Exception e) {
			// Bad!
			Log.e(LOGTAG, e.getMessage());
			// Let stuff continue without exceptions elsewhere
			return new DummyDataLog(e);
		}
	}

	/**
	 * Set a value for now.
	 *
	 * @param x
	 *            Replace the existing value
	 * @param tag
	 *            Use . for hierarchical tags
	 */
	public static void set(double x, String... tagBits) {
		tagBits = check(tagBits);
		dflt.set(x, tagBits);
	}

	/**
	 * Set historical -- edit an old Stat entry.
	 * Optional method! Not all implementations support this.
	 *
	 * @param dx
	 * @param tags
	 */
	public static void set(Time at, double x, String... tagBits) throws UnsupportedOperationException {
		tagBits = check(tagBits);
		dflt.set(at, x, (Object[]) tagBits);
	}

	/**
	 * TODO Label the current data point (not supported yet)
	 *
	 * @return old-label for the current time-bucket, or null
	 */
	public static String label(String label, String... tagBits) {
		tagBits = check(tagBits);
		return dflt.label(label, tagBits);
	}

	/**
	 * Track mean & variance!
	 * For every tag in tagBits, update the mean & variance of its distribution with x.
	 * WARNING: it is a mistake to mix count() and mean()
	 * @param x
	 * @param tagBits
	 */
	public static void mean(double x, Object... tagBits) {
		assert tagBits != null;
		assert tagBits.length != 0;
		tagBits = check(tagBits);
		dflt.mean(x, tagBits);
	}

	/**
	 * @param tag
	 * @return A MeanVar1D, can be null.
	 */
	// <X> to avoid maths dependency
	public static MeanRate getMean(String... tagBits) {
		tagBits = check(tagBits);
		return dflt.getMean(tagBits);
	}

	/**
	 * Can be zero. never null.
	 *
	 * @param tag
	 * @return The rate right now.
	 */
	public static Rate get(String... tagBits) {
		tagBits = check(tagBits);
		return dflt.get(tagBits);
	}

	public static Dt getPeriod(String ignoredForNow) {
		return dflt.getPeriod();
	}

	/**
	 *
	 * @WARNING bucket filtering is done by mid-point value, and you only get
	 *          buckets whose mid-points fall within start & end.
	 *
	 * @param id
	 * @param start
	 * @param end
	 *            Can be in the future! If you want to ensure you get the latest
	 *            value, use <code>new Time().plus(Tunit.DAY)</code> or similar.
	 * @param interpolationFn
	 *            null is the default.
	 * @param bucketSize Can be null for "as it is stored"
	 * @return An IDataStream never null! May be empty.
	 */
	// <X> to avoid maths dependency or cast
	@SuppressWarnings("unchecked")
	public static <X extends Iterable> IFuture<X> getData(Time start, Time end, KInterpolate interpolationFn, Dt bucketSize, String... tagBits) {
		tagBits = check(tagBits);
		return (IFuture<X>) dflt.getData(start, end, interpolationFn, bucketSize, tagBits);
	}
	

	/**
	 *
	 * @WARNING bucket filtering is done by mid-point value, and you only get
	 *          buckets whose mid-points fall within start & end.
	 *
	 * @param id
	 * @param start
	 * @param end
	 *            Can be in the future! If you want to ensure you get the latest
	 *            value, use <code>new Time().plus(Tunit.DAY)</code> or similar.
	 * @param interpolationFn
	 *            null is the default.
	 * @return An IDataStream never null! May be empty.
	 */
	// <X> to avoid maths dependency or cast
	public static <X extends Iterable> IFuture<X> getData(Pattern tagMatcher,
			Time start, Time end) {
		return (IFuture<X>) dflt.getData(tagMatcher, start, end);
	}

	/**
	 * @param tagBits
	 *            Must not be empty. WIll be run through {@link #check(String...)} to convert any nulls
	 * @return The tag/id for using in e.g. getData()
	 */	
	public static final String tag(Object... tagBits) {
		assert tagBits.length != 0;
		tagBits = check(tagBits);
		StringBuilder sb = new StringBuilder();
		for (Object tagBit : tagBits) {
			String string = tagBit.toString();
			assert ! Utils.isBlank(string) : "[" + string + "] " + Printer.toString(tagBits);
			// Standardise whitespace
			String tb = tag2_escape(string); // Why?
			sb.append(tb);
			sb.append(HIERARCHY_CHAR);
		}
		StrUtils.pop(sb, 1);
		return sb.toString();
	}

	static String tag2_escape(String string) {
		if (Utils.isBlank(string)) {
			throw new IllegalArgumentException("[" + string + "]");
		}
		String tb = StrUtils.compactWhitespace(string);
		// // TODO escape /
		// tb = tb.replace(HEIRARCHY_CHAR, HEIRARCHY_CHAR+HEIRARCHY_CHAR);
		return tb;
	}

	/**
	 * Replace the default DataLog with a new one, as specified by config.
	 *
	 * @param myConfig
	 */
	public static void setConfig(StatConfig myConfig) {
		if (dflt != null) {
			try {
				dflt.close();
			} catch (Throwable e) {
				// swallow
				Log.e(LOGTAG, e);
			}
		}
		try {
			Class<?> klass = Class.forName("com.winterwell.datalog.StatImpl");
			Constructor<?> cons = klass.getConstructor(StatConfig.class);
			dflt = (IDataLog) cons.newInstance(myConfig);
		} catch (Exception ex) {
			throw Utils.runtime(ex);
		}
	}

	public static Collection<String> getActiveLabels() {
		return dflt.getActiveLabels();
	}

	public static void count(double dx, Object... tagBits) {
		assert tagBits != null;
		assert tagBits.length != 0;
		tagBits = check(tagBits);
		dflt.count(dx, tagBits);
	}

	/**
	 * Count! The main way to put data into Stat.
	 *
	 * @param dx
	 * @param tags
	 */
	public static void count(double dx, String... tagBits) {
		tagBits = check(tagBits);
		dflt.count(dx, (Object[]) tagBits);
	}
	
	public static void count(DataLogEvent event) {
	}

	/**
	 * Count historical -- edit an old Stat entry.
	 * Optional method! Not all implementations support this.
	 *
	 * @param dx
	 * @param tags
	 */
	public static void count(Time at, double dx, String... tagBits) throws UnsupportedOperationException {
		tagBits = check(tagBits);
		dflt.count(at, dx, (Object[]) tagBits);
	}

	/**
	 * @param tag
	 * @param start
	 *            Can be null for "from the dawn of time"
	 * @param end
	 *            Can be null
	 * @return the total for the period. Never null. Can be zero.
	 */
	public static IDataLogReq<Double> getTotal(Time start, Time end, String... tagBits) {
		tagBits = check(tagBits);
		return dflt.getTotal(start, end, tagBits);
	}

	static String[] check(Object... tagBits) {
		String[] arr = new String[tagBits.length];
		for(int i=0; i<tagBits.length; i++) {
			Object ti = tagBits[i];
			assert ! (ti instanceof DataLogEvent) : ti; // wrong method!
			arr[i] = ti==null? null : ti.toString(); 
		}
		return check(arr);
	}
	/**
	 * Check validity of tagBits. Fill in nulls and blanks with "null" and "blank"
	 * @param tagBits
	 * @return tagBits
	 */
	static String[] check(String... tagBits) {		
		assert tagBits != null;
		assert tagBits.length != 0;
		for(int i=0; i<tagBits.length; i++) {
			String tbi = tagBits[i];
			if (tbi==null) {
				tagBits[i] = "null";
			} else if (Utils.isBlank(tbi)) {
				tagBits[i] = "blank";
			}
		}
		return tagBits;
	}

	public static void removeListener(String... tagBits) {
		tagBits = check(tagBits);
		dflt.removeListener(tagBits);
	}

	/**
	 * Self-test that Stat has loaded OK.
	 * NB: Callers should always catch Warning, which is non-fatal.
	 *
	 * @throws FailureException
	 * @throws Warning
	 */
	public static void test() throws FailureException, Warning {
		IDataLog impl = DataLog.getImplementation();
		if (impl==null || impl instanceof DummyDataLog) {
			throw new FailureException("Stat implementation not set! Check StatImpl class loaded OK.");
		}
		// Try this test a few times, to allow for the race condition around bucket changes.
		for(int i=0; i<5; i++) {
			DataLog.count(1, "test");
			Rate cnt = DataLog.get("test");
			if (cnt.x >= 1) break; // OK
			if (i==4) {
				throw new FailureException("Stat count/get test failed! Check that the StatImpl started OK.");
			}
			Utils.sleep(50);
		}
		// Try a flush
		DataLog.flush();
		// Check something which should have stored data -- e.g. the Stat heartbeat free_mem
		IFuture<Iterable> heartbeat = DataLog.getData(new Time().minus(TUnit.MONTH), new Time().minus(TUnit.HOUR), null, null, IDataLog.STAT_MEM_USED);
		List data = Containers.getList(heartbeat.get());
		if (data.isEmpty()) {
			throw new Warning("Stat heartbeat missing. This could be a clean install (not a bug), or it could be a storage issue.");
		}
	}

	public static IDataLog getImplementation() {
		return dflt;
	}

	public static IFuture<MeanRate> getMean(Time start, Time end, String... tagBits) {
		tagBits = check(tagBits);
		return dflt.getMean(start, end, tagBits);
	}

	public static IFuture<? extends Iterable> getMeanData(Time start, Time end,
			KInterpolate fn, Dt bucketSize, String... tagbits) 
	{
		tagbits = check(tagbits);
		return dflt.getMeanData(start, end, fn, bucketSize, tagbits);
	}

	private static final ThreadLocal<String> dataspace = new ThreadLocal();
	public static final String DEFAULT_DATASPACE = "default";
	
	public static String getDataspace() {
		String ds = dataspace.get();
		return ds==null? DEFAULT_DATASPACE : ds; 
	}

	public static void setEventCount(DataLogEvent event) {
		dflt.setEventCount(event);
	}


}
