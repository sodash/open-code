package com.winterwell.maths.stats.distributions;

import com.winterwell.maths.ITrainable;
import com.winterwell.maths.ITrainable.IHandleWeights;
import com.winterwell.maths.matrix.DiagonalMatrix;
import com.winterwell.maths.stats.StatsUtils;
import com.winterwell.maths.stats.distributions.d1.Gaussian1D;
import com.winterwell.maths.timeseries.DataUtils;
import com.winterwell.utils.MathUtils;

import no.uib.cipr.matrix.Matrix;
import no.uib.cipr.matrix.Vector;

/**
 * A multi-dimensional Gaussian with zero off diagonal covariance. I.e. the
 * straightforward cross product of several 1D Gaussians. TODO test!
 * 
 * TODO training // variance in each dimension Vector var =
 * StatsUtils.weightedVar(weights, data); // Don't set variance in any axis to 0
 * // FIXME: Use a more sensible value here - perhaps a fraction of the mean if
 * it is non-zero - or user settable for (int i = 0; i < var.size(); i++) { if
 * (var.get(i) == 0) var.set(i, 0.001); // FIXME this is a hack for the cows! if
 * (var.get(i) > 1000) var.set(i, 1000); } g.getVariance().set(var);
 * 
 * @tested {@link AxisAlignedGaussianTest}
 * @author daniel
 */
public final class AxisAlignedGaussian extends ADistribution implements
		IGaussian, IHandleWeights<Vector>, ITrainable.Unsupervised<Vector> {

	private final Vector mean;
	private final Vector var;

	/**
	 * This is for gaussians you intend to train then fit.
	 * 
	 * @param dim
	 */
	public AxisAlignedGaussian(int dim) {
		this(DataUtils.newVector(dim), DataUtils.newVector(dim));
		pleaseTrainFlag = true;

	}

	/**
	 * 
	 * @param mean
	 *            This is copied.
	 * @param var
	 *            This is copied.
	 */
	public AxisAlignedGaussian(Vector mean, Vector var) {
		this.mean = mean.copy();
		this.var = var.copy();
		assert DataUtils.isFinite(mean);
		assert DataUtils.min(var) >= 0;
	}

	@Override
	public double density(Vector x) {
		assert x.size() == var.size() : x.size() + " != " + var.size();
		// remove mean
		x = x.copy().add(-1, mean);
		// variance in each dim
		double p = 1;
		for (int i = 0; i < getDim(); i++) {
			double vi = var.get(i);
			assert vi >= 0;
			Gaussian1D g1d = new Gaussian1D(0, vi);
			double pi = g1d.density(x.get(i));
			// Infinity * 0 = Nan
			// If we hit a zero, just return it
			if (pi == 0)
				return 0;
			p *= pi;
		}
		// Density is allowed to be infinite nowadays
		// assert p >= 0 : p;
		return p;
	}

	@Override
	public void finishTraining() {
		mean.zero();
		var.zero();
		if (dataWeights != null) {
			// weight any unweighted pre-existing data as 1
			fillInWeights();
			double[] weights = MathUtils.toArray(dataWeights);
			Vector m = StatsUtils.weightedMean(weights, trainingData);
			mean.add(m);
			Vector v = StatsUtils.weightedVar(weights, trainingData);
			var.add(v);
			assert DataUtils.min(var) >= 0;
			super.finishTraining();
			return;
		}
		Vector m = StatsUtils.mean(trainingData);
		mean.add(m);
		Vector v = StatsUtils.var(trainingData);
		var.add(v);
		assert DataUtils.min(var) >= 0;
		super.finishTraining();
	}

	@Override
	public Matrix getCovar() {
		return new DiagonalMatrix(var);
	}

	@Override
	public int getDim() {
		return var.size();
	}

	@Override
	public Vector getMean() {
		return mean;
	}

	@Override
	public Vector getVariance() {
		return var;
	}

	@Override
	public boolean isReady() {
		return super.isReady();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * winterwell.maths.stats.distributions.ADistribution#prob(no.uib.cipr.matrix
	 * .Vector, no.uib.cipr.matrix.Vector)
	 */
	@Override
	public double prob(Vector minCorner, Vector maxCorner) {
		assert minCorner.size() == var.size();
		assert maxCorner.size() == var.size();
		// variance in each dim
		double p = 1;
		// ??By Fubini's theorem...
		for (int i = 0; i < getDim(); i++) {
			double vi = var.get(i);
			Gaussian1D g1d = new Gaussian1D(mean.get(i), vi);
			double min = minCorner.get(i);
			double max = maxCorner.get(i);
			assert min <= max;
			double pi = g1d.prob(min, max);
			p *= pi;
		}
		assert p >= 0;
		return p;
	}

	/**
	 * Setup with random mean and variance in -1,1]
	 */
	@Override
	public void resetup() {
		mean.zero();
		var.zero();
		// VectorUtils.abs(var);
		// assert VectorUtils.min(var) >= 0;
		super.resetup();
	}

	@Override
	public Vector sample() {
		// sample in each dim
		Vector x = DataUtils.newVector(getDim());
		for (int i = 0; i < getDim(); i++) {
			double s = random().nextGaussian();
			s = s * Math.sqrt(var.get(i));
			x.set(i, s);
		}
		// add mean
		x.add(mean);
		return x;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + " N(" + getMean() + ", "
				+ getVariance() + ")";
	}

	@Override
	public void train(double[] weights, Iterable<? extends Vector> wdata) {
		super.train(weights, wdata);
	}

	@Override
	public void train(Iterable<? extends Vector> data) {
		super.train(data);
	}

	@Override
	public void train1(Vector x) {
		super.train1(x);
	}

	@Override
	public void train1(Vector x, Object tag, double weight) {
		super.train1(x, tag, weight);
	}

	@Override
	public void train1weighted(double weight, Vector data) {
		super.train1weighted(weight, data);
	}
}
