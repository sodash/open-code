package com.winterwell.maths.stats.distributions.discrete;

import org.junit.Test;

import com.winterwell.maths.stats.distributions.d1.IDistribution1D;
import com.winterwell.utils.MathUtils;

public class RandomChoiceTest {

	@Test
	public void testProb() {
		RandomChoice rc = new RandomChoice(0.1);
		assert rc.prob(true) == 0.1;
		assert rc.prob(false) == 0.9;
	}

	@Test
	public void testSample() {
		RandomChoice rc = new RandomChoice(0.1);
		int cnt = 0;
		for(int i=0; i<100000; i++) {
			Boolean yes = rc.sample();
			if (yes) cnt++;
		}
		assert MathUtils.approx(cnt, 10000) : cnt;
	}
	
	@Test
	public void test1D() {
		RandomChoice rc = new RandomChoice(0.1);
		IDistribution1D d1d = rc.as1D();
		int cnt = 0;
		for(int i=0; i<100000; i++) {
			Double x = d1d.sample();
			assert x==0 | x==1 : x;
			if (x==1) cnt++;
		}
		assert MathUtils.approx(cnt, 10000) : cnt;
	}

}
