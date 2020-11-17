package com.winterwell.maths.stats.distributions.d1;

import java.util.ArrayList;
import java.util.List;

import com.winterwell.maths.ITrainable;
import com.winterwell.maths.stats.StatsUtils;
import com.winterwell.maths.vector.IMetric1D;
import com.winterwell.utils.MathUtils;
import com.winterwell.utils.TodoException;

import no.uib.cipr.matrix.Vector;

/**
 * A 1D gaussian over a non-Euclidean metric.
 * <p>
 * 
 * @warning Note that this is *not* a properly normalised distribution. But it's
 *          OK for most uses if you're careful.<br>
 *          - E.g.1 if the variance is high and we use a CyclicMetric, then the
 *          underlying Gaussian will assign some weight to areas which are
 *          outside the range.<br>
 *          - E.g.2 if we use a CyclicMetric and integrate over values outside
 *          the range, it could sum to much greater than 1.<br>
 * 
 * @testedby  MetricGaussian1DTest}
 * 
 * @author daniel
 * 
 */
public class MetricGaussian1D extends ADistribution1D implements
		ITrainable.Unsupervised<Double>, ITrainable.Unsupervised.Weighted<Double> {
	/**
	 * This is the distribution of distance from the mean.
	 */
	private Gaussian1D euclidean;
	private double mean;
	private final IMetric1D metric;

	public MetricGaussian1D(double mean, double var, IMetric1D metric) {
		super();
		this.euclidean = new Gaussian1D(0, var);
		this.mean = mean;
		this.metric = metric;
	}

	/**
	 * Create an untrained MetricGaussian1D
	 * 
	 * @param metric
	 */
	public MetricGaussian1D(IMetric1D metric) {
		this.metric = metric;
	}

	@Override
	public double density(double x) {
		double d = metric.dist(mean, x);
		// double density 'cos half the gaussian is on the negative
		// and we never get negative distances
		return 2 * euclidean.density(d);
	}

	@Override
	public void finishTraining() {
		if (dataWeights != null) {
			prep2_weights();
			return;
		}
		// get mean
		double[] xs = MathUtils.toArray(trainingData);
		List<Vector> vs = new ArrayList<Vector>(xs.length);
		for (double x : xs) {
			Vector v = metric.embed(x);
			vs.add(v);
		}
		Vector mv = StatsUtils.mean(vs);
		mean = metric.project(mv);
		// get variance
		// ...collect distance squared from mean
		double[] dx2 = new double[xs.length];
		for (int i = 0; i < xs.length; i++) {
			double x = xs[i];
			double d = metric.dist(x, mean);
			dx2[i] = d * d;
		}
		double var = StatsUtils.mean(dx2);
		// make gaussian
		euclidean = new Gaussian1D(0, var);
		// done
	}

	@Override
	public double getMean() {
		return mean;
	}

	@Override
	public double getVariance() {
		// I don't think this is really correct, though it is probably
		// OK for many uses. - DBW
		return euclidean.getVariance();
	}

	@Override
	public boolean isReady() {
		return euclidean != null;
	}

	private void prep2_weights() {
		if (true)
			throw new TodoException(); // TODO
		// get mean
		double[] xs = MathUtils.toArray(trainingData);
		// weight any unweighted data
		fillInWeights();
		double[] weights = MathUtils.toArray(dataWeights);
		List<Vector> vs = new ArrayList<Vector>(xs.length);
		for (double x : xs) {
			// TODO weight
			Vector v = metric.embed(x);
			vs.add(v);
		}
		Vector mv = StatsUtils.mean(vs);
		mean = metric.project(mv);
		// get variance
		// ...collect distance squared from mean
		double[] dx2 = new double[xs.length];
		for (int i = 0; i < xs.length; i++) {
			double x = xs[i];
			double d = metric.dist(x, mean);
			dx2[i] = d * d;
		}
		double var = StatsUtils.weightedMean(weights, dx2);
		// make gaussian
		euclidean = new Gaussian1D(0, var);
		// done
	}

	@Override
	public final void resetup() {
		super.resetup();
		euclidean = null;
	}

	@Override
	public Double sample() {
		double d = euclidean.sample();
		double x = mean + d;
		x = metric.canonical(x);
		return x;
	}

	@Override
	public void train(double[] weights, Iterable<? extends Double> wdata) {
		super.train(weights, wdata);
	}

	@Override
	public void train(Iterable<? extends Double> data) {
		super.train(data);
	}

	@Override
	public void train1(Double x) {
		super.train1(x);
	}
	
	@Override
	public void train1(Double data, double weight) {
		super.train1(data, weight);
	}

	@Override
	public void train1(Double x, Object tag, double weight) {
		super.train1(x, tag, weight);
	}

}
