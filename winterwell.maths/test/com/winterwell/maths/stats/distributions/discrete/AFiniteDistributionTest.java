package com.winterwell.maths.stats.distributions.discrete;

import org.junit.Test;

import com.winterwell.maths.vector.XY;

/**
 * @tested {@link AFiniteDistribution}
 * @author daniel
 * 
 */
public class AFiniteDistributionTest {

	@Test
	public void testSample() {
		VectorDistribution vd = new VectorDistribution(new XY(0.25, 0.75));
		int[] cnts = new int[2];
		for (int i = 0; i < 1000; i++) {
			Integer s = vd.sample();
			cnts[s]++;
		}
		assert cnts[0] > 200 && cnts[0] < 300;
		assert cnts[1] > 700 && cnts[1] < 800;
	}

}
