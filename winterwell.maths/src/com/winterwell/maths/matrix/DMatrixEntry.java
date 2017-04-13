package com.winterwell.maths.matrix;

import no.uib.cipr.matrix.Matrix;
import no.uib.cipr.matrix.MatrixEntry;

/**
 * Why is MatrixEntry an interface and not a class?
 * <p>
 * Warning: MatrixEntrys get recycled - typically only one is made per iterator.
 * 
 * @author daniel
 * 
 */
final class DMatrixEntry implements MatrixEntry {

	private final Matrix matrix;

	private int row, column;

	public DMatrixEntry(Matrix matrix) {
		this.matrix = matrix;
	}

	@Override
	public int column() {
		return column;
	}

	@Override
	public double get() {
		return matrix.get(row, column);
	}

	@Override
	public int row() {
		return row;
	}

	@Override
	public void set(double value) {
		matrix.set(row, column, value);
	}

	@Override
	public String toString() {
		return "[" + row + ", " + column + "] = " + get();
	}

	/**
	 * Updates the entry
	 */
	void update(int row, int column) {
		this.row = row;
		this.column = column;
	}
}
