package com.winterwell.maths.stats.distributions;

import java.util.List;

import com.winterwell.maths.GridInfo;
import com.winterwell.maths.stats.StatsUtils;
import com.winterwell.maths.stats.distributions.d1.Gaussian1D;
import com.winterwell.maths.stats.distributions.d1.MeanVar1D;
import com.winterwell.maths.timeseries.DataUtils;
import com.winterwell.maths.vector.X;
import com.winterwell.maths.vector.XY;
import com.winterwell.utils.MathUtils;

import junit.framework.TestCase;
import no.uib.cipr.matrix.DenseVector;
import no.uib.cipr.matrix.Vector;
import no.uib.cipr.matrix.VectorEntry;

public class GaussianBallTest extends TestCase {

	public void testGetMean() {
		GaussianBall g = new GaussianBall(new XY(1, 4), 1);
		assert DataUtils.equals(g.getMean(), new XY(1, 4));
	}

	public void testGetVariance() {
		GaussianBall g = new GaussianBall(new XY(1, 4), 2);
		Vector v = g.getVariance();
		for (VectorEntry ve : v) {
			assert ve.get() == 2;
		}
	}

	public void testProb1D() {
		GaussianBall g = new GaussianBall(new DenseVector(new double[] { 4 }),
				3);
		Gaussian1D g1d = new Gaussian1D(4, 3);
		for (double x : new double[] { -2, -1, 0, 1, 2, 3, 4, 5, 6 }) {
			double p1 = g.density(new DenseVector(new double[] { x }));
			double p2 = g1d.density(x);
			assert Math.abs(p2 - p1) < 0.05;
		}
	}

	public void testProb2D() {
		GaussianBall g = new GaussianBall(new DenseVector(
				new double[] { 1, -1 }), 2);
		double[] xs = new GridInfo(-10, 10, 2000).getMidPoints();
		double[] ys = new GridInfo(-10, 10, 2000).getMidPoints();
		double total = 0, vol = 0;
		double b = MathUtils.sq(20.0 / 2000); // area of a grid square
		for (int i = 0; i < xs.length; i++) {
			for (int j = 0; j < ys.length; j++) {
				XY xy = new XY(xs[i], xs[j]);
				double p = g.density(xy);
				total += p;
				vol += p * b;
			}
		}
		assert Math.abs(1 - vol) < 0.1;
	}

	public void testSample() {
		{ // 1D #1
			GaussianBall g = new GaussianBall(new X(4), 3);
			MeanVar1D sm = new MeanVar1D();
			for (int i = 0; i < 10000; i++) {
				sm.train1(g.sample().get(0));
			}

			assert MathUtils.approx(sm.getMean(), 4) : sm;
		}
		{ // 1D #2
			GaussianBall g = new GaussianBall(new X(4), 3);
			List<Vector> sample = StatsUtils.sample(g, 10000);
			double[] xs = DataUtils.get1DArr(sample, 0);
			double mean = StatsUtils.mean(xs);
			double var = StatsUtils.var(xs);
			assert Math.abs(mean - 4) < 0.25 : var;
			assert Math.abs(var - 3) < 0.25 : var;
		}
	}

}
