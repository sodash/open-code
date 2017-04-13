package com.winterwell.maths.stats.distributions.cond;

import com.winterwell.maths.ITrainable;
import com.winterwell.maths.stats.distributions.IDistributionBase;
import com.winterwell.maths.stats.distributions.discrete.IDiscreteDistribution;

/**
 * Wrap an {@link IDiscreteDistribution} to make it into an
 * {@link ICondDistribution} which just ignores the context.
 * <p>
 * Warning: Although this _says_ it's trainable, that may not be true
 * (depending on what it's wrapping). Test {@link #dist} to be sure. 
 * 
 * @author daniel
 * 
 * @param <X>
 */
public final class UnConditional<X> implements
		ICondDistribution.WithExplanation<X, Cntxt>, ITrainable.Supervised<Cntxt, X> {

	public final IDiscreteDistribution<X> dist;

	public UnConditional(IDiscreteDistribution<X> dist) {
		super();
		assert dist != null;
		this.dist = dist;
	}
	
	@Override
	public String toString() {
		return dist.toString();
	}

	@Override
	public void finishTraining() {
		((ITrainable) dist).finishTraining();
	}

	@Override
	public IDistributionBase<X> getMarginal(Cntxt ignored) {
		return dist;
	}

	@Override
	public boolean isReady() {
		return ((ITrainable) dist).isReady();
	}

	@Override
	public double logProb(X outcome, Cntxt context) {
		return dist.logProb(outcome);
	}

	@Override
	public double prob(X outcome, Cntxt context) {
		return dist.normProb(outcome);
	}

	@Override
	public void resetup() {
		((ITrainable) dist).resetup();
	}

	@Override
	public X sample(Cntxt context) {
		return dist.sample();
	}

	@Override
	public void train1(Cntxt x, X tag, double weightIgnored) {
		((ITrainable.Unsupervised) dist).train1(tag);
	}

	@Override
	public double probWithExplanation(X outcome, Cntxt context, ExplnOfDist explain) {
		return prob(outcome, context);
	}

}
