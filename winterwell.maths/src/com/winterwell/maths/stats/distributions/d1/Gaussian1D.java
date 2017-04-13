package com.winterwell.maths.stats.distributions.d1;


import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.special.Erf;

import com.winterwell.maths.IScalarArithmetic;
import com.winterwell.utils.MathUtils;
import com.winterwell.utils.Printer;
import com.winterwell.utils.containers.Range;

/**
 * A one dimensional Gaussian.
 * 
 * <h3>Notes on arithmetic with 1D Gaussians</h3>
 * If X, Y are independent Gaussian random variables with means x,y, and variances vx,vy
 * 
 * aX + bY is Gaussian 
 * 
 * Z = X.Y is Gaussian with variance 
 * vz = vx.vy/(vx+vy), 
 * and mean z = (vz/vx).x + (vz/vy).y  
 * 
 * See http://www.tina-vision.net/docs/memos/2003-003.pdf, 
 * or https://www.cs.nyu.edu/~roweis/notes/gaussid.pdf
 * 
 * 
 * @author daniel
 * @testedby {@link Gaussian1DTest}
 */
public final class Gaussian1D extends ADistribution1D implements
		IDistribution1D, IScalarArithmetic {
	
	private static final double RT_2 = Math.sqrt(2);

	private static final double RT_2_PI = Math.sqrt(2 * Math.PI);

	/**
	 * Compute the Error Function. 
	 * http://en.wikipedia.org/wiki/Error_function
	 * 
	 * Just uses Apache Common's implementation.
	 */
	public static double erf(double x) {
		return Erf.erf(x);
	}

	private final double mean;

	private final double norm;
	private final double stdDev;
	private final double var;

	/**
	 * 
	 * @param mean
	 * @param var
	 *            Can be zero, in which case the distribution is just a spike.
	 */
	public Gaussian1D(double mean, double var) {
		assert MathUtils.isFinite(mean);
		assert MathUtils.isFinite(var);
		assert var >= 0 : var;
		this.mean = mean;
		this.var = var;
		stdDev = Math.sqrt(var);
		norm = 1 / (stdDev * RT_2_PI);
		assert var == 0 || MathUtils.isFinite(norm);
		// yes we are normalised
		normalised = true;
	}

	// TODO test
	/*
	 * (non-Javadoc)
	 * 
	 * @see winterwell.maths.stats.distributions.IDistribution1D#prob(double)
	 */
	@Override
	public double density(double x) {
		assert MathUtils.isFinite(x);
		if (var == 0)
			return (x == mean) ? Double.POSITIVE_INFINITY : 0;
		double a = MathUtils.sq(x - mean) / (-2 * var);
		assert MathUtils.isFinite(a) : this; // trying to debug
		double exp = Math.exp(a);
		assert MathUtils.isFinite(exp);
		double p = exp * norm;
		assert p >= 0 : x + " " + this;
		return p;
	}

	@Override
	public double getConfidence(double totalWeight) {
		// HACK: a few useful z-values
		double z = -1;
		// note: 90% confidence interval = 5% to 95%
		// 95% confidence interval = 2.5% to 97.5%
		// 99% confidence interval = 0.5% to 99.5%
		if (totalWeight == 0.95 || totalWeight == 0.05) {
			z = 1.64;
		} else if (totalWeight == 0.975 || totalWeight == 0.025) {
			z = 1.96;
		} else if (totalWeight == 0.995 || totalWeight == 0.005) {
			z = 2.58;
		}						
		if (z == -1) {
			NormalDistribution nd = new NormalDistribution(null, getMean(), getStdDev());
			double v = nd.inverseCumulativeProbability(totalWeight);
			return v;
		}
		double pm = totalWeight > 0.5 ? 1 : -1;
		return getMean() + pm * z * getStdDev();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see winterwell.maths.stats.distributions.IDistribution1D#getMean()
	 */
	@Override
	public double getMean() {
		return mean;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see winterwell.maths.stats.distributions.IDistribution1D#getStdDev()
	 */
	@Override
	public double getStdDev() {
		return stdDev;
	}

	@Override
	public Range getSupport() {
		// Arbitrary numbers - this may be over-generous
		return new Range(mean - 100 * stdDev, mean + 100 * stdDev);
	}

	@Override
	public double getVariance() {
		return var;
	}

	public double logDensity(double x) {
		double a = MathUtils.sq(x - mean) / (-2 * var);
		double ld = Math.log(norm) + a;
		assert MathUtils.equalish(ld, Math.log(density(x))) : x + " " + ld
				+ " " + Math.log(density(x));
		return ld;
	}

	@Override
	public Gaussian1D plus(double x) {
		return new Gaussian1D(mean + x, var);
	}

	@Override
	public double prob(double min, double max) {
		assert min <= max;
		// If variance is zero return one if mean is in region, zero otherwise
		if (var == 0)
			return (min <= mean && mean <= max) ? 1 : 0;
		
		// Otherwise we'll calculate based on ERF
		// http://en.wikipedia.org/wiki/Gaussian_distribution
		double a = erf((min - mean) / (RT_2 * stdDev));
		double b = erf((max - mean) / (RT_2 * stdDev));
		double result = 0.5 * (b - a);
		if (result < 0) {
			assert MathUtils.equalish(result, 0) : result;
			return 0;
		}
		return result;		
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see winterwell.maths.stats.distributions.IDistribution1D#sample()
	 */
	@Override
	public Double sample() {
		return stdDev == 0 ? mean : (random().nextGaussian() * stdDev) + mean;
	}

	@Override
	public Gaussian1D times(double x) {
		return new Gaussian1D(mean * x, var * x * x);
	}

	@Override
	public String toString() {
		return "N(" + Printer.toString(mean) + ", " + Printer.toString(var)
				+ ")";
	}

}
