package com.winterwell.maths.stats.distributions.d1;

import org.junit.Test;

import com.winterwell.maths.stats.StatsUtils;
import com.winterwell.utils.MathUtils;

public class Gaussian1DTest {

	@Test
	public void testDensity() {
		{
			Gaussian1D g = new Gaussian1D(10, 1);
			assert g.density(10) > g.density(9);
			assert g.density(10) > g.density(11);
			assert g.density(15) < 0.001;
		}
		{ // Bug from ITI work
			Gaussian1D g = new Gaussian1D(28, 315);
			double p = g.density(4);
			assert p != 0 : "" + p;
		}
	}

	
	@Test
	public void testErf() {
		double erf = Gaussian1D.erf(1000);
		System.out.println(erf);
		double erf2 = Gaussian1D.erf(-1000);
		System.out.println(erf2);
	}
	
	

	@Test
	public void testKeyValues() {
		{	// 90% confidence interval (so 5% outside on either side)
			Gaussian1D g = new Gaussian1D(0, 1);
			double x = g.getConfidence(0.95);
			double p1 = g.prob(-10000000, x); // 1.64
			System.out.println(p1+"\t"+x);
			assert MathUtils.equalish(p1, 0.95) : p1;
		}
		{	// 95% confidence interval (so 2.5% outside on either side)
			Gaussian1D g = new Gaussian1D(0, 1);
			double p1 = g.prob(-10000000, 1.96);
			System.out.println(p1);
			assert MathUtils.equalish(p1, 0.975);
		}
		{	// 99% confidence interval (so 0.5% outside on either side)
			Gaussian1D g = new Gaussian1D(0, 1);
			double p1 = g.prob(-10000000, 2.5758);
			System.out.println(p1);
			assert MathUtils.equalish(p1, 0.995);
		}
	}

	@Test
	public void testProb() {
		{ // varaince 1
			Gaussian1D g = new Gaussian1D(1, 1);
			double p1 = g.prob(0.2, 0.5);
			double p2 = g.prob(1.5, 1.8);
			assert MathUtils.equalish(p1, p2);

			double p3 = g.prob(2, 10);
			assert MathUtils.equalish(p3, 0.1587);
			// 68% within 1 sd, 95% within 2 sd
			// => 84% within -infinity upto +1 sd, and 97.5% within < +2sd
			double sd1 = g.prob(-1000, 2);
			double sd2 = g.prob(-1000, 3);
			assert MathUtils.equalish(sd1, 0.84) : sd1;
			assert MathUtils.equalish(sd2, 0.975) : sd2;
		}
		{ // variance 4
			Gaussian1D g = new Gaussian1D(0, 4);
			double p1 = g.prob(0, 2);
			double p2 = g.prob(-2, 0);
			assert MathUtils.equalish(p1, p2);

			double sd1 = g.prob(-1000, 2);
			double sd2 = g.prob(-1000, 4);
			assert MathUtils.equalish(sd1, 0.84) : sd1;
			assert MathUtils.equalish(sd2, 0.975) : sd2;
		}
	}

	@Test
	public void testProb2() {
		Gaussian1D g = new Gaussian1D(1, 0);
		assert MathUtils.equalish(g.prob(0.5, 1.5), 1.0);
		assert MathUtils.equalish(g.prob(1.0, 1.0), 1.0);
		assert MathUtils.equalish(g.prob(0.5, 0.7), 0.0);
	}

	@Test
	public void testSample() {
		{
			Gaussian1D g = new Gaussian1D(0, 10);
			double[] xs = new double[10000];
			for (int i = 0; i < 10000; i++) {
				double x = g.sample();
				xs[i] = x;
			}
			double var = StatsUtils.var(xs);
			double mean = StatsUtils.mean(xs);
			assert Math.abs(var - 10) < 0.5 : var;
			assert Math.abs(mean) < 0.2 : mean;
		}
		{
			Gaussian1D g = new Gaussian1D(-2, 0.1);
			double[] xs = new double[10000];
			for (int i = 0; i < 10000; i++) {
				double x = g.sample();
				xs[i] = x;
			}
			double var = StatsUtils.var(xs);
			double mean = StatsUtils.mean(xs);
			assert Math.abs(var - 0.1) < 0.1 : var;
			assert Math.abs(mean + 2) < 0.2 : mean;
		}
	}

}
