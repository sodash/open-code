package com.winterwell.maths.stats.distributions.d1;

import com.winterwell.maths.stats.distributions.ADistribution;
import com.winterwell.maths.stats.distributions.IDistribution;
import com.winterwell.maths.vector.X;

import no.uib.cipr.matrix.Vector;

/**
 * Wrap an {@link IDistribution1D} so it implements the vector-based interface {@link IDistribution}.
 * 
 * Note: Only works with 1-d vectors.
 * @author daniel
 *
 */
public final class DistributionFrom1D extends ADistribution implements IDistribution {

	final IDistribution1D base;
	
	public DistributionFrom1D(IDistribution1D base) {
		this.base = base;
	}
	
	@Override
	public double prob(Vector minCorner, Vector maxCorner) {
		double min = minCorner.get(0);
		double max = maxCorner.get(0);
		return base.prob(min, max);
	}

	@Override
	public Vector sample() {
		Double d = base.sample();
		return new X(d);
	}
	
	@Override
	public Vector getVariance() {
		return new X(base.getVariance());
	}

	@Override
	public double density(Vector x) {
		assert x.size() == 1 : x;
		double v = x.get(0);
		return base.density(v);
	}

	@Override
	public int getDim() {
		return 1;
	}

	@Override
	public Vector getMean() {
		return new X(base.getMean());
	}

}
