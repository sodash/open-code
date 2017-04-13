package com.winterwell.maths.stats.distributions.discrete;

import com.winterwell.maths.stats.distributions.d1.ADistribution1D;
import com.winterwell.utils.MathUtils;

/**
 * Emits 1 with the given probability.
 * 
 * @author daniel
 * @see RandomChoice
 */
public final class Random01 extends ADistribution1D {

	final double p;

	public Random01(double p) {
		assert MathUtils.isProb(p) : p;
		this.p = p;
	}

	@Override
	public double density(double x) {
		return x == p ? Double.POSITIVE_INFINITY : 0;
	}

	@Override
	public double getMean() {
		return p;
	}

	@Override
	public double getVariance() {
		return 2 * (1 - p) * p;
	}

	@Override
	public Double sample() {
		return random().nextDouble() <= p ? 1.0 : 0.0;
	}

}
