package com.winterwell.maths.stats.distributions;

import org.junit.Test;

import com.winterwell.maths.timeseries.DataUtils;

import no.uib.cipr.matrix.Vector;

public class MeanVarTest {

	// Since we're doing running-sum stats, we have to be fairly generous
	// about floating-point equality.
	static double GENEROUS_EPS = 1e-15;

	@Test
	public void testTrain1() {
		MeanVar stats = new MeanVar(4);
		for (int i = 0; i < 100; i++) {
			Double scalar_value;
			if (i % 2 == 0) {
				scalar_value = 0.25;
			} else {
				scalar_value = 0.75;
			}
			double[] _v = { scalar_value, scalar_value, 0.0, 1.0 };
			Vector v = DataUtils.newVector(_v);
			stats.train1(v);
		}
		double[] _expected_mean = { 0.5, 0.5, 0.0, 1.0 };
		double[] _expected_variance = { 0.25 * 0.25, 0.25 * 0.25, 0.0, 0.0 };
		Vector expected_mean = DataUtils.newVector(_expected_mean);
		Vector expected_variance = DataUtils.newVector(_expected_variance);
		assert stats.getCount() == 100;
		Vector mean = stats.getMean();
		Vector var = stats.getVariance();
		assert DataUtils.equalish(mean, expected_mean, GENEROUS_EPS) : stats;
		assert DataUtils.equalish(var, expected_variance, GENEROUS_EPS) : stats;
	}
}
