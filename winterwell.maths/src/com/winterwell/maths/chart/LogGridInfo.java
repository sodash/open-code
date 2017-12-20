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
		this(max, buckets, 0, 10);
	}
	
	/**
	 * HACK:
	 * If max > 1, then [0,1] is the smallest possible bucket
	 * If max = 1, then it will break [0,1] into pieces.
	 * 
	 * @param max
	 * @param buckets
	 * @param minBucketSize Normally 0. If >0, no bucket will be smaller than this, which can
	 * result in the early buckets being uniform size for a while (so not quite a log scale).
	 * use-case: For integer-valued data, to avoid having the early buckets be 0.1, 0.2... etc.
	 * @param base usually 10
	 */
	public LogGridInfo(double max, int buckets, double minBucketSize, double base) {
		super(new double[buckets + 1]);
		assert max > 0 && buckets > 0;
		assert base > 0 : "try base=10";
		// setup buckets
		times[0] = 0;
		times[times.length - 1] = max;
		double dexp;
		if (base==10) dexp = Math.log10(max) / buckets;
		else dexp = (Math.log(max) / Math.log(base)) / buckets;
		if (max==1) {
			// HACK just slice by powers
			dexp = -1;
		}
		for (int i = 1; i < times.length - 1; i++) {
			int j = dexp>0? i : times.length - i -1;
			double ti = Math.pow(base, i * dexp);
			if (ti < minBucketSize*i) {
				times[j] = i*minBucketSize;
			} else {
				times[j] = ti;
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
