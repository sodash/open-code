package com.winterwell.maths.timeseries;

import no.uib.cipr.matrix.Matrix;

/**
 * Apply a matrix to all vectors.
 * 
 * @author daniel
 * @testedby {@link MatrixStreamTest}
 */
public class MatrixStream extends FilteredDataStream {

	private static final long serialVersionUID = 1L;

	private final Matrix matrix;


	public MatrixStream(Matrix projection, IDataStream base) {
		super(base, projection.numRows());
		matrix = projection;
	}

	@Override
	protected Datum filter(Datum datum) throws EnoughAlreadyException {
		// apply the matrix
		Datum d = new Datum(datum.getTime(), new double[matrix.numRows()],
				datum.getLabel());
		d.setModifiable(true);
		matrix.mult(datum, d);
		return d;
	}

}
