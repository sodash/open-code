/**
 *
 */
package com.winterwell.maths.stats.distributions.discrete;

import java.io.Serializable;

import com.winterwell.maths.stats.distributions.MixtureModelBase;

/**
 * A additive (OR) mixture of discrete models.
 * 
 * @author Joe Halliwell <joe@winterwell.com>, Daniel
 * @testedby {@link DiscreteMixtureModelTest}
 *           {@link DiscreteMixtureModelClusteringTest}
 * 
 */
public class DiscreteMixtureModel<X, D extends IDiscreteDistribution<X>>
		extends MixtureModelBase<X, D> implements IDiscreteDistribution<X>,
		Serializable {

	@Override
	public int size() {		
		return -1; // we could try to get the size from the components. Overlaps would be an issue.
	}
	
	private static final long serialVersionUID = 1L;

	/**
	 * WARNING! Hokey (but efficient) implementation. This is *NOT* the actual most likely value, but
	 * rather the most likely of the the component distributions' most likely values.
	 * 
	 * JH: Even with 2 distributions, I think the only way to find the actual most likely value
	 * is to go through all of the candidates which a) we can't do; b) would be slow.
	 * 
	 * @see winterwell.maths.stats.distributions.discrete.IDiscreteDistribution#getMostLikely()
	 */
	@Override
	public X getMostLikely() {
		double bestProb = -1;
		X best = null;
		
		for (D d : getComponents()) {
			try {
				X candidate = d.getMostLikely();
				double candidateProb = prob(candidate);
				if (candidateProb > bestProb) {
					best = candidate;
					bestProb = candidateProb;
				}}
			catch (UnsupportedOperationException ex) {
				// Oh well
			}
		}
		return best;
		
		// Or possibly we just do this :/
		//return getComponents().getMostLikely().getMostLikely();
	}

	/**
	 * @see winterwell.maths.stats.distributions.discrete.IDiscreteDistribution#logProb(java.lang.Object)
	 */
	@Override
	public double logProb(X x) {
		return Math.log(prob(x));
	}

	@Override
	public double normProb(X x) {
		double prob = 0;
		for (D d : getComponents()) {
			prob += getComponents().normProb(d) * d.normProb(x);
		}
		return prob;
	}

	@Override
	public double prob(X x) {
		double prob = 0;
		ObjectDistribution<D> dists = getComponents();
		for (D dist : dists) {
			double px_d = dist.prob(x);
			double pd = getComponents().prob(dist);
			prob += pd * px_d;
		}
		return prob;
	}

	@Override
	public void setProb(X obj, double value)
			throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

	@Override
	protected double trainOneIteration3_prob(X x, int gi) {
		IDiscreteDistribution d = (IDiscreteDistribution) distributions[gi];
		return d.prob(x);
	}

}
