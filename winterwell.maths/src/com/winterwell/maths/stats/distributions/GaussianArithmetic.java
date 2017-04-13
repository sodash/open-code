/**
 * 
 */
package com.winterwell.maths.stats.distributions;

import com.winterwell.maths.stats.distributions.d1.Gaussian1D;

/**
 * Status: Experimental
 * 
 * Efficient BGM usage will want to do this kind of things... how to handle this
 * in an OO stylee?
 * 
 * If X and Y are gaussians then aX + bY is gaussian. X.Y isn't, but it'd be
 * nice to handle it somehow
 * 
 * @author daniel
 * 
 */
public class GaussianArithmetic {

	public Gaussian1D plus(double a, Gaussian1D x, double b, Gaussian1D y) {
		double v = a * a * x.getVariance() + b * b * y.getVariance();
		return new Gaussian1D(a * x.getMean() + b * y.getMean(), v);
	}

}
