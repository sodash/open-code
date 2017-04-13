package com.winterwell.maths.timeseries;

import com.winterwell.utils.time.Dt;
import com.winterwell.utils.time.Time;


public interface ITimeGrid {
	
	Time getEnd();
	Time getStart();

	/**
	 * @param time 
	 * @return bucket-index (zero-indexed). This is always in range -- too small/large gets capped.
	 *         Can be negative! e.g. SimpleTimeGrid
	 */
	int getBucket(Time time);

	Dt getDt();

	Time getBucketStart(int bucket);
	/**
	 * @param bucket
	 * @return the end (latest time) of the bucket. This is always after getBucketStart()
	 */
	Time getBucketEnd(int bucket);


	/**
	 * @param time
	 * @return true if this is within a bucket
	 */
	boolean contains(Time time);
}
