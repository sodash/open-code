package com.winterwell.maths.timeseries;

import com.winterwell.utils.containers.AbstractIterator;

/**
 * Make a data stream out of a set of arrays, one for each dimension.
 * 
 * Status: experimental. is this useful?
 * 
 * @author daniel
 * 
 */
public final class ArrayDataStream extends ADataStream {
	private static final long serialVersionUID = 1L;
	/**
	 * [dimension][timelike-index]
	 */
	private double[][] values;

	public ArrayDataStream(double[]... values) {
		super(values.length);
		assert values.length != 0;
		this.values = values;
	}

	@Override
	public AbstractIterator<Datum> iterator() {
		return new AbstractIterator<Datum>() {

			int i = 0;

			@Override
			protected Datum next2() {
				if (i >= size())
					return null;
				double[] v = new double[getDim()];
				for (int j = 0; j < getDim(); j++) {
					v[j] = values[j][i];
				}
				Datum n = new Datum(v);
				i++;
				return n;
			}

		};
	}

	// int i=0;
	//
	// @Override
	// protected Datum next2() {
	// if (i>=size()) return null;
	// double[] v = new double[getDim()];
	// for(int j=0; j<getDim(); j++) {
	// v[j] = values[j][i];
	// }
	// Datum n = new Datum(v);
	// i++;
	// return n;
	// }

	@Override
	public int size() {
		return values[0].length;
	}

}
