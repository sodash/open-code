package com.winterwell.maths.classifiers;

import com.winterwell.maths.stats.distributions.d1.Gaussian1D;
import com.winterwell.utils.MathUtils;
import com.winterwell.utils.containers.Range;

/**
 * {@link WilsonScoreIntervalTest}
 * @author daniel
 *
 */
public class WilsonScoreInterval {
	/**
	 * Computes a Wilson score interval
	 * See http://en.wikipedia.org/wiki/Binomial_proportion_confidence_interval#Wilson_score_interval
	 * 
	 * TODO with continuity correction, c.f. 
	 * Newcombe, Robert G. "Two-Sided Confidence Intervals for the Single Proportion: Comparison of Seven Methods," Statistics in Medicine, 17, 857-872 (1998).
	 * 
	 * @param error The "width" of the interval. For example, for a 95% confidence level the error is 5% = 0.05
	 * @param p % success
	 * @param n Number of trials
	 * @return a range within [0, 1]. E.g. error=0.5 -> 95% confidence band for n.p successes out of n trials  
	 */
	public static Range getInterval(double error, double p, double n) {
		if (n==0) {
			// no data => total uncertainty
			return new Range(0, 1);
		}
		assert MathUtils.isProb(p);
		assert error > 0 && error < 0.5 : error;
		double conf = 1 - error/2;
		double z = new Gaussian1D(0, 1).getConfidence(conf);
		double z2_n = z*z/n;
		assert MathUtils.isFinite(z2_n);
		double pm = z*Math.sqrt(p*(1-p)/n + z2_n/(4*n));
		double top = p + 0.5*z2_n;		
		double low = top - pm;
		double hi = top + pm;
		double bottom = (1 + z2_n);
		assert MathUtils.isFinite(bottom);
		return new Range(low/bottom, hi/bottom);
	}
	
}
