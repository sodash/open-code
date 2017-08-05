package com.winterwell.maths.stats.distributions.discrete;

import com.winterwell.maths.classifiers.WilsonScoreInterval;

/**
 * experimental
 * combine 2 distributions, using 2nd order probability reasoning
 * 
 * Suppose we have observations for 
 * 
 * x,y -- few observations
 * x -- lots of observations
 * y -- lots of observations
 * 
 * How should we combine them?
 * 
 * 
 * @author daniel
 *
 */
public class AndCombinator<X> {

	void combine(ObjectDistribution<X> a, ObjectDistribution<X> b) {
		
//		a and b are both samples, giving pa, pb which are estimates of true-p
//		the estimates have a varianPce, related to sample size 
		
		// Wikipedia: The central limit theorem applies poorly to this distribution with a sample size less than 30 or where the proportion is close to 0 or 1.
		
		double aw = a.getTotalWeight();
		double bw = b.getTotalWeight();
		
		for(X k : a) {
			double na = a.prob(k);
			double nb = b.prob(k);
			
			double pa = na / aw;
			double pb = nb / bw;
			
			
//			WilsonScoreInterval.getInterval(error, p, n)
		}
		
	}
	
}
