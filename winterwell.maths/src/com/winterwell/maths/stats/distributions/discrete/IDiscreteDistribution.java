package com.winterwell.maths.stats.distributions.discrete;

import com.winterwell.maths.stats.distributions.IDistributionBase;

/**
 * A distribution over a discrete (possibly infinite) set of arbitrary values -
 * Strings, women, etc.
 * 
 * @author daniel
 */
public interface IDiscreteDistribution<X> extends IDistributionBase<X> {

	/**
	 * Returns the most likely element i.e. the mode
	 * 
	 * @return the most likely element in the distribution. Ties are broken
	 *         arbitrarily!
	 */
	X getMostLikely();

	/**
	 * Log probability of a point - need not be normalised
	 * 
	 * @param x
	 * @return
	 */
	double logProb(X x);

	/**
	 * The normalised probability (i.e. an actual probability). This may be
	 * expensive to compute. This will NOT normalise the underlying data. I.e.
	 * it will not affect the values returned by {@link #prob(Object)}.
	 * 
	 * @param obj
	 *            Must not be null
	 * @return in [0,1]
	 */
	public double normProb(X obj);

	/**
	 * Probability of a point - need not be normalised
	 * 
	 * @param x
	 *            Must not be null
	 * @return
	 */
	double prob(X x);

	/**
	 * Set the probability for a value (throws any normalisation)
	 * 
	 * @param obj
	 * @param value
	 */
	void setProb(X obj, double value) throws UnsupportedOperationException;

	/**
	 * @return Number of elements (some of which may have zero probability) -1
	 *         for infinite.
	 */
	int size();
}
