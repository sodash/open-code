package com.winterwell.datalog;

import java.util.Iterator;
import java.util.Map;
import java.util.regex.Pattern;

import com.winterwell.maths.stats.distributions.d1.MeanVar1D;
import com.winterwell.maths.timeseries.IDataStream;
import com.winterwell.utils.containers.Pair2;
import com.winterwell.utils.time.Dt;
import com.winterwell.utils.time.Period;
import com.winterwell.utils.time.Time;

import com.winterwell.datalog.Stat.KInterpolate;
import com.winterwell.utils.threads.IFuture;

public interface IStatStorage {

	/**
	 * Use IDataStream csv format: timestamp, tag, value.
	 * 
	 * @param period
	 * @param tag2count
	 * @param tag2mean
	 */
	public abstract void save(Period period, Map<String, Double> tag2count, Map<String, MeanVar1D> tag2mean);
	
	/**
	 * Save tags at the specified time, instead of the one of the current bucket. 
	 * Currently implemented only by SQLStorage.
	 *  
	 * @param tag2time2count
	 */
	public abstract void saveHistory(Map<Pair2<String, Time>, Double> tag2time2count);

	public abstract IFuture<IDataStream> getData(Pattern id, Time start, Time end);

	/**
	 * TODO How can we return spread information? i.e. mean/var etc.??
	 * 
	 * @WARNING bucket filtering is done by mid-point value, and you only get buckets
	 * whose mid-points fall within start & end.
	 * 
	 * @param tag
	 * @param start Can be null (includes all)
	 * @param end Can be null (includes all)
	 * @param bucketSize Can be null for "as it is stored"
	 * @return never null! May be empty.
	 */
	public abstract StatReq<IDataStream> getData(String tag, Time start, Time end, KInterpolate fn, Dt bucketSize);
	
	/**
	 * 
	 * @param tag
	 * @param start Can be null for all-of-history
	 * @param end Can be null for upto-now
	 * @return
	 */
	public abstract StatReq<Double> getTotal(String tag, Time start, Time end);
	
	/**
	 * 
	 * @param server
	 * @param start
	 * @param end
	 * @param tagMatcher
	 * @param tag
	 * @return an iterator over the filtered values.
	 */
	public Iterator getReader(String server, Time start, Time end, Pattern tagMatcher, String tag);

	public IFuture<MeanRate> getMean(Time start, Time end, String tag);

	public StatReq<IDataStream> getMeanData(String tag, Time start,
			Time end, KInterpolate fn, Dt bucketSize);

	void setHistory(Map<Pair2<String, Time>, Double> tagTime2set);
}