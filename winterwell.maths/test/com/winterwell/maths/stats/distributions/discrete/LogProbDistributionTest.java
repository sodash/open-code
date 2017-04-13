package com.winterwell.maths.stats.distributions.discrete;

import org.junit.Test;

import com.winterwell.utils.MathUtils;

public class LogProbDistributionTest {

	@Test
	public void testNormalise() {
		LogProbDistribution<String> distro = new LogProbDistribution<>();
		// 1000 x a 1%, 0.1% event
		double low=0,vlow=0,p=1;
		for(int i=0; i<1000; i++) {
			low += Math.log(0.01);
			vlow += Math.log(0.001);
			p *= 0.01;
		}		
		distro.setLogProb("a", low);
		distro.setLogProb("b", vlow);
		double pa0 = distro.prob("a");
//		double npa0 = distro.normProb("a");
		distro.normalise();
		double pa = distro.prob("a");
		double pb = distro.prob("b");
//		double npa = distro.normProb("a");
		assert MathUtils.equalish(pa+pb, 1) : pa+pb;
	}
	
	@Test
	public void testNormalise2() {
		LogProbDistribution<String> distro = new LogProbDistribution<>();
		// A very unlikely event -- 1000 x a 1% event
		double low=0,p=1;
		for(int i=0; i<1000; i++) {
			low += Math.log(0.01);
			p *= 0.01;
		}		
		// P(b) = half of P(a)
		double vlow = low + Math.log(0.5);
		distro.setLogProb("a", low);
		distro.setLogProb("b", vlow);
		double pa0 = distro.prob("a");
		distro.normalise();
		double pa = distro.prob("a");
		double pb = distro.prob("b");
		assert MathUtils.equalish(pa+pb, 1) : pa+pb;
		System.out.println(distro);
		assert MathUtils.equalish(pb, 1.0/3) : pb; 
	}
	
	@Test
	public void testNormalise3() {
		LogProbDistribution<String> distro = new LogProbDistribution<>();
		// A likely event
		distro.setProb("a", 0.9);
		distro.setProb("b", 0.8);
		distro.normalise();
		double pa = distro.prob("a");
		double pb = distro.prob("b");
		assert MathUtils.equalish(pa+pb, 1) : pa+pb;
		System.out.println(distro);
		assert MathUtils.equalish(pb, 0.8/1.7) : pb; 
	}

}
