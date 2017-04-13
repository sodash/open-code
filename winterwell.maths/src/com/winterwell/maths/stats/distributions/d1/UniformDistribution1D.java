/**
 * 
 */
package com.winterwell.maths.stats.distributions.d1;

import com.winterwell.maths.IScalarArithmetic;
import com.winterwell.utils.MathUtils;
import com.winterwell.utils.Printer;
import com.winterwell.utils.containers.Range;

/**
 * A uniform distribution
 * 
 * @author Daniel
 * 
 */
public class UniformDistribution1D extends ADistribution1D implements
		IScalarArithmetic {

	private final Range range;

	public UniformDistribution1D(Range range) {
		this.range = range;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see winterwell.maths.stats.distributions.IDistribution1D#density(double)
	 */
	@Override
	public double density(double x) {
		return range.contains(x) ? 1 / range.size() : 0;
	}

	@Override
	public double getConfidence(double totalWeight) {
		assert MathUtils.isProb(totalWeight);
		if (totalWeight == 0)
			return Double.NEGATIVE_INFINITY;
		if (totalWeight == 1)
			return range.high;
		return range.low + range.size() * totalWeight;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see winterwell.maths.stats.distributions.IDistribution1D#getMean()
	 */
	@Override
	public double getMean() {
		return (range.high + range.low) / 2;
	}

	@Override
	public Range getSupport() {
		return range;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see winterwell.maths.stats.distributions.IDistribution1D#getVariance()
	 */
	@Override
	public double getVariance() {
		double s = range.size();
		return s * s / 12;
	}

	@Override
	public UniformDistribution1D plus(double x) {
		return new UniformDistribution1D(range.plus(x));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see winterwell.maths.stats.distributions.IDistribution1D#sample()
	 */
	@Override
	public Double sample() {
		return range.low + random().nextDouble() * range.size();
	}

	@Override
	public IScalarArithmetic times(double x) {
		return new UniformDistribution1D(range.times(x));
	}

	@Override
	public String toString() {
		return Printer.toStringNumber(getMean()) + " ±"
				+ Printer.toStringNumber((range.size() / 2));
	}

}
