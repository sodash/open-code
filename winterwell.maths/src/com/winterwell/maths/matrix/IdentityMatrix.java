package com.winterwell.maths.matrix;

import java.util.Iterator;

import no.uib.cipr.matrix.AbstractMatrix;
import no.uib.cipr.matrix.Matrix;
import no.uib.cipr.matrix.MatrixEntry;
import no.uib.cipr.matrix.Vector;

/**
 * Actually this is a scaled identity matrix. TODO override more methods
 * 
 * @author Daniel
 * @testedby {@link IdentityMatrixTest}
 */
public final class IdentityMatrix extends AbstractMatrix {

	private final double lambda;

	public IdentityMatrix(int dim) {
		this(dim, 1);
	}
	
	@Override
	public IdentityMatrix copy() {
		return new IdentityMatrix(numColumns, lambda);
	}
	
	@Override
	public Matrix mult(double alpha, Matrix B, Matrix C) {
		return C.set(lambda*alpha, B);
	}
	
	@Override
	public Vector mult(double alpha, Vector x, Vector y) {
		return y.set(lambda*alpha, x);
	}
		
	
//	@Override I think this is right
//	public Matrix solve(Matrix B, Matrix X) {
//		return X.set(B);
//	}

	@Override
	public Iterator<MatrixEntry> iterator() {
		final DMatrixEntry me = new DMatrixEntry(this);
		return new Iterator<MatrixEntry>() {
			int i = 0;
			@Override
			public boolean hasNext() {
				return i<numRows;
			}

			@Override
			public MatrixEntry next() {
				me.update(i, i);
				i++;
				return me;
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}


	/**
	 * Create a scaled identity matrix.
	 * 
	 * @param dim
	 * @param lambda
	 */
	public IdentityMatrix(int dim, double lambda) {
		super(dim, dim);
		this.lambda = lambda;
	}

	@Override
	public double get(int row, int column) {
		return row == column ? lambda : 0;
	}
		

}
