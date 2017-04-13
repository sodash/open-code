package com.winterwell.maths.stats.distributions.d1;

/**
 * Distribution with exponential decay.
 * <p>
 * This is a scale-invariant distribution - i.e. it can generate outliers
 * reasonably well.
 * <p>
 * c.f. http://en.wikipedia.org/wiki/Exponential_distribution
 * 
 * @author Daniel
 * 
 */
public final class ExponentialDistribution1D extends ADistribution1D {

	/**
	 * @return the exponent, which is 1/mean
	 */
	public double getLambda() {
		return lambda;
	}
	
	private final double lambda;

	/**
	 * A distribution with parameter lambda - hence mean 1/lambda and variance
	 * 1/lambda^2.
	 * <p>
	 * To fit this: normally, just use the sample mean, which is the Maximum-Likelihood solution.
	 * c.f. http://en.wikipedia.org/wiki/Exponential_distribution#Parameter_estimation
	 * 
	 * 
	 * @param lambda
	 */
	public ExponentialDistribution1D(double lambda) {
		this.lambda = lambda;
		assert lambda > 0 : lambda;
	}

	@Override
	public double density(double x) {
		if (x < 0)
			return 0;
		return lambda * Math.exp(-lambda * x);
	}

	@Override
	public double getMean() {
		return 1 / lambda;
	}

	@Override
	public double getStdDev() {
		return 1 / lambda;
	}

	@Override
	public double getVariance() {
		return 1 / (lambda * lambda);
	}

	@Override
	public Double sample() {
		double v = random().nextDouble();
		double s = -Math.log(v) / lambda;
		assert s >= 0 : v + ", " + s;
		return s;
	}

}
