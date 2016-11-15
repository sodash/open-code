package com.winterwell.datalog;

import java.io.Closeable;
import java.io.Flushable;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import winterwell.utils.time.Dt;
import winterwell.utils.time.Time;

import com.winterwell.datalog.Stat.KInterpolate;
import com.winterwell.utils.threads.IFuture;

/**
 * An implementation to back Stat.
 * 
 * @author daniel
 *         <p>
 *         <b>Copyright & license</b>: (c) Winterwell Associates Ltd, all rights
 *         reserved. This class is NOT formally a part of the winterwell.utils
 *         library. In particular, licenses for the winterwell.utils library do
 *         not apply to this file.
 */
public interface IStat extends Closeable, Flushable {
	
	/**
	 * The memory used by the JVM -- this should be saved by IStat implementations, so it can also be used as a heartbeat for Stat.
	 */
	public static final String STAT_MEM_USED = "mem_used";
	
	/**
	 * @return the tags with active counts in the current time-window.
	 */
	Set<String> getLive();

	/**
	 * Replace the existing value
	 * @param x
	 * @param tagBits Unlike {@link #count(double, Object...)}, this will NOT recurse up the tag hierarchy.
	 * It will set one entry, the combined tags.
	 */
	void set(double x, Object... tagBits);

	/**
	 * TODO mean & variance
	 * Update the distribution of every tag in tagBits with x.
	 * 
	 * WARNING: it is a mistake to mix count() and mean()
	 * @param x
	 * @param tagBits Use . for hierarchical tags
	 */
	void mean(double x, Object... tagBits);

	/**
	 * @param tag
	 * @return Can be null
	 */
	MeanRate getMean(String... tag);

	/**
	 * @param tag
	 * @return can be zero, never null
	 */
	Rate get(String... tag);

	/**
	 * @return the bucket's period
	 */
	Dt getPeriod();

	/**
	 * 
	 * @WARNING bucket filtering is done by mid-point value, and you only get
	 *          buckets whose mid-points fall within start & end.
	 * 
	 * @param tagBits
	 * @param start
	 * @param end
	 *            If you want to ensure you get the latest value, use
	 *            <code>new Time().plus(Tunit.DAY)</code> or similar.
	 * @param bucketSize Can be null for "as it is stored"
	 * @return never null! May be empty.
	 */
	IFuture<? extends Iterable> getData(Time start, Time end, KInterpolate interpolationFn, Dt bucketSize, String... tagBits);

	
	IFuture<? extends Iterable> getMeanData(Time start, Time end, KInterpolate fn,
			Dt bucketSize, String... tagBits);

	/**
	 * 
	 * @WARNING bucket filtering is done by mid-point value, and you only get
	 *          buckets whose mid-points fall within start & end.
	 * 
	 * @param id
	 *            Include all data-points that match! See the individual Datums
	 *            for what they matched.
	 * @param start
	 * @param end
	 *            If you want to ensure you get the latest value, use
	 *            <code>new Time().plus(Tunit.DAY)</code> or similar.
	 * @return never null! May be empty.
	 */
	IFuture<? extends Iterable> getData(Pattern id, Time start, Time end);

	void close();

	void flush();

	Collection<String> getActiveLabels();

	/**
	 * Add to the existing value
	 * Use . for hierarchical tags
	 * WARNING: it is a mistake to mix count() and mean()<br>
	 * TODO should we return the current Rate?
	 * @param dx
	 * @param tag 
	 */
	void count(double dx, Object... tags);

	String label(String label, String... tagBits);

	/**
	 * @param tag
	 * @param start
	 * @param end
	 * @return
	 * @see Stat#getTotal(Time, Time, String...)
	 */
	IStatReq<Double> getTotal(Time start, Time end, String... tagBits);

	void setEvent(String event, String... tags);

	/**
	 * 
	 * @param tag
	 * @param listener
	 * Probably an {@link Alert}
	 */
	void setListener(IListenStat listener, String... tagBits);

	void removeListener(String... tagBits);

	Map<String, IListenStat> getListeners();

	/**
	 * Set historical -- edit an old Stat entry.
	 * Optional method! Not all implementations support this. 
	 * 
	 * @param dx
	 * @param tags Unlike {@link #count(double, Object...)}, this will NOT recurse up the tag hierarchy.
	 * It will set one entry, the combined tags.
	 */
	void set(Time at, double x, Object... tags);

	/**
	 * Count historical -- edit an old Stat entry.
	 * Optional method! Not all implementations support this. 
	 * 
	 * @param dx
	 * @param tags
	 */
	void count(Time at, double dx, Object... tags);

	/**
	 * @return The config object (never null).
	 * This might include DB connection options -- but it does not have to, provided those are set
	 * elsewhere.
	 */
	StatConfig getConfig();

	IFuture<MeanRate> getMean(Time start, Time end, String... tagBits);

	
}
