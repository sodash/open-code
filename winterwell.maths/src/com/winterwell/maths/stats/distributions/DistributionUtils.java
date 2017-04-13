/**
 *
 */
package com.winterwell.maths.stats.distributions;

import com.winterwell.maths.stats.distributions.discrete.IFiniteDistribution;

import no.uib.cipr.matrix.Vector;

/**
 * @author Joe Halliwell <joe@winterwell.com>
 * 
 */
public class DistributionUtils {

	/**
	 * Rattle through data and compute its likelihood.
	 * 
	 * @param <X>
	 * @param distribution
	 * @param data
	 * @return
	 */
	public static double getLikelihood(IDistribution distribution,
			Iterable<? extends Vector> data) {
		double likelihood = 1;
		for (Vector datum : data) {
			likelihood *= distribution.density(datum);
		}
		return likelihood;
	}

	/**
	 * Variant for finite distributions
	 * 
	 * @param <X>
	 * @param distribution
	 * @param data
	 * @return
	 */
	public static <X> double getLikelihood(IFiniteDistribution<X> distribution,
			Iterable<? extends X> data) {
		double likelihood = 1;
		for (X datum : data) {
			likelihood *= distribution.prob(datum);
		}
		return likelihood;
	}

	/**
	 * Rattle through data and compute its scaled log likelihood. i.e (1/n) *
	 * sum ln f (d_i)
	 * 
	 * @param <X>
	 * @param distribution
	 * @param data
	 * @return
	 */
	public static double getLogLikelihood(IDistribution distribution,
			Iterable<? extends Vector> data) {
		double l = 1;
		int count = 0;
		for (Vector datum : data) {
			l += Math.log(distribution.density(datum));
			count++;
		}
		if (count == 0)
			return 0;
		return l / count;
	}

}
