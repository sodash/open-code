package com.winterwell.maths.vector;

import org.junit.Test;

import com.winterwell.maths.timeseries.DataUtils;
import com.winterwell.utils.MathUtils;

import no.uib.cipr.matrix.Vector;

public class CyclicMetricTest {

	@Test
	public void testAssumptions() {
		double mod = -1.5 % 2;
		assert mod == -1.5 : mod;

		mod = -2.5 % 2;
		assert mod == -0.5 : mod;
	}

	@Test
	public void testDist() {
		CyclicMetric clock = new CyclicMetric(24);

		assert clock.dist(0, 1) == 1;
		assert clock.dist(1, 4) == 3;
		assert clock.dist(1, 0) == 1;
		assert clock.dist(4, 1) == 3;

		assert clock.dist(-1, 1) == 2;
		assert clock.dist(1, -2) == 3;
		assert clock.dist(0, -4) == 4;

		assert clock.dist(23, 1) == 2;
		assert clock.dist(1, 23) == 2;
		assert clock.dist(23, -1) == 0;
	}

	@Test
	public void testEmbed() throws Exception {
		CyclicMetric clock = new CyclicMetric(24);

		Vector v1 = clock.embed(0);
		Vector v2 = clock.embed(1);
		Vector v3 = clock.embed(23);
		Vector v3b = clock.embed(-1);
		Vector v2b = clock.embed(25);
		Vector v2c = clock.embed(-23);
		Vector v4 = clock.embed(12);
		Vector v5 = clock.embed(6);
		Vector v6 = clock.embed(18);

		assert DataUtils.equals(v1, 0, 1);
		assert DataUtils.equalish(v4, new XY(0, -1)) : v4;
		assert DataUtils.equalish(v5, new XY(1, 0));
		assert DataUtils.equalish(v6, new XY(-1, 0));

		assert DataUtils.equals(v2, v2b);
		assert DataUtils.equals(v2, v2c);
		assert DataUtils.equals(v3, v3b);
	}

	@Test
	public void testProject() throws Exception {
		CyclicMetric clock = new CyclicMetric(24);
		double[] testme = new double[] { 0, 1, -1, 6, 12, 18, 24, 100 };
		double[] answer = new double[] { 0, 1, 23, 6, 12, 18, 0, 4 };
		for (int i = 0; i < testme.length; i++) {
			double x = testme[i];
			Vector v = clock.embed(x);
			double x2 = clock.project(v);
			assert MathUtils.equalish(answer[i], x2) : answer[i] + " vs " + x2
					+ " for " + x;
		}
	}

}
