package com.winterwell.maths.matrix;

import java.util.Iterator;

import com.winterwell.utils.MathUtils;
import com.winterwell.utils.Printer;
import com.winterwell.utils.StrUtils;

import no.uib.cipr.matrix.AbstractMatrix;
import no.uib.cipr.matrix.Matrix;
import no.uib.cipr.matrix.MatrixEntry;
import no.uib.cipr.matrix.Vector;
import no.uib.cipr.matrix.VectorEntry;

/**
 * A matrix where all off-diagonal values are zero. TODO override more methods
 * 
 * @author daniel
 */
public final class DiagonalMatrix extends AbstractMatrix {

	private final Vector diagonal;

	/**
	 * 
	 * @param diagonal
	 *            This will be used directly.
	 */
	public DiagonalMatrix(Vector diagonal) {
		super(diagonal.size(), diagonal.size());
		this.diagonal = diagonal;
	}

	@Override
	public Matrix copy() {
		return new DiagonalMatrix(diagonal.copy());
	}

	@Override
	public double get(int row, int column) {
		if (row != column)
			return 0;
		return diagonal.get(row);
	}

	@Override
	public Iterator<MatrixEntry> iterator() {
		final DMatrixEntry me = new DMatrixEntry(this);
		final Iterator<VectorEntry> it = diagonal.iterator();
		return new Iterator<MatrixEntry>() {
			@Override
			public boolean hasNext() {
				return it.hasNext();
			}

			@Override
			public MatrixEntry next() {
				VectorEntry ve = it.next();
				me.update(ve.index(), ve.index());
				return me;
			}

			@Override
			public void remove() {
				it.remove();
			}
		};
	}

	@Override
	public Vector multAdd(double alpha, Vector x, Vector y) {
		if (alpha == 0)
			return y;
		for (VectorEntry ve : x) {
			int i = ve.index();
			double vi = diagonal.get(i) * ve.get() * alpha;
			y.add(i, vi);
		}
		return y;
	}

	@Override
	public void set(int row, int column, double value) {
		if (row != column) {
			if (MathUtils.equalish(value, 0))
				return;
			throw new IllegalArgumentException("Off diagonal value");
		}
		diagonal.set(row, value);
	}

	@Override
	public String toString() {
		if (numRows > 20)
			return getClass().getSimpleName();
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

	@Override
	public Matrix transpose() {
		return this;
	}
}
