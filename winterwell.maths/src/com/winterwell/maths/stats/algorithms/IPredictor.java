package com.winterwell.maths.stats.algorithms;

import no.uib.cipr.matrix.Vector;

/**
 * double from vector predictions
 * 
 * @author daniel
 * 
 */
public interface IPredictor {

	public double predict(Vector x);

}
