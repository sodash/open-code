package com.winterwell.maths.stats.distributions.cond;

import java.util.Random;

import com.winterwell.maths.stats.distributions.ATrainableBase;
import com.winterwell.maths.stats.distributions.IDistributionBase;
import com.winterwell.utils.Utils;

/**
 * Base class for building conditional distributions.
 * Trainable ones should implement
 * ITrainable.Supervised<C,X>
 * 
 * @author daniel
 * 
 * @param <X>
 *            probabilities over X (e.g. X=String or Tkn for words)
 * @param <C>
 *            condition on objects of type C (e.g. C = Cntxt)
 */
public abstract class ACondDistribution<X, C> extends ATrainableBase<C, X>
		implements ICondDistribution.WithExplanation<X, C> {

	private transient Random random;
	
	// NB: Override to actually return an explanation!
	@Override
	public double probWithExplanation(X outcome, C context, ExplnOfDist explain) {
		return prob(outcome, context);
	}
	
	@Override
	public IDistributionBase<X> getMarginal(C context)
			throws UnsupportedOperationException {
		throw new UnsupportedOperationException(getClass().getName());
	}

	@Override
	public double logProb(X outcome, C context) {
		return Math.log(prob(outcome, context));
	}

	protected final Random random() {
		if (random == null) {
			random = Utils.getRandom();
		}
		return random;
	}

	@Override
	public X sample(C context) {
		return getMarginal(context).sample();
	}

}
