package com.winterwell.maths.stats.distributions.discrete;

import java.util.Iterator;

/**
 * Choose the sub-model which maximises a chosen outcome.
 * 
 * This is like using a mixture model and using EM to fit the weightings so that one part gets all the weight.
 *
 * @author daniel
 *
 */
public class MaxOrDistribution<X> extends AFiniteDistribution<X> {

	private X prefOutCome;
	private IFiniteDistribution<X> best;

	public MaxOrDistribution(X prefOutcome) {
		this.prefOutCome = prefOutcome;
	}

	@Override
	public Iterator<X> iterator() throws UnsupportedOperationException {
		return best.iterator();
	}

	@Override
	public int size() {
		return best.size();
	}

	@Override
	public double prob(X x) {
		return best.prob(x);
	}

	/**
	 * Shall we take this dist as the one that maximises the preferred outcome?
	 * @param dist
	 * @return true if dist is adopted
	 */
	public boolean max(IFiniteDistribution<X> dist) {
		if (best==null) {
			best = dist;
			return true;
		}
		double pa = best.normProb(prefOutCome);
		double pb = dist.normProb(prefOutCome);
		if (pb > pa) {
			best = dist;
			return true;
		}
		return false;
	}


	
}
