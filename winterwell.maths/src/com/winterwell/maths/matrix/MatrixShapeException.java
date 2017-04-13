package com.winterwell.maths.matrix;

import no.uib.cipr.matrix.Matrix;

public class MatrixShapeException extends IllegalArgumentException {
	public MatrixShapeException(Matrix A, Matrix B) {
		super(
				(A==null? "": "("+A.numRows()+"x"+A.numColumns()+")")
				+(B==null?"": " !~ ("+B.numRows()+"x"+B.numColumns()+")")
				);
	}

	private static final long serialVersionUID = 1L;

}
