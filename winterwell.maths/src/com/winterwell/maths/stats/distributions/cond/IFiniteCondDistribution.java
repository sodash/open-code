package com.winterwell.maths.stats.distributions.cond;

import java.util.Collection;

/**
 * An {@link ICondDistribution} where there are a finite number of possible
 * conditioning cases, and a finite number of outcomes.
 * 
 * @author daniel
 * 
 * @param <X>
 * @param <C>
 */
public interface IFiniteCondDistribution<X, C> extends ICondDistribution<X, C> {

	/**
	 * Given an outcome, what are the possible causes?
	 * 
	 * @param outcome
	 * @return All Cs that might have produced outcome.
	 */
	Collection<C> getPossibleCauses(X outcome);	
	
}
