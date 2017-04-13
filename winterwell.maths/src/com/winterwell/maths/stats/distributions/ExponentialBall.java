package com.winterwell.maths.stats.distributions;

import java.util.Random;

import com.winterwell.maths.stats.distributions.d1.ExponentialDistribution1D;
import com.winterwell.maths.timeseries.DataUtils;
import com.winterwell.utils.TodoException;

import no.uib.cipr.matrix.Vector;

/**
 * A distribution which is unbiased in terms of direction and has exponential
 * decay in terms of distance from mean.
 * <p>
 * This is scale invariant so, there is no assumption (as with the Gaussian)
 * that deviances are likely to be small - i.e. it can return outliers.
 * <p>
 * Useful for: <br>
 * - random movements in vectors in a GA
 * 
 * @author Daniel
 */
public class ExponentialBall extends ADistribution {

	private final int dim;
	final RandomDirection dir;

	final ExponentialDistribution1D len;

	private Vector mean;

	public ExponentialBall(int dim, double lambda) {
		this.dim = dim;
		dir = new RandomDirection(dim);
		len = new ExponentialDistribution1D(lambda);
		mean = DataUtils.newVector(dim);
	}

	@Override
	public double density(Vector x) {
		// TODO Auto-generated method stub
		throw new TodoException();
	}

	@Override
	public int getDim() {
		return dim;
	}

	@Override
	public Vector getMean() {
		return mean;
	}

	@Override
	public Vector getVariance() {
		// TODO Auto-generated method stub
		throw new TodoException();
	}

	@Override
	public Vector sample() {
		Vector v = dir.sample();
		double length = len.sample();
		v.scale(length);
		v.add(mean);
		return v;
	}

	public void setMean(Vector mean) {
		assert mean.size() == dim : mean.size() + " vs " + dim;
		this.mean = mean;
	}

	@Override
	public void setRandomSource(Random randomSrc) {
		dir.setRandomSource(randomSrc);
		len.setRandomSource(randomSrc);
	}

}
