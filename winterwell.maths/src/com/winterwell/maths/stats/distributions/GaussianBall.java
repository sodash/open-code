package com.winterwell.maths.stats.distributions;

import java.util.Arrays;

import com.winterwell.maths.timeseries.DataUtils;
import com.winterwell.utils.TodoException;

import no.uib.cipr.matrix.Matrix;
import no.uib.cipr.matrix.Vector;

/**
 * A Gaussian which has the same variance in all directions. TODO proper tests
 * for this! FIXME fix errors re variance = in each dimension versus variance =
 * in distance from mean.
 * 
 * @author daniel
 * 
 */
public final class GaussianBall extends ADistribution implements IGaussian {

	private final int dim;

	private final Vector mean;

	private double norm;

	private double var;

	/**
	 * @param var
	 *            The variance in each dimension.
	 */
	public GaussianBall(Vector mean, double var) {
		this.mean = mean;
		dim = mean.size();
		setVariance(var);
	}

	@Override
	public double density(Vector x) {
		// Just use the 1D on distance-from mean
		// TODO is this correct? No - consider very high dimensions
		double d = DataUtils.dist(x, mean);
		double p = norm * Math.exp(-0.5 * d * d / var);
		return p;
	}

	@Override
	public Matrix getCovar() {
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
		double[] v = new double[getDim()];
		Arrays.fill(v, var);
		return DataUtils.newVector(v);
	}

	@Override
	public Vector sample() {
		// loop over dims generating from underlying g
		double[] x = new double[getDim()];
		for (int i = 0; i < x.length; i++) {
			x[i] = random().nextGaussian() * Math.sqrt(var);
		}
		Vector v = DataUtils.newVector(x);
		v.add(mean);
		return v;
	}

	public void setVariance(double var) {
		this.var = var;
		norm = 1 / Math.pow(2 * Math.PI * var, dim / 2.0);
	}

	@Override
	public String toString() {
		return "N[" + mean + ", " + var + "]";
	}

}
