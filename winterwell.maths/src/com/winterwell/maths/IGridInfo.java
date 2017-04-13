/**
 *
 */
package com.winterwell.maths;

/**
 * Info on a 1D grid. This is a "bucket selector"
 * 
 * <p>
 * We support irregular grids, which motivates the plethora of methods: getEndPoints() etc.
 * 
 * @see GridInfo
 * @author Joe Halliwell <joe@winterwell.com>
 */
 public interface IGridInfo {

	/**
	 * @param x
	 *            The value you wish to store.
	 * @return bucket-index (zero-indexed)
	 */
	  int getBucket(double x);

	  /**
	   * 
	   * @param bucket
	   * @return the lower / left-most edge of the bucket
	   */
	  double getBucketBottom(int bucket);

	  double getBucketMiddle(int bucket);

	  /**
	   * @param bucket
	   * @return The higher / right-most edge of the bucket
	   */
	  double getBucketTop(int bucket);

	  double[] getEndPoints();

	double getMax();

	  double[] getMidPoints();

	double getMin();

	  double[] getStartPoints();

	/**
	 * @return the number of buckets
	 */
	int size();

	  double getBucketWidth(int bucket);

}