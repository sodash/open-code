package com.winterwell.optimization;

import com.winterwell.depot.Desc;

public abstract class AEvaluate<T> implements IEvaluate<T, Double> {

	@Override
	public Desc<Double> getResultDesc(T candidate) {
		return null;
	}

	@Override
	public double result2score(Double result) {
		return result;
	}

}
