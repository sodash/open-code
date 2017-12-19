package com.winterwell.maths.stats.distributions.d1;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.winterwell.maths.GridInfo;
import com.winterwell.maths.IGridInfo;
import com.winterwell.maths.ITrainable;
import com.winterwell.maths.stats.StatsUtils;
import com.winterwell.maths.stats.distributions.IntDistribution;
import com.winterwell.maths.stats.distributions.discrete.IntegerDistribution;
import com.winterwell.maths.timeseries.ArrayDataStream;
import com.winterwell.maths.timeseries.IDataStream;
import com.winterwell.utils.MathUtils;
import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.utils.containers.Range;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.web.IHasJson;

/**
 * A frequency-count distribution based on buckets. Use with Distribution1DChart for a histogram.
 * Thread safety: train1() is thread-safe, normalise() is not.
 * Big data safety: Tests for overrun and keeps within integer precision limits.
 * 
 * <h3>Lifecycle</h3>
 * <ol>
 * <li>Create
 * <li>Use {@link #count(double)} to add data.
 * <li> {@link #normalise()} if you want probabilities instead of counts. 
 * After this you CANNOT add more data. Use
 * {@link #copy()} if you want to access probabilities but also carry on
 * counting.
 * <li>Use {@link #getMean()} and other functions.
 * </ol>
 * 
 * @see IntDistribution
 * @see IntegerDistribution
 * @testedby {@link HistogramDataTest}
 * @author daniel
 */
public final class HistogramData extends ADistribution1D 
implements ITrainable.Unsupervised.Weighted<Double>, IHasJson
{
	
	@Override
	public void finishTraining() {
		super.finishTraining();
	}
	
	@Override
	public void train(Iterable<? extends Double> data) {
		super.train(data);
	}
	
	/**
	 * bucket-number -> count
	 */
	private final double[] backing;

	private final IGridInfo gridInfo;

//	private transient double[] CDF; an optimisation we probably don't need

	/**
	 * Convenience constructor for {@link #Histogram(IGridInfo)} with regular-sized buckets.
	 * @param min
	 * @param max
	 * @param numBuckets
	 */
	public HistogramData(double min, double max, int numBuckets) {
		this(new GridInfo(min, max, numBuckets));
	}
	
	public HistogramData(IGridInfo grid) {
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
	public HistogramData copy() {
		HistogramData copy = new HistogramData(gridInfo);
		for (int i = 0; i < backing.length; i++) {
			copy.backing[i] = backing[i];
		}
		copy.normalised = normalised;
		return copy;
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
		// TODO normalise by sum backing??
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
	 * Access the stored value for a bucket
	 * @param x Lookup the bucket holding this value. Large/small values will be capped to last/first.
	 * @return the count for the bucket holding x
	 */
	public double getCount(double x) {
		int i = gridInfo.getBucket(x);
		return backing[i];
	}
	
	/**
	 * Probability for a region, i.e. the integrated density over the region
	 * [min, max).
	 * <p>
	 * FIXME this over-estimates at the end of the range by including all counts from the 1st & last buckets,
	 * instead of a proportion.
	 * <p>
	 * NB: Within a bucket, we assume a uniform distribution (so a linear cumulative-probability).
	 * @return 0 if min=max 
	 */
	@Override
	public double prob(double min, double max) {
		assert max >= min : max+" < "+min;
		if (max==min) return 0; // No point probability
		int i = gridInfo.getBucket(min);
		int j = gridInfo.getBucket(max);
		double p = 0;
		// cache the scores (there could be lots of buckets -- but do we really care??)
//		if (normalised) {
//			if (CDF==null) {
//				CDF = new double[backing.length];
//				// Sum the lot
//				for (int b=0; b<backing.length; b++) {			
//					CDF[b] += backing[b];
//				}	
//			}
//			p = CDF[j] - CDF[i];
//		} else {
			// Sum from i to j
			for (int b = i; b <= j; b++) {			
				p += backing[b];
			}
//		}
		// Assume linear within a bucket
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
		// Cap at 1 (can slightly over-run due to floating point errors)
		if (p>1) {
			p = 1;
			assert MathUtils.equalish(p, 1) : p;
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
		assert ! normalised;
		int i = gridInfo.getBucket(x);
		backing[i] = count;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + gridInfo + "]";
	}

	@Override
	public synchronized void train1(Double x, double weight) {
		assert ! normalised;
		int i = gridInfo.getBucket(x);
		backing[i] += weight;
		// Defend against over-run
		overrunDefence(i);
	}

	/**
	 * 		// The underlying doubles would keep going for ages, then stop incrementing.
		// You lose +1 precision at about 10 trillion!
		// At 10^15 we slightly reset the counts by reducing by 10^3
	 * @param i bucket that was modified
	 */
	private void overrunDefence(int i) {
		if (backing[i] < MathUtils.getMaxIntWithDouble()) return;
		Log.w("HistogramData", "Bucket "+i+" "+gridInfo.getBucketBottom(i)+" has reached max int level accuracy - reducing all counts by 1024");
		for (int j = 0; j < backing.length; j++) {
			backing[j] = backing[j] / 1024;
		}		
	}

	@Override
	public void train1(Double x) {
		count(x);
	}
	
	/**
	 * Identical to {@link #train1(Double)} (but fractionally faster as it uses primitive double).
	 * 
	 * @param x
	 *            If this is outside the grid, it be capped at the min or max
	 */
	public synchronized void count(double x) {
		assert ! normalised;
		int i = gridInfo.getBucket(x);
		backing[i]++;
		overrunDefence(i);
	}
	
	@Override
	public void train(double[] weights, Iterable<? extends Double> wdata) {
		int i = 0;
		for(Double x : wdata) {
			double w = weights[i];
			i++;
			train1(x, w);
		}
	}

	@Override
	public Map<String, Object> toJson2() throws UnsupportedOperationException {
		Map<String, Object> map = super.toJson2();		
		
		// add in bucket info			
		// HACK: neater for uniform grids
		if (gridInfo instanceof GridInfo) {
			map.put("counts", Arrays.copyOf(backing, backing.length));
		} else {
			List bucketInfo = new ArrayList();
			for(int i=0; i<backing.length; i++) {
				bucketInfo.add(new ArrayMap(
						"min", gridInfo.getBucketBottom(i),
						"max", gridInfo.getBucketTop(i),
						"count", backing[i]
						));
			}
			map.put("buckets", bucketInfo);
		}		
		
		return map;
	}

}
