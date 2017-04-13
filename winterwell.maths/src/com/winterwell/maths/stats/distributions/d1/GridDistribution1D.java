package com.winterwell.maths.stats.distributions.d1;

import java.util.Arrays;

import com.winterwell.maths.IGridInfo;
import com.winterwell.maths.stats.StatsUtils;
import com.winterwell.maths.timeseries.ArrayDataStream;
import com.winterwell.maths.timeseries.IDataStream;
import com.winterwell.utils.MathUtils;
import com.winterwell.utils.containers.Range;

/**
 * Status: Kept for safety re old xml
 * @see HistogramData
 * 
 * A frequency-count distribution based on buckets. Use with Distribution1DChart for a histogram.
 * 
 * <h3>Lifecycle</h3>
 * <ol>
 * <li>Create
 * <li>Use {@link #count(double)} to add data.
 * <li> {@link #normalise()}. After this you cannot add more data. Use
 * {@link #copy()} if you want to access probabilities but also carry on
 * counting.
 * <li>Use {@link #getMean()} and other functions.
 * </ol>
 * 
 * @author daniel
 */
@Deprecated
public class GridDistribution1D extends ADistribution1D {

	private final double[] backing;

	private final IGridInfo gridInfo;

	public GridDistribution1D(IGridInfo grid) {
		this.gridInfo = grid;
		backing = new double[grid.size()];
	}

	/**
	 * Like {@link #count(double)}, but adds/substracts a variable amount.
	 * 
	 * @param x
	 * @param dCount
	 *            amount to add
	 */
	public void add(double x, double dCount) {
		assert !normalised;
		int i = gridInfo.getBucket(x);
		backing[i] += dCount;
	}

	/**
	 * Make a deep copy, e.g. so you can normalise the copy but keep counting.
	 */
	public GridDistribution1D copy() {
		GridDistribution1D copy = new GridDistribution1D(gridInfo);
		for (int i = 0; i < backing.length; i++) {
			copy.backing[i] = backing[i];
		}
		copy.normalised = normalised;
		return copy;
	}

	/**
	 * 
	 * @param x
	 *            If this is outside the grid, it be capped at the min or max
	 */
	public void count(double x) {
		assert !normalised;
		int i = gridInfo.getBucket(x);
		backing[i]++;
	}

	public void count(double[] xs) {
		for (double x : xs) {
			count(x);
		}
	}

	/**
	 * @return The count for this bucket / bucket-width.
	 * 			May not be normalised! See {@link #isNormalised()} and {@link #normalise()}.
	 */
	@Override
	public double density(double x) {
		int i = gridInfo.getBucket(x);
		double w = gridInfo.getBucketTop(i) - gridInfo.getBucketBottom(i);
		return backing[i] / w;
	}

	/**
	 * @return XY vectors where X=bucket-mid-point, Y=count/density
	 */
	public IDataStream getDataStream() {
		double[] mids = gridInfo.getMidPoints();
		return new ArrayDataStream(mids, backing);
	}

	public IGridInfo getGridInfo() {
		return this.gridInfo;
	}

	@Override
	public double getMean() {
		// Using the mid-point, which seems OK for both fixed & variable grids. Besides, what else could we do?
		double[] pts = gridInfo.getMidPoints();
		double mean = 0, totalWeight = 0;
		for (int i = 0; i < pts.length; i++) {
			mean += backing[i] * pts[i];	// the use of mid-point takes into account bucket-width
			totalWeight += backing[i];
		}
		return mean / totalWeight;
	}

	@Override
	public Range getSupport() {
		return new Range(gridInfo.getMin(), gridInfo.getMax());
	}

	public double getTotalWeight() {
		return MathUtils.sum(backing);
	}

	@Override
	public double getVariance() {
		double m = getMean();
		double[] pts = gridInfo.getMidPoints();
		double var = 0;
		for (int i = 0; i < pts.length; i++) {
			var += backing[i] * MathUtils.sq(m - pts[i]);
		}
		return var;
	}

	@Override
	public void normalise() {
		if (normalised)
			return;
		StatsUtils.normaliseProbVector(backing);
		normalised = true;
	}

	/**
	 * FIXME this over-estimates by including all counts from the 1st & last buckets,
	 * instead of a proportion
	 */
	@Override
	public double prob(double min, double max) {
		int i = gridInfo.getBucket(min);
		int j = gridInfo.getBucket(max);
		double p = 0;		
		for (int b = i; b <= j; b++) {			
			p += backing[b];
		}
		// slices to deduct
		double bottom = gridInfo.getBucketBottom(i);
		double top = gridInfo.getBucketTop(j);
		double gap1 = min - bottom;
		if (gap1 > 0) {			
			// NB: gaps can be negative if the range has gone of the range we counted for
			// -- though arguably that's a user error
			double deduct = backing[i]*gap1/gridInfo.getBucketWidth(i);
			p -= deduct;
		}
		double gap2 = top - max;
		if (gap2 > 0) {			
			double deduct = backing[j]*gap2/gridInfo.getBucketWidth(j);
			p -= deduct;
		}
		return p;
	}

	/**
	 * zero all entries
	 */
	@Override
	public void resetup() {
		Arrays.fill(backing, 0);
		super.resetup();
	}

	/**
	 * Sample from this distribution
	 * 
	 * @return an x selected by random weight
	 */
	@Override
	public Double sample() {
		assert isNormalised();
		double p = random().nextDouble();
		double sum = 0;
		for (int i = 0; i < backing.length; i++) {
			sum += backing[i];
			if (sum > p) {
				// uniform within a bucket				
				return gridInfo.getBucketBottom(i)
						+ random().nextDouble()*gridInfo.getBucketWidth(i);
			}
		}
		// What? must be a rounding issue. Return anything
		assert p > 0.99 && p <= 1;
		return gridInfo.getMax();
	}

	/**
	 * @param x
	 *            Problems will occur if this is outside the grid!
	 * @param count
	 *            The count-value to set
	 */
	public void setCount(double x, double count) {
		assert !normalised;
		int i = gridInfo.getBucket(x);
		backing[i] = count;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + gridInfo + "]";
	}

}
