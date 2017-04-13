package com.winterwell.maths.stats.distributions.discrete;

import com.winterwell.maths.stats.distributions.d1.ADistribution1D;
import com.winterwell.utils.MathUtils;
import com.winterwell.utils.containers.Range;

public class Dist1DFromDiscrete extends ADistribution1D {

	@Override
	public Range getSupport() {
		if (base instanceof RandomChoice) {
			return new Range(0,1);
		}
		if (base instanceof IndexedDistribution) {
			return new Range(0, ((IndexedDistribution) base).size());
		}
		return super.getSupport();
	}
	
	public IDiscreteDistribution getBase() {
		return base;
	}
	
	@Override
	public String toString() {
		return base.toString();
	}

	private static final double SPIKE = 100;
	IDiscreteDistribution base;
	
	public Dist1DFromDiscrete(IDiscreteDistribution base) {
		this.base= base;
	}
	
	@Override
	public Double sample() {
		Object s = base.sample();
		return numberFromBaseObject(s);
	}

	@Override
	public double density(double x) {
		double p;
		if (base instanceof IndexedDistribution) {
			// a minor efficiency gain
			IndexedDistribution idistro = (IndexedDistribution) base;
			p = idistro.probFromIndex((int) x);
		} else {
			Object bo = baseObjectFromNumber(x);
			p = base.prob(bo);
		}
		// Let's not return infinity. How spiky should we make this?		
		return p*SPIKE;
	}

	@Override
	public double getMean() {
		if (base instanceof RandomChoice) {
			return ((RandomChoice) base).prob(true);
		}
		if (base instanceof IFiniteDistribution) {
			// TODO
		}		
		throw new UnsupportedOperationException(base.getClass()+" "+base);
	}

	@Override
	public double getVariance() {
		if (base instanceof RandomChoice) {
			double p = ((RandomChoice) base).prob(true);
			// E(X^2) - E(X)^2
			return p - p*p;
		}
		if (base instanceof IFiniteDistribution) {
			// TODO
		}
		throw new UnsupportedOperationException(base.getClass()+" "+base);
	}

	/**
	 * Convert from the base-space into numbers
	 * @param ml
	 * @return
	 */
	public double numberFromBaseObject(Object ml) {
		if (base instanceof RandomChoice) {
			boolean on = (Boolean) ml;
			return on? 1 : 0;
		} else if (base instanceof IndexedDistribution) {
			IndexedDistribution idistro = (IndexedDistribution) base;
			return idistro.getIndex().indexOf(ml);
		}
		// hope base uses numbers
		return MathUtils.num(ml);		
	}

	/**
	 * @param v
	 * @return the underlying discrete-distribution probability
	 */
	public double prob(double v) {
		Object b = baseObjectFromNumber(v);
		return base.prob(b);
	}

	public Object baseObjectFromNumber(double x) {
		if (base instanceof RandomChoice) {
			return x > 0.5;
		}
		if (base instanceof IndexedDistribution) {
			IndexedDistribution idistro = (IndexedDistribution) base;
			Object bo = idistro.getIndex().get((int) x);
			return bo;
		}
		// hope base uses numbers
		return x;
	}
	
}