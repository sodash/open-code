package com.winterwell.maths.matrix;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import com.winterwell.utils.MathUtils;
import com.winterwell.utils.Printer;

import no.uib.cipr.matrix.AbstractMatrix;
import no.uib.cipr.matrix.Matrix;
import no.uib.cipr.matrix.MatrixEntry;
import no.uib.cipr.matrix.Vector;
import no.uib.cipr.matrix.VectorEntry;

/**
 * A crude projection matrix, which keeps some dimensions & discards others.
 * 
 * @author daniel
 * @testedby {@link ProjectionMatrixTest}
 */
public class ProjectionMatrix extends AbstractMatrix {

	private int[] keepThese;

	public ProjectionMatrix(int numColumns, int... keepTheseDims) {
		super(keepTheseDims.length, numColumns);
		keepThese = keepTheseDims;
		Arrays.sort(keepThese);
		assert keepTheseDims[0] >= 0 : Printer.toString(keepTheseDims);
	}

	public ProjectionMatrix(int numColumns, List<Integer> keepTheseDims) {
		this(numColumns, MathUtils.toIntArray(keepTheseDims));
	}

	@Override
	public Matrix copy() {
		return new ProjectionMatrix(numColumns, keepThese);
	}

	@Override
	public double get(int row, int column) {
		int i = Arrays.binarySearch(keepThese, column);
		if (i < 0)
			return 0;
		if (i != row)
			return 0;
		return 1;
	}

	@Override
	public Iterator<MatrixEntry> iterator() {
		final DMatrixEntry me = new DMatrixEntry(this);
		return new Iterator<MatrixEntry>() {
			int i = 0;

			@Override
			public boolean hasNext() {
				return i < keepThese.length;
			}

			@Override
			public MatrixEntry next() {
				me.update(i, keepThese[i]);
				return me;
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}

	@Override
	public Vector multAdd(double alpha, Vector x, Vector y) {
		if (alpha == 0)
			return y;
		for (VectorEntry ve : x) {
			int i = ve.index();
			int keep = Arrays.binarySearch(keepThese, i);
			if (keep < 0) {
				continue; // discard this column
			}
			double vi = ve.get() * alpha;
			y.add(keep, vi);
		}
		return y;
	}

}
