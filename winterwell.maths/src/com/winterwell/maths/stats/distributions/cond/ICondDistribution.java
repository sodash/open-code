package com.winterwell.maths.stats.distributions.cond;

import com.winterwell.maths.stats.distributions.IDistributionBase;
import com.winterwell.maths.stats.distributions.discrete.IDiscreteDistribution;

/**
 * 
 * @author daniel
 * 
 * @param <X>
 *            Type of objects we're assigning probabilities to
 * @param <C>
 *            Type of context info TODO limit to C extends Cntxt
 */
public interface ICondDistribution<X, C> {

	/**
	 * @param context
	 * @return TODO what should this be? IDistributionBase is too vague to be
	 *         useful.
	 */
	IDistributionBase<X> getMarginal(C context);

	default double logProb(X outcome, C context) {
		return Math.log(prob(outcome, context));
	}

	/**
	 * NB: This is a "true" normalised probability -- equivalent to {@link IDiscreteDistribution#normProb(Object)} 
	 * @param outcome
	 * @param context
	 * @return
	 */
	double prob(X outcome, C context);
	
	/**
	 * @return a random sample from this distribution.
	 */
	X sample(C context);

	/**
	 * Actually everything implements this WithExplanation extension -- but only some actually return an explanation. 
	 *
	 * @param <Outcome>
	 * @param <Context>
	 */
	public static interface WithExplanation<Outcome, Context> extends ICondDistribution<Outcome, Context> {
		/**
		 * 
		 * @param outcome
		 * @param context
		 * @param explain Can be null. If not null, this should add a node (or several linked nodes) with "explanatory" info about the output.
		 * The explanation will (typically) only cover a single outcome-context pair.
		 * @return P(outcome|context) Can also return NaN
		 * NB: This is a normalised probability.
		 */
		double probWithExplanation(Outcome outcome, Context context, ExplnOfDist explain);
		
		@Override
		default double prob(Outcome outcome, Context context) {
			return probWithExplanation(outcome, context, null);
		}
	}
	
}