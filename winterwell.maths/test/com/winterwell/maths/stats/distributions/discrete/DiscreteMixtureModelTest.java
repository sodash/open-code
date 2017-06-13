package com.winterwell.maths.stats.distributions.discrete;

import static org.junit.Assert.*;

import org.junit.Test;

import com.winterwell.utils.MathUtils;

public class DiscreteMixtureModelTest {

	@Test
	public void testProb() {
		ObjectDistribution od1 = new ObjectDistribution<>();
		od1.train1("apple");
		od1.train1("apple");
		od1.train1("banana");
		od1.finishTraining();
		od1.normalise();
		ObjectDistribution od2 = new ObjectDistribution<>();
		od2.train1("apple");
		od2.train1("carrot");
		od2.finishTraining();
		od2.normalise();
		DiscreteMixtureModel mm = new DiscreteMixtureModel<>(od1, od2);
		
		double pa = mm.prob("apple");
		double pb = mm.prob("banana");
		double pc = mm.prob("carrot");
		assert MathUtils.equalish(pa, 7 / 12.0) : pa+" != "+(7/12.0); // 1/2 * ( 2/3 + 1/2)
		assert pb == 1 / 6.0 : pb;
		assert pc == 1 / 4.0 : pc;
	}

	@Test
	public void testSample() {
		ObjectDistribution<String> od1 = new ObjectDistribution<>();
		od1.train1("apple");
		od1.train1("apple");
		od1.train1("banana");
		od1.finishTraining();
		od1.normalise();
		ObjectDistribution<String> od2 = new ObjectDistribution<>();
		od2.train1("apple");
		od2.train1("carrot");
		od2.finishTraining();
		od2.normalise();
		DiscreteMixtureModel<String> mm = new DiscreteMixtureModel<>(od1, od2);

		ObjectDistribution<String> od = new ObjectDistribution<>();
		for(int i=0; i<1000000; i++) { // need a lot of samples to reliably get 1% tolerance below
			String x = mm.sample();
			od.count(x);
		}
		od.normalise();
		double pa = od.prob("apple");
		double pb = od.prob("banana");
		double pc = od.prob("carrot");
		assert MathUtils.equalish(pa, 7 / 12.0) : pa+" != "+(7/12.0); // 1/2 * ( 2/3 + 1/2)
		assert MathUtils.equalish(pb, 1 / 6.0) : pb;
		assert MathUtils.equalish(pc, 1 / 4.0) : pc;
	}

}
