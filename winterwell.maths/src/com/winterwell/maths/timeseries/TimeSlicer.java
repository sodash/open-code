package com.winterwell.maths.timeseries;

import java.util.ArrayList;
import java.util.List;

import com.winterwell.utils.containers.AbstractIterator;
import com.winterwell.utils.time.Dt;
import com.winterwell.utils.time.Period;
import com.winterwell.utils.time.Time;
import com.winterwell.utils.time.TimeIterator;

/**
 * Makes buckets [),[),... []. Works with {@link ListDataStream} to give a
 * time-based histogram. Usage: to build-up time based streams using
 * {@link #get(int)}. E.g. you might use<br>
 * <code><pre>
dataStream = new ListDataStream(1, slicer)
dataStream.get(bucketer.getBucket(time)).add(0, 1);
</pre></code> to count time-stamped entries.
 * 
 * @author daniel
 * @testedby {@link TimeSlicerTest}
 */
public final class TimeSlicer extends VariableGridInfo implements
		Iterable<Period>,ITimeGrid {
	

	private static long[] buckets(TimeIterator times) {
		// setup buckets
		List<Long> ts = new ArrayList();
		for (Time time : times) {
			ts.add(time.getTime());
		}
		// Always include the end as a bucket
		Long last = ts.get(ts.size()-1);
		long end = times.getEnd().getTime();
		if (end!=last) {
			assert end > last : times;
			ts.add(end);
		}
		// copy into an array
		long[] buckets = new long[ts.size()];
		for (int i = 0; i < buckets.length; i++) {
			buckets[i] = ts.get(i);
		}
		return buckets;
	}

	private final Dt dt;

	/**
	 * Example usage: <code><pre>
dataStream = new ListDataStream(1, slicer)
dataStream.get(bucketer.getBucket(time)).add(0, 1);
</pre></code>

	 * @param start The first bucket will be [start, start+step)
	 * @param end The last bucket will be [?, end) 
	 * NB: All the buckets are created on construction, so don't use a far-future end!
	 * @param step
	 */
	public TimeSlicer(Time start, Time end, Dt step) {
		this(new TimeIterator(start, end, step));
		assert step.getValue() > 0 : step;
	}
	
	/**
	 * Create a time-slicer for the given times.
	 * <p>
	 * NB: All the buckets are created on construction, so don't use a far-future end!
	 * @param times Will use the output of times, always including the end (which is sometimes omitted by the slicer).
	 */
	public TimeSlicer(TimeIterator times) {
		super(buckets(times));
		Dt _dt = times.getStep();
		// always positive
		if (_dt.getValue() < 0) {
			dt = _dt.multiply(-1);
		} else {
			dt = _dt;
		}
	}

	/**
	 * @param time
	 * @return true if this is within a bucket
	 */
	public boolean contains(Time time) {
		long t = time.getTime();
		if (t < times[0])
			return false;
		if (t > times[times.length - 1])
			return false;
		return true;
	}

	/**
	 * @param x
	 *            The value you wish to store.
	 * @return bucket-index (zero-indexed). This is always in range -- too-small
	 *         gets capped to 0, too-large gets capped to the end bucket.
	 *         <p>
	 *         So if your data runs outside the buckets, you are liable to get
	 *         very large start/end buckets unless you screen the results, which
	 *         you can do with {@link #contains(Time)}.
	 * @see #contains(Time)
	 */
	public int getBucket(Time time) {
		int b = getBucket(time.getTime());
		assert b >= 0 && b < size() : b + " vs " + size()+" for "+time+" in "+this;
		return b;
	}
	
	@Override
	public String toString() {
		return "TimeSlicer["+getStart()+",+"+dt+","+getEnd()+"]";
	}

	public Time getEnd() {
		return new Time((long)times[times.length-1]);
	}
	public Time getStart() {
		return new Time((long)times[0]);
	}

	public Time getBucketEnd(int bucket) {
		return new Time(times[bucket + 1]);
	}

	public Time getBucketMiddleTime(int bucket) {
		return new Time((times[bucket + 1] + times[bucket]) / 2);
	}

	/**
	 * The bucket size in milliseconds
	 */
	@Override
	public double getBucketWidth(int ignored) {
		return dt.getMillisecs();
	}
	
	public Dt getDt() {
		assert dt != null;
		return dt;
	}

	public Time getBucketStart(int bucket) {
		return new Time(times[bucket]);
	}

	@Override
	public AbstractIterator<Period> iterator() {
		return new TimeSlicerIterator(this);
	}
		

	/**
	 * Convenience for "add 1 to the counter for time"
	 * @param time
	 * @param dx
	 * @param volumeOverTime A 1D data stream
	 */
	public void add(Time time, double dx, ListDataStream volumeOverTime) {
		int bi = getBucket(time);
		volumeOverTime.get(bi).add(0, dx);
	}

	

}

final class TimeSlicerIterator extends AbstractIterator<Period> {
	final TimeSlicer slicer;
	int i = 0;
	public TimeSlicerIterator(TimeSlicer timeSlicer) {
		this.slicer = timeSlicer;
	}
	@Override
	protected Period next2() throws Exception {
		if (i == slicer.size())
			return null;
		double b = slicer.getBucketBottom(i);
		double t = slicer.getBucketTop(i);
		assert b < t : i+" "+b+" "+t;
		Time s = new Time(b);
		Time e = new Time(t);
		i++;
		return new Period(s, e);
	}
	
}