package com.winterwell.maths.stats.distributions;

import java.util.ArrayList;
import java.util.List;

import com.winterwell.maths.stats.StatsUtils;
import com.winterwell.utils.containers.Containers;
import com.winterwell.utils.containers.Range;

import no.uib.cipr.matrix.Matrix;
import no.uib.cipr.matrix.Vector;

/**
 * Helper class for making vector-based {@link IDistribution}s
 * 
 * @author Daniel
 * 
 */
public abstract class ADistribution extends ADistributionBase<Vector> implements
		IDistribution {

	static Vector getVariance(IDistribution dist) {
		int n = 1000; // should this grow with the dims? let's hope for
						// independent dims
		List<Vector> pts = new ArrayList<Vector>(n);
		for (int i = 0; i < n; i++) {
			Vector x = dist.sample();
			pts.add(x);
		}
		return StatsUtils.var(pts);
	}

	public ADistribution() {
	}

	@Override
	public Matrix getCovar() throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

	@Override
	public Range[] getSupport() {
		Range[] support = new Range[getDim()];
		for (int i = 0; i < support.length; i++) {
			support[i] = Range.REALS;
		}
		return support;
	}

	/**
	 * @return variance of each dimension.
	 *         <p>
	 *         The base implementation uses sampling - override this where
	 *         possible to be more efficient!
	 */
	@Override
	public Vector getVariance() {
		return getVariance(this);
	}

	@Override
	public double logDensity(Vector x) {
		// override to do better
		double d = density(x);
		return Math.log(d);
	}

	/**
	 * Probability over a cuboid region
	 * <p>
	 * This implementation uses a crude monte-carlo approach. Subclasses should
	 * override for better efficiency and accuracy.
	 * 
	 * @param minCorner
	 * @param maxCorner
	 * @return
	 */
	@Override
	public double prob(Vector minCorner, Vector maxCorner) {
		// Monte Carlo!
		// Should use numerical integration instead for low dims
		// Two loops: 2nd is for low probs
		// Note: there is a very slight bias introduced by ditching the 1st
		// count if it is zero
		for (int N : new int[] { 1000, 100000 }) {
			int count = 0;
			for (int i = 0; i < N; i++) {
				Vector s = sample();
				if (UniformCuboidDistribution.isInside(s, minCorner, maxCorner)) {
					count++;
				}
			}
			if (count != 0)
				return (1.0 * count) / N;
		}
		return 0;
	}

	@Override
	protected void train(double[] weights, Iterable<? extends Vector> wdata) {
		super.train(weights, wdata);
		assert Containers.last(trainingData).size() == getDim();
	}

	@Override
	protected void train(Iterable<? extends Vector> data) {
		super.train(data);
		assert Containers.last(trainingData).size() == getDim();
	}

	@Override
	protected void train1(Vector x) {
		assert x.size() == getDim();
		super.train1(x);
	}

	@Override
	protected void train1(Vector x, Object tag, double weight) {
		assert x.size() == getDim() : getDim() + " != " + x.size() + "	" + tag;
		super.train1(x, tag, weight);
	}

}
