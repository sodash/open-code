package com.winterwell.maths.chart;

import com.winterwell.maths.IGridInfo;
import com.winterwell.maths.timeseries.VariableGridInfo;

/**
 * Makes buckets based on evenly slicing the *log* (base 2 or 10 depending on the constructor) of the range. TODO a common
 * base class with TimeSlicer
 * 
 * @author daniel
 * @testedby {@link LogGridInfoTest}
 */
public final class LogGridInfo extends VariableGridInfo implements IGridInfo {

	/**
	 * Base 10 from 0 to max
	 * @param max
	 * @param buckets
	 */
	public LogGridInfo(double max, int buckets) {
		this(max, buckets, 0);
	}
	
	/**
	 * 
	 * @param max
	 * @param buckets
	 * @param minBucketSize Normally 0. If >0, no bucket will be smaller than this, which can
	 * result in the early buckets being uniform size for a while (so not quite a log scale).
	 * use-case: For integer-valued data, to avoid having the early buckets be 0.1, 0.2... etc.
	 */
	public LogGridInfo(double max, int buckets, double minBucketSize) {
		super(new double[buckets + 1]);
		assert max > 0 && buckets > 0;
		// setup buckets
		times[0] = 0;
		times[times.length - 1] = max;		
		double dexp = Math.log10(max) / buckets;
		for (int i = 1; i < times.length - 1; i++) {
			double ti = Math.pow(10, i * dexp);
			if (ti < minBucketSize*i) {
				times[i] = i*minBucketSize;
			} else {
				times[i] = ti;
			}
		}
	}
	
	/**
	 * Base 2 (binary) from 0 to 2^(buckets-1).
	 * E.g. 5 buckets gives the grid [0,1,2,4,8,16]
	 * @param max
	 * @param buckets
	 */
	public LogGridInfo(int n) {
		super(new double[n + 1]);
		assert n > 0;
		// setup buckets
		times[0] = 0;
		times[1] = 1;		
		for (int i = 2; i < times.length; i++) {
			times[i] = 2*times[i-1];
		}
	}

}
