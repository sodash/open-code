package com.winterwell.maths.stats.distributions.discrete;

/**
 * Adds in a prior.
 * 
 * @author daniel
 * 
 */
public interface IDiscreteBayesian<X> extends IDiscreteDistribution<X> {

	IDiscreteDistribution getPrior();

}
