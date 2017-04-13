package com.winterwell.maths.stats.algorithms;

import no.uib.cipr.matrix.Vector;

public class ConstantPredictor implements IPredictor {

	private double v;

	public ConstantPredictor(double v) {
		this.v = v;
	}

	@Override
	public double predict(Vector x) {
		return v;
	}

	public double getValue() {
		return v;
	}
	
	@Override
	public String toString() {	
		return "ConstantPredictor["+v+"]";
	}

}
