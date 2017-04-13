package com.winterwell.maths.stats.distributions.d1;

import org.apache.commons.math3.distribution.GammaDistribution;
import org.apache.commons.math3.special.Gamma;

import com.winterwell.utils.TodoException;
import com.winterwell.utils.Utils;

/**
 * Wikipedia: In probability theory and statistics, the gamma distribution is a
 * two-parameter family of continuous probability distributions. It has a scale
 * parameter theta and a shape parameter k. If k is an integer then the
 * distribution represents the sum of k independent exponentially distributed
 * random variables, each of which has a mean of theta (which is equivalent to a
 * rate parameter of 1/theta) .
 * 
 * The gamma distribution is frequently a probability model for waiting times;
 * for instance, in life testing, the waiting time until death is a random
 * variable that is frequently modeled with a gamma distribution.[1]
 * <p>
 * Useful as a prior for Bayesian work with normally distributed variables.
 * 
 * @author daniel
 * @testedby {@link GammaDistribution1DTest}
 */
public final class GammaDistribution1D extends ADistribution1D {

	private final GammaDistribution gdi;
	private final double scaleTheta;
	private final double shapeK;

	public GammaDistribution1D(double shapeK, double scaleTheta) {
		assert shapeK > 0 && scaleTheta > 0;
		this.shapeK = shapeK;
		this.scaleTheta = scaleTheta;
		gdi = new GammaDistribution(shapeK, 1 / scaleTheta);
	}

	@Override
	public double density(double x) {
		// FIXME the output from this looks wrong!
		if (true)
			throw new TodoException();
		double f1 = Math.pow(x, shapeK - 1);
		double gk = Gamma.logGamma(shapeK);
		double f2 = Math.exp(-x / scaleTheta - gk);
		double f3 = Math.pow(scaleTheta, shapeK);
		double d = f1 * f2 / f3;
		assert d >= 0 && d <= 1 : d;
		return d;
	}

	@Override
	public double getMean() {
		return scaleTheta * shapeK;
	}

	@Override
	public double getVariance() {
		return shapeK * scaleTheta * scaleTheta;
	}

	@Override
	public double prob(double min, double max) {
		try {
			if (min <= 0)
				return gdi.cumulativeProbability(max);
			if (max == Double.POSITIVE_INFINITY)
				return 1 - gdi.cumulativeProbability(min);
			return gdi.cumulativeProbability(min, max);
		} catch (Exception e) {
			throw Utils.runtime(e);
		}
	}

}
