/**
 * 
 */
package com.winterwell.maths.stats.distributions.d1;

import org.junit.Test;

import com.winterwell.utils.containers.Range;

/**
 * @author daniel
 *
 */
public class SkippingTrainerTest {

	@Test
	public void testMeanVar1D() {
		MeanVar1D mv = new MeanVar1D();
		SkippingTrainer<Double> skippy = new SkippingTrainer<Double>(mv);
		
		UniformDistribution1D target1 = new UniformDistribution1D(new Range(0,10));
		UniformDistribution1D target2 = new UniformDistribution1D(new Range(10,20));
		
		skippy.resetup();
		for(int i=0; i<10000; i++) {
			if (skippy.skip()) continue;
			Double x = target1.sample();
			skippy.train1(x);
		}
		System.out.println(skippy.getCnt());
		
		for(int i=0; i<10000; i++) {
			if (skippy.skip()) continue;
			Double x = target2.sample();
			skippy.train1(x);
		}
		skippy.finishTraining();
		
		System.out.println(skippy.getCnt()+":\t"+mv+" "+mv.getMin()+", "+mv.getMax());
		assert com.winterwell.utils.MathUtils.approx(mv.getMin(), 0);
		assert com.winterwell.utils.MathUtils.approx(mv.getMax(), 20);
		assert com.winterwell.utils.MathUtils.approx(mv.getMean(), 10);
	}

}
