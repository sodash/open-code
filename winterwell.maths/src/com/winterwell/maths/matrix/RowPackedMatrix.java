/**
 *
 */
package com.winterwell.maths.matrix;

import java.util.ArrayList;
import java.util.List;

import com.winterwell.utils.Printer;
import com.winterwell.utils.StrUtils;

import no.uib.cipr.matrix.AbstractMatrix;
import no.uib.cipr.matrix.Matrix;
import no.uib.cipr.matrix.Vector;

/**
 * A matrix made of row vectors (which can be sparse or dense). TODO over-ride
 * default kernel methods
 * 
 * @author daniel
 * 
 */
public final class RowPackedMatrix extends AbstractMatrix implements Matrix {

	private final Vector[] rows;

	public RowPackedMatrix(List<? extends Vector> rows) {
		super(rows.size(), rows.get(0).size());
		this.rows = rows.toArray(new Vector[0]);
	}

	@Override
	public Matrix copy() {
		ArrayList<Vector> copies = new ArrayList<Vector>();
		for (Vector row : rows) {
			copies.add(row.copy());
		}
		return new RowPackedMatrix(copies);
	}

	@Override
	public double get(int row, int column) {
		return rows[row].get(column);
	}

	@Override
	public Vector multAdd(double alpha, Vector x, Vector y) {
		for (int r = 0; r < rows.length; r++) {
			double v = alpha * rows[r].dot(x);
			y.add(r, v);
		}
		return y;
	}

	@Override
	public void set(int row, int column, double value) {
		rows[row].set(column, value);
	}

	@Override
	public String toString() {
		if (numRows > 10 || numColumns > 10)
			return getClass().getSimpleName() + "[" + numRows + "x"
					+ numColumns + "]";
		StringBuilder sb = new StringBuilder();
		sb.append('\n');
		for (int i = 0; i < numRows; i++) {
			sb.append("(");
			for (int j = 0; j < numColumns; j++) {
				sb.append(Printer.toStringNumber(get(i, j)));
				sb.append("\t");
			}
			StrUtils.pop(sb, 1);
			sb.append(")\n");
		}
		return sb.toString();
	}

}
