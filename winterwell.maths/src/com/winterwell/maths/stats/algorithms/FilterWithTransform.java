package com.winterwell.maths.stats.algorithms;

import java.util.List;

import com.winterwell.maths.stats.distributions.IDistribution;
import com.winterwell.maths.timeseries.IDataStream;
import com.winterwell.maths.timeseries.IVectorTransform;
import com.winterwell.utils.time.Dt;

import no.uib.cipr.matrix.Vector;

public class FilterWithTransform implements ITimeSeriesFilter {

	private IVectorTransform transform;
	private ITimeSeriesFilter base;

	public FilterWithTransform(IVectorTransform transform, ITimeSeriesFilter kalmanFilter) {
		this.transform = transform;
		this.base = kalmanFilter;
	}

	@Override
	public int getMeasurementDimension() {
		// WHat if transform changes this??
		return base.getMeasurementDimension();
	}

	@Override
	public int getStateDimension() {
		return base.getStateDimension();
	}

	@Override
	public IDistribution filter(IDistribution state, Vector observation) {
		Vector obs = transform.apply(observation);
		return null;
	}

	@Override
	public List<IDistribution> smooth(IDistribution initialState, IDataStream observations) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Dt getTimeStep() {
		// TODO Auto-generated method stub
		return null;
	}

}
