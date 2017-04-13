package com.winterwell.maths.timeseries;

import com.winterwell.utils.time.Time;

/**
 * Status: sketch!
 * 
 * {@link IDataStream} is kind of inefficient for low-dimensional data. Should
 * we have an alternative? Wondering about the IO read(byte[]) methods as a
 * pattern to copy.
 * 
 * @author daniel
 * 
 */
public interface IDataArray1D {

	/**
	 * Fill the arrays with data.
	 * 
	 * @param data
	 * @param times
	 *            can be null
	 * @param labels
	 *            can be null. Can contain nulls on return
	 * @return the number of data points to read out of the arrays, may be less
	 *         than the array length. 0 if at the end of the stream.
	 */
	int next1D(double[] data, Time[] times, Object[] labels);

	int next2D(double[] xdata, double[] ydata, Time[] times, Object[] labels);

}
