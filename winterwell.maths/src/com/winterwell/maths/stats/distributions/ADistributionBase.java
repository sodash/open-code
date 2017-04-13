package com.winterwell.maths.stats.distributions;

import java.util.Random;

import com.winterwell.maths.ITrainable;
import com.winterwell.utils.Utils;

/**
 * Base class for building distributions - 1D, multi-dimensional, or
 * object-based.
 * <p>
 * This also provides support for {@link ITrainable}s, based on collecting data
 * together for batch-trained by prep(). This is done via protected methods (to
 * avoid public confusion), which sub-classes that implement ITrainable can make
 * public.
 * <p>
 * Training data is not serialised, ie. it is transient.
 * 
 * @author daniel
 * 
 * @param <X>
 *            the domain of the distribution
 */
public abstract class ADistributionBase<X> extends ATrainableBase<X, Object>
		implements IDistributionBase<X> {

	/**
	 * If true, the values are known to be normalised so they sum to 1.
	 */
	protected boolean normalised;

	private transient Random random;

	@Override
	public final boolean isNormalised() {
		return normalised;
	}

	@Override
	public void normalise() throws UnsupportedOperationException {
		if (isNormalised())
			return;
		// Needs to be implemented in each class :(
		throw new UnsupportedOperationException();
	}

	protected final Random random() {
		if (random == null) {
			random = Utils.getRandom();
		}
		return random;
	}

	@Override
	public void setRandomSource(Random randomSrc) {
		this.random = randomSrc;
	}
}
