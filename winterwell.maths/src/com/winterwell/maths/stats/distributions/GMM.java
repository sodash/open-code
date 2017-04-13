package com.winterwell.maths.stats.distributions;

import no.uib.cipr.matrix.Vector;

/**
 * TODO Gaussian specific mixture model, so we can calculate variance.
 * 
 * @author daniel
 * 
 */
public class GMM<D extends IDistribution> extends MixtureModel<D> {

	public GMM(int dim) {
		super(dim);
	}

	@Override
	public Vector getVariance() {
		// TODO Auto-generated method stub
		return super.getVariance();
	}

}
