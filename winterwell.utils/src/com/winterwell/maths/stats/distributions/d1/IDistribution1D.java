package com.winterwell.maths.stats.distributions.d1;

import com.winterwell.maths.stats.distributions.IDistributionBase;
import com.winterwell.utils.containers.Range;

/**
 * 
 * @author daniel
 *         <p>
 *         <b>Copyright & license</b>: (c) Winterwell Associates Ltd, all rights
 *         reserved. This class is NOT formally a part of the winterwell.utils
 *         library. In particular, licenses for the winterwell.utils library do
 *         not apply to this file.
 *         </p>
 */
public interface IDistribution1D extends IDistributionBase<Double> {

	/**
	 * The probability density at a point
	 * 
	 * @param x
	 * @return Warning: could be {@link Double#POSITIVE_INFINITY} if the
	 *         distribution contains point-masses.
	 * @see #prob(double, double) which is better behaved (no infinities)
	 */
	double density(double x);

	/**
	 * The (approximate) point at which the integrated density = the given
	 * confidence value.
	 * <p>
	 * This is also known as the "inverse Cumulative Density Function (CDF)"
	 * or in scipy, the Percent Point Function (ppf).
	 * <p>
	 * To get the 95% confidence interval, call this twice with 0.025 and 0.975.
	 * 
	 * @param totalWeight
	 *            e.g. 0.05, and 0.95 would give 90% confidence intervals,
	 *            because the weight between these points is 0.9. Must be in
	 *            [0,1].
	 * @return may return +/- infinity for 0 or 1
	 */
	double getConfidence(double totalWeight);

	double getMean();

	double getStdDev();

	/**
	 * @return area where this distribution has density > machine-epsilon. This
	 *         may be a wild over-estimate - e.g. [-infinity, +infinity] which
	 *         could be true, or turn out to really be [0,1].
	 */
	Range getSupport();

	double getVariance();

	/**
	 * Probability for a region, i.e. the integrated density over the region
	 * [min, max).
	 * 
	 * @param min
	 *            inclusive, can be -infinity
	 * @param max
	 *            exclusive, can be +infinity
	 * @return
	 */
	double prob(double min, double max);

	/**
	 * Create a random sample from this distribution.
	 */
	@Override
	Double sample();

}
