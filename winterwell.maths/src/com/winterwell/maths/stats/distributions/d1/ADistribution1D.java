package com.winterwell.maths.stats.distributions.d1;

import java.util.Map;

import com.winterwell.maths.GridInfo;
import com.winterwell.maths.stats.distributions.ADistributionBase;
import com.winterwell.utils.FailureException;
import com.winterwell.utils.MathUtils;
import com.winterwell.utils.Printer;
import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.utils.containers.Range;

/**
 * Helper class for building 1D distributions.
 * 
 * @author Daniel
 * 
 */
public abstract class ADistribution1D extends ADistributionBase<Double>
		implements IDistribution1D {

	/**
	 * Inefficient and inaccurate integration! Override to replace!
	 */
	@Override
	public double getConfidence(double totalWeight) {
		assert MathUtils.isProb(totalWeight);
		double low = getSupport().low;
		double high = getSupport().high;
		low = Math.max(low, -(Double.MAX_VALUE / 4));
		high = Math.min(high, Double.MAX_VALUE / 4);
		if (totalWeight == 0)
			return low;
		if (totalWeight == 1)
			return high;
		double sum = 0;
		double dx = (high - low) / 10000;
		for (double x = low; x < high; x += dx) {
			double d = density(x);
			sum += d * dx;
			if (sum > totalWeight)
				return x;
		}
		return high;
	}
	
	public Map<String,Object> toJson2() {
		Range range = getSupport();
		return new ArrayMap(
			"mean", getMean(),
			"var", MathUtils.isFinite(getVariance())? getVariance() : null,
			"max", MathUtils.isFinite(range.high)? range.high : null,
			"min", MathUtils.isFinite(range.low)? range.low : null			
		);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see winterwell.maths.stats.distributions.IDistribution1D#getStdDev()
	 */
	@Override
	public double getStdDev() {
		double var = getVariance();
		return Math.sqrt(var);
	}

	@Override
	public Range getSupport() {
		return Range.REALS;
	}

	/**
	 * Probability for a region, i.e. the integrated density over the region
	 * [min, max).
	 * <p>
	 * This implementation uses a crude numerical integration. Subclasses should
	 * override for better efficiency and accuracy!
	 * 
	 * @param min
	 *            inclusive
	 * @param max
	 *            exclusive
	 * @return
	 */
	@Override
	public double prob(double min, double max) {
		GridInfo gi = new GridInfo(min, max, 100);
		double sum = 0;
		for (double x : gi.getMidPoints()) {
			sum += density(x) * gi.getBucketSize();
		}
		return sum;
	}

	/**
	 * Create a random sample using rejection sampling. Sub-classes should
	 * over-ride this to do something more efficient!
	 */
	@Override
	public Double sample() {
		Gaussian1D g = new Gaussian1D(getMean(), getVariance());
		int MAX = 100000;
		for (int i = 0; i < MAX; i++) {
			Double x = g.sample();
			double u = random().nextDouble();
			double fx = density(x);
			if (fx == 0) {
				continue;
			}
			double gx = g.density(x);
			if (gx == 0) {
				// This implies a bad choice of sample generator!
				continue;
			}
			if (u > fx / gx)
				return x;
		}
		throw new FailureException("Failed to generate a sample from " + this);
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[mean="
				+ Printer.toStringNumber(getMean()) + ", var="
				+ Printer.toStringNumber(getVariance()) + "]";
	}
}
