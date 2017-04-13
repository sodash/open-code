package com.winterwell.maths.stats.algorithms;

import com.winterwell.utils.IFn;

import no.uib.cipr.matrix.Vector;

public interface IVectorFn extends IFn<Vector, Vector> {

	/**
	 * Often we're just working in the one dimensional space.
	 * @return
	 */
	double apply1D(double x) throws UnsupportedOperationException;
}
