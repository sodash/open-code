package com.winterwell.maths.stats.distributions;

import java.util.Random;

import com.winterwell.maths.stats.distributions.d1.IDistribution1D;

/**
 * The common ground between the vector-based {@link IDistribution}, the
 * object-based {@link IDiscreteDistribution}, and the double-based
 * {@link IDistribution1D}
 * 
 * @author daniel
 * 
 * @param <X>
 *            e.g. String if this is a distribution over Strings.
 * 
 *            <p>
 *            <b>Copyright & license</b>: (c) Winterwell Associates Ltd, all
 *            rights reserved. This class is NOT formally a part of the
 *            winterwell.utils library. In particular, licenses for the
 *            winterwell.utils library do not apply to this file.
 *            </p>
 */
public interface IDistributionBase<X> {

	/**
	 * @return if true this distribution is normalised, false if not (or the
	 *         status is unknown)
	 */
	boolean isNormalised();

	/**
	 * Normalise the distribution. This can be very expensive depending on the
	 * class!
	 * 
	 * @throws UnsupportedOperationException
	 */
	void normalise() throws UnsupportedOperationException;

	/**
	 * @return a random sample from this distribution.
	 */
	X sample();

	/**
	 * Set the underlying random number generator. This should be called before
	 * using the distribution, otherwise the results are undefined.
	 * <p>
	 * If Random objects with the same seed are set, the distribution should
	 * output the same (repeatable) sequence of samples. This is useful in
	 * testing. When using this, be careful to avoid sharing Random objects
	 * between separate distributions.
	 */
	void setRandomSource(Random randomSrc);

}