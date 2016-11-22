package com.winterwell.datalog;

import com.winterwell.maths.stats.distributions.d1.IDistribution1D;
import com.winterwell.utils.time.Dt;

/**
 * 
 * @author daniel
 *         <p>
 *         <b>Copyright & license</b>: (c) Winterwell Associates Ltd, all rights
 *         reserved. This class is NOT formally a part of the com.winterwell.utils
 *         library. In particular, licenses for the com.winterwell.utils library do
 *         not apply to this file.
 */
public final class MeanRate {

	public MeanRate(IDistribution1D x, Dt dt) {
		this.x = x;
		this.dt = dt;
	}

	public final Dt dt;
	public final IDistribution1D x;

	@Override
	public String toString() {
		String dts = dt.getValue() == 1 ? dt.getUnit().toString().toLowerCase()
				: dt.toString();
		return x.getMean() + "/" + dts;
	}

}
