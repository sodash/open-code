package com.winterwell.maths.timeseries;

import com.winterwell.maths.stats.algorithms.LinearRegression;
import com.winterwell.maths.vector.X;
import com.winterwell.utils.containers.AbstractIterator;

import no.uib.cipr.matrix.Vector;

/**
 * Linear detrend: remove linear time-trend. Does a least squares best-fit of
 * value = f(time), then subtracts this from the data.
 * 
 * Obviously, if there is a non-linear trend then this won't (fully) remove it.
 * 
 * @author daniel
 * @testedby  DetrendedStreamTest}
 */
public class DetrendedStream extends ADataStream {
	private static final long serialVersionUID = 1L;
	private ListDataStream list;

	double[] offsets;

	/**
	 * WARNING: shared across iterators!
	 */
	double[] tWeights;

	/**
	 * Will read all data into memory.
	 * 
	 * @param base
	 */
	public DetrendedStream(IDataStream base) {
		super(base.getDim());
		// read it all in
		list = new ListDataStream(base);
		// the linear trend in each dimension
		tWeights = new double[base.getDim()];
		offsets = new double[base.getDim()];
		for (int d = 0; d < list.getDim(); d++) {
			LinearRegression lr = new LinearRegression();
			for (Datum datum : list.getList()) {
				long x = datum.time.getTime();
				double y = datum.get(d);
				lr.train1(new X(x), y);
			}
			lr.finishTraining();
			// what was the trend?
			Vector ws = lr.getWeights();
			tWeights[d] = ws.get(0);
			offsets[d] = ws.get(1);
		}
	}

	/**
	 * Apply the detrending weights to another data stream. E.g. learn a trend,
	 * then apply the trend-removal to some fresh data -- but without learning
	 * fresh detrending.
	 * 
	 * @param stream
	 * @return
	 */
	public IDataStream apply(IDataStream stream) {
		return new FilteredDataStream(stream) {
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
			protected Datum filter(Datum datum) {
				// add in the trend
				long t = datum.time.getTime();
				Datum x = datum.copy();
				for (int d = 0; d < getDim(); d++) {
					double trend = offsets[d] + tWeights[d] * t;
					x.add(d, -trend);
				}
				return x;
			}
		};
	}

	/**
	 * Inverse of the detrend operation. Usage: e.g. detrend data, generate
	 * predictions, add the trend back in to get final predictions.
	 * 
	 * @param detrendedData
	 * @return data with trend added back in
	 */
	public IDataStream inverse(IDataStream detrendedData) {
		return new FilteredDataStream(detrendedData) {
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
			protected Datum filter(Datum datum) {
				// add in the trend
				long t = datum.time.getTime();
				Datum x = datum.copy();
				for (int d = 0; d < getDim(); d++) {
					double trend = offsets[d] + tWeights[d] * t;
					x.add(d, trend);
				}
				return x;
			}
		};
	}

	@Override
	public AbstractIterator<Datum> iterator() {
		return new AbstractIterator<Datum>() {
			AbstractIterator<Datum> it = list.iterator();

			@Override
			protected Datum next2() {
				if (!it.hasNext())
					return null;
				Datum nxt = it.next();
				// subtract the trend
				Datum x = nxt.copy();
				for (int d = 0; d < getDim(); d++) {
					double trend = offsets[d] + tWeights[d]
							* nxt.time.getTime();
					x.add(d, -trend);
				}
				return x;
			}
		};
	}

}
