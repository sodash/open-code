package com.winterwell.maths.timeseries;

import com.winterwell.maths.stats.algorithms.IVectorFn;

import no.uib.cipr.matrix.Vector;

public interface IVectorTransform extends IVectorFn, IInvertibleFn<Vector, Vector> {

	Vector apply(Vector input);
		
}
