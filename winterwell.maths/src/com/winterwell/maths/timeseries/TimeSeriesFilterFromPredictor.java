package com.winterwell.maths.timeseries;

import java.util.ArrayList;
import java.util.List;

import com.winterwell.maths.stats.algorithms.IPredictor;
import com.winterwell.maths.stats.algorithms.ITimeSeriesFilter;
import com.winterwell.maths.stats.distributions.IDistribution;
import com.winterwell.maths.stats.distributions.d1.DistributionFrom1D;
import com.winterwell.maths.stats.distributions.d1.Gaussian1D;
import com.winterwell.utils.time.Dt;
import com.winterwell.utils.time.TUnit;

import no.uib.cipr.matrix.Vector;

public class TimeSeriesFilterFromPredictor implements ITimeSeriesFilter {

	IPredictor base;
	private int stateDim;
	private double noiseVar;

	@Override
	public int getMeasurementDimension() {
		return 1;
	}

	@Override
	public int getStateDimension() {
		return stateDim;
	}

	@Override
	public IDistribution filter(IDistribution stateIsIgnoredHere, Vector observation) {
		double pred = base.predict(observation);
		return new DistributionFrom1D(new Gaussian1D(pred, noiseVar));
	}

	@Override
	public List<IDistribution> smooth(IDistribution initialState, IDataStream observations) {
		List<IDistribution> predicted = new ArrayList();
		for (Datum obs : observations) {
			IDistribution pred = filter(null, obs);
			predicted.add(pred);
		}
		return predicted;
	}

	@Override
	public Dt getTimeStep() {
		return TUnit.SECOND.dt;
	}
		

}
