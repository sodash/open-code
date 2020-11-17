/**
 * 
 */
package com.winterwell.maths.stats.distributions.d1;

import java.io.Serializable;
import java.util.Map;
import java.util.logging.Level;

import com.winterwell.maths.ITrainable;
import com.winterwell.utils.MathUtils;
import com.winterwell.utils.StrUtils;
import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.utils.containers.Range;
import com.winterwell.utils.log.Log;

/**
 * Calculate the mean and variance. Does not hold training data, so you can
 * merrily stream gigabytes of data through it. Can be the true-mean or be
 * forgetful. Also stores the min and max it's seen. Note: Does not apply the
 * n/(n-1) adjustment to variance for sample bias.
 * <p>
 * Use via {@link #train1(Double)}
 * 
 * @author daniel
 * @testedby  MeanVar1DTest}
 */
public class MeanVar1D extends ADistribution1D implements
		ITrainable.Unsupervised.Weighted<Double>, Serializable
		// TODO IHasJson 
		{

	private static final long serialVersionUID = 1L;
	private int count;
	double lossFactor;

	@Override
	public void train(double[] weights, Iterable<? extends Double> wdata) {
		int i=0;
		for (Double d : wdata) {
			train1(d, weights[i]);
		}
	}
	
	double max = Double.NEGATIVE_INFINITY;

	double mean;

	double mean2;

	double min = Double.POSITIVE_INFINITY;

	@Override
	public double density(double x) throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void finishTraining() {
		super.finishTraining();
	}

	/**
	 * @return how many data-points?
	 */
	public final int getCount() {
		return count;
	}

	public final double getMax() {
		return max;
	}

	@Override
	public final double getMean() {
		return mean;
	}

	public final double getMin() {
		return min;
	}
	
	@Override
	public Range getSupport() {
		return new Range(min, max);
	}

	@Override
	public final double getVariance() {
		return mean2 - (mean * mean);
	}

	@Override
	public boolean isReady() {
		return count != 0;
	}

	@Override
	public void resetup() {
		assert trainingData==null || trainingData.size() == 0 : this; // should always be empty anyway
		super.resetup();
		count = 0;
	}

	/**
	 * @param lossFactor
	 *            value in [0, 1). If zero (the default) this will compute the
	 *            true mean. If >0, then that amount will be "lost" each round,
	 *            thus favouring more recent values (makes it into a form of
	 *            low-pass filter). Values close to 1 will work, but be so lossy
	 *            as to make little sense.
	 * 
	 * */
	public final void setLossFactor(double lossFactor) {
		if (lossFactor < 0 || lossFactor >= 1)
			throw new IllegalArgumentException();
		this.lossFactor = lossFactor;
	}

	/**
	 * @return mean +- standard-deviation (so _if_ the data is normal ~60% would be within this range)
	 */
	@Override
	public String toString() {
		String aString = StrUtils.toNSigFigs(getMean(), 2) + " ± "
				+ StrUtils.toNSigFigs(getStdDev(), 2);
		return aString;
	}

	@Override
	public void train(Iterable<? extends Double> data) {
		for (Double x : data) {
			train1(x);
		}
	}

	@Override
	public final void train1(Double _x) {
		train1(_x, 1.0);
	}
	
	@Override
	public synchronized void train1(Double _x, double weight) {
		if (weight==0) return;
		assert weight > 0;
		double x = _x;
		mean = train2_updateMean(x, mean,weight);
		mean2 = train2_updateMean(x * x, mean2,weight);
		min = Math.min(x, min);
		max = Math.max(x, max);
		// increment count
		count += weight;
		// Reduce count?
		if (count == Integer.MAX_VALUE) {
			count = count / 2;
			Log.report("stream count reached max (reset to half)", Level.FINE);
		}		
	}


	/**
	 * Called before incrementing count. Pure functional - does not modify
	 * anything!
	 * 
	 * @param d
	 *            will not be modified
	 * @param average
	 *            will not be modified
	 * @param weight Normally 1
	 * @return new average
	 */
	final double train2_updateMean(double d, double average, double weight) {
		assert MathUtils.isFinite(d) : d;
		double a = ((1 - lossFactor) * count) / (count + weight);
		assert MathUtils.isFinite(a) : a + " " + lossFactor + " " + count;
		double a1 = 1 - a;
		average = a * average + a1 * d;
		assert MathUtils.isFinite(average);
		return average;
	}

	/**
	 * 
	 * @return {mean, var, max, min, count}
	 */
	public Map<String,Object> toJson2() {
		return new ArrayMap(
				"mean", getMean(),
				"var", MathUtils.isFinite(getVariance())? getVariance() : null,
				"max", MathUtils.isFinite(max)? max : null,
				"min", MathUtils.isFinite(min)? min : null,
				"count", getCount()
				);
	}

}
