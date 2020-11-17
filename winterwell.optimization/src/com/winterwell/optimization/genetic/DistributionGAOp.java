package com.winterwell.optimization.genetic;

import java.util.Random;

import com.winterwell.maths.stats.distributions.discrete.IDiscreteDistribution;
import com.winterwell.utils.Utils;

/**
 * Simple GA operator created by wrapping a discrete distribution.
 * 
 * It just picks members by sampling. Mutate picks from scratch.
 * Crossover just picks one of its inputs with a 50/50 probability.
 * 
 * @testedby  DistributionGAOpTest}
 * @author daniel
 */
public class DistributionGAOp<T> implements IBreeder<T> {

	public DistributionGAOp(IDiscreteDistribution<T> dist) {
		this.dist = dist;
	}
	
	Random r = Utils.getRandom();
	
	@Override
	public T crossover(T a, T b) {
		return r.nextBoolean()? a : b;
	}

	@Override
	public T generate() {
		return dist.sample();
	}

	@Override
	public T mutate(T candidate) {
		return dist.sample();
	}

	final IDiscreteDistribution<T> dist;
	
	@Override
	public void setRandomSource(Random seed) {
		this.r = seed;
		dist.setRandomSource(seed);
	}

}
