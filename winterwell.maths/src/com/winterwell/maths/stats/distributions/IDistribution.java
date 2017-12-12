package com.winterwell.maths.stats.distributions;

import com.winterwell.utils.containers.Range;

import no.uib.cipr.matrix.Matrix;
import no.uib.cipr.matrix.Vector;

/**
 * Interface for a probability model over real-valued vectors.
 * <p>
 * IDistributions are typically mutable and trainable.
 * 
 * @author Daniel Winterstein
 */
public interface IDistribution extends IDistributionBase<Vector> {

	/**
	 * @param x
	 * @return The probability density at a point. Warning: may be
	 *         {@link Double#POSITIVE_INFINITY}!
	 * @see #prob(Vector, Vector) which is never infinite, but often slower.
	 */
	double density(Vector x);

	/**
	 * @return The covariance matrix.
	 */
	Matrix getCovar();

	/**
	 * @return number of dimensions.
	 */
	int getDim();

	/**
	 * This should (usually) be backed by the distribution object. I.e. changes
	 * will "write through".
	 * 
	 * @return
	 */
	Vector getMean();

	/**
	 * @return the range for each dimension. Outside this range, the
	 *         distribution is always zero, or at least, below machine epsilon.
	 *         Can use infinity.
	 */
	Range[] getSupport();

	/**
	 * @return variance of each dimension. This should not generally be edited.
	 */
	Vector getVariance();

	/**
	 * @param x
	 * @return Log of the density. This can help avoid numerical errors when
	 *         working with low-densities.
	 * @see #density(Vector)
	 */
	double logDensity(Vector x);

	/**
	 * Probability over a cuboid region
	 * 
	 * @param minCorner
	 * @param maxCorner
	 * @return probability of being inside the region.
	 */
	double prob(Vector minCorner, Vector maxCorner);

}