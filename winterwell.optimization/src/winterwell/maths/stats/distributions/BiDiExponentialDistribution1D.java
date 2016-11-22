package winterwell.maths.stats.distributions;

import winterwell.maths.stats.distributions.d1.ADistribution1D;
import winterwell.maths.stats.distributions.d1.ExponentialDistribution1D;


/**
 * Bi-directional distribution with exponential decay. This has mean 0
 * and decays on both sides.
 * <p>
 * This is a scale-invariant distribution - i.e. it can generate
 * outliers reasonably well.
 * <p>
 * c.f. http://en.wikipedia.org/wiki/Exponential_distribution
 * @see ExponentialDistribution1D
 * @author Daniel
 */
public final class BiDiExponentialDistribution1D extends ADistribution1D {

	private final double lambda;

	/**
	 * A distribution with parameter lambda
	 * @param lambda
	 */
	public BiDiExponentialDistribution1D(double lambda) {
		this.lambda = lambda;
	}

	@Override
	public double density(double x) {
		return 0.5 * lambda * Math.exp(-lambda*x);
	}

	@Override
	public double getMean() {
		return 1.0 / lambda;
	}

	@Override
	public double getStdDev() {
		return 1.0 / lambda;
	}

	@Override
	public double getVariance() {
		return 1.0 / (lambda * lambda);
	}

	@Override
	public Double sample() {
		double v = random().nextDouble();
		double s = - Math.log(v)/lambda;
		assert s >= 0 : v+", "+s;
		if (random().nextBoolean()) return s;
		return - s;
	}

}
