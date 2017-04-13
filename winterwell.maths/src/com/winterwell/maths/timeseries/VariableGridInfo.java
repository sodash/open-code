package com.winterwell.maths.timeseries;

import java.util.Arrays;

import com.winterwell.maths.IGridInfo;
import com.winterwell.utils.Printer;

/**
 * A grid with variable-sized buckets.
 * 
 * @author daniel
 */
public class VariableGridInfo implements IGridInfo {

	@Override
	public double getBucketWidth(int bucket) {
		return times[bucket+1] - times[bucket];
	}
	
	/**
	 * The bucket edges. This includes the start and end edges, so length is 1
	 * more than the number of buckets.
	 */
	protected final double[] times;

	/**
	 * @param buckets The bucket edges. This includes the start and end edges, so length is 1
	 * more than the number of buckets.
	 */
	public VariableGridInfo(double[] buckets) {
		assert buckets.length > 1 : buckets.length;
		this.times = buckets;
		Arrays.sort(this.times);
	}

	/**
	 * @param buckets The bucket edges. This includes the start and end edges, so length is 1
	 * more than the number of buckets.
	 */
	public VariableGridInfo(long[] buckets) {
		if (buckets.length < 2) {
			throw new IllegalArgumentException("Only "+buckets.length+" buckets: "+Printer.toString(buckets));
		}
		// convert long[] -> double[]
		this.times = new double[buckets.length];
		for (int i = 0; i < buckets.length; i++) {
			this.times[i] = buckets[i];
		}
		Arrays.sort(this.times);
	}

	@Override
	public final int getBucket(double x) {
		// let the start point be included in the next bucket
		int i = Arrays.binarySearch(times, x);
		if (i < 0) {
			// before the beginning?
			if (i == -1)
				return 0;
			i = -i - 2;
		}
		// after the end?
		i = Math.min(i, times.length - 2);
		assert i >= 0: x+" "+i;
		return i;
	}

	@Override
	public final double getBucketBottom(int bucket) {
		return times[bucket];
	}

	@Override
	public final double getBucketMiddle(int bucket) {
		return (times[bucket + 1] + times[bucket]) / 2.0;
	}


	@Override
	public final double getBucketTop(int bucket) {
		return times[bucket + 1];
	}

	@Override
	public double[] getEndPoints() {
		double[] ends = new double[times.length-1];
		System.arraycopy(times, 1, ends, 0, ends.length);
		return ends;
	}

	@Override
	public final double getMax() {
		return times[times.length - 1];
	}

	@Override
	public double[] getMidPoints() {
		double[] mids = new double[times.length-1];
		for(int i=0; i<mids.length; i++) {
			mids[i] = (times[i]+times[i+1])*0.5;
		}
		return mids;
	}

	@Override
	public final double getMin() {
		return times[0];
	}

	@Override
	public double[] getStartPoints() {
		return Arrays.copyOf(times, times.length - 1);
	}

	@Override
	public final int size() {
		return times.length - 1;
	}

	@Override
	public String toString() {		
		return getClass().getSimpleName()+"["+
				(times.length > 5? Printer.toStringNumber(times[0])+"..."+Printer.toStringNumber(times[times.length-1])
						:Printer.toString(times))
				+"]";
	}
}
