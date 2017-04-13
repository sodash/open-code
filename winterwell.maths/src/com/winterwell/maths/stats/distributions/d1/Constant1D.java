package com.winterwell.maths.stats.distributions.d1;

import java.util.Random;

import com.winterwell.maths.IScalarArithmetic;
import com.winterwell.utils.containers.Range;

/**
 * Trivial distribution which always returns the same value.
 * 
 * @author daniel
 */
public final class Constant1D implements IDistribution1D, IScalarArithmetic {

	private double k;

	public Constant1D(double k) {
		this.k = k;
	}

	@Override
	public double density(double x) {
		return x == k ? Double.POSITIVE_INFINITY : 0;
	}

	@Override
	public double getConfidence(double totalWeight) {
		return totalWeight > 0 ? k : Double.NEGATIVE_INFINITY;
	}

	@Override
	public double getMean() {
		return k;
	}

	@Override
	public double getStdDev() {
		return 0;
	}

	@Override
	public Range getSupport() {
		return new Range(k, k);
	}

	@Override
	public double getVariance() {
		return 0;
	}

	@Override
	public boolean isNormalised() {
		return true;
	}

	@Override
	public void normalise() throws UnsupportedOperationException {
		return;
	}

	@Override
	public Constant1D plus(double x) {
		return new Constant1D(k + x);
	}

	@Override
	public double prob(double min, double max) {
		if (k >= min && k < max)
			return 1;
		return 0;
	}

	@Override
	public Double sample() {
		return k;
	}

	@Override
	public void setRandomSource(Random randomSrc) {
		// ignore
	}

	@Override
	public Constant1D times(double x) {
		return new Constant1D(k * x);
	}

}
