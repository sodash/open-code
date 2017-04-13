package com.winterwell.maths.stats.algorithms;

import java.util.List;

import com.winterwell.maths.stats.distributions.IDistribution;
import com.winterwell.maths.timeseries.IDataStream;
import com.winterwell.utils.time.Dt;

import no.uib.cipr.matrix.Vector;

/**
 * A filter takes a sequence of noisy observations, and infers a cleaner underlying sequence.
 * This interface is for a discrete time-step filter.  
 * @author daniel
 *
 */
public interface ITimeSeriesFilter {

	int getMeasurementDimension();

	int getStateDimension();

	/**
	 * 
	 * @param state
	 * @param observation
	 * @return
	 */
	IDistribution filter(IDistribution state, Vector observation);
	
	/**
	 * 
	 * @param initialState Can be null
	 * @param observations
	 * @return
	 */
	List<IDistribution> smooth(IDistribution initialState, IDataStream observations);

	/**
	 * @return Can be null
	 */
	Dt getTimeStep();
}
