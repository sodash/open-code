package com.winterwell.maths.matrix;

import java.util.Iterator;

import com.winterwell.maths.vector.ICompact;
import com.winterwell.maths.vector.IntXY;

import gnu.trove.iterator.TObjectDoubleIterator;
import gnu.trove.map.hash.TObjectDoubleHashMap;
import no.uib.cipr.matrix.AbstractMatrix;
import no.uib.cipr.matrix.MatrixEntry;
import no.uib.cipr.matrix.Vector;
import no.uib.cipr.matrix.sparse.SparseVector;

/**
 * A HashMap-backed matrix for sparse non-symmetric use.
 * 
 * WARNING: this is probably not thread safe. It back-ends onto a Trove
 * hash-map.
 * 
 * @testedby {@link SparseMatrixTest}
 * @author daniel
 * 
 */
public class SparseMatrix extends AbstractMatrix implements ICompact {

	/**
	 * the keys are (row, column)
	 */
	private final TObjectDoubleHashMap<IntXY> map = new TObjectDoubleHashMap<IntXY>();

	public SparseMatrix() {
		this(Integer.MAX_VALUE, Integer.MAX_VALUE);
	}

	public SparseMatrix(int numRows, int numColumns) {
		super(numRows, numColumns);
	}

	@Override
	public void compact() {
		// prune the zeroes
		TObjectDoubleIterator<IntXY> it = map.iterator();
		while (it.hasNext()) {
			it.advance();
			if (it.value() == 0) {
				it.remove();
			}
		}
		// compact
		map.compact();
	}

	@Override
	public SparseMatrix copy() {
		SparseMatrix sm = new SparseMatrix(numRows, numColumns);
		TObjectDoubleIterator<IntXY> it = map.iterator();
		while (it.hasNext()) {
			sm.map.put(it.key(), it.value());
			it.advance();
		}
		return sm;
	}

	@Override
	public double get(int row, int column) {
		IntXY xy = new IntXY(row, column);
		double v = map.get(xy);
		return v;
	}

	/**
	 * This is NOT backed by the matrix :(
	 * 
	 * @param col
	 * @return a column from this matrix
	 */
	public Vector getColumn(int col) {
		SparseVector vec = new SparseVector(numRows);
		for (MatrixEntry me : this) {
			if (me.column() != col) {
				continue;
			}
			vec.set(me.row(), me.get());
		}
		return vec;
	}

	@Override
	public Iterator<MatrixEntry> iterator() {
		final TObjectDoubleIterator<IntXY> it = map.iterator();
		final DMatrixEntry me = new DMatrixEntry(this);
		// Trove being an arse about not implementing Java iterators
		Iterator<MatrixEntry> mit = new Iterator<MatrixEntry>() {
			@Override
			public boolean hasNext() {
				return it.hasNext();
			}

			@Override
			public MatrixEntry next() {
				it.advance();
				IntXY rc = it.key();
				me.update(rc.x, rc.y);
				return me;
			}

			@Override
			public void remove() {
				it.remove();
			}
		};
		return mit;
	}

	@Override
	public void set(int row, int column, double value) {
		IntXY xy = new IntXY(row, column);
		// This causes a ConcurrentModificationException if you use it
		// during a matrix iteration :(
		// So we have to rely on compact to do key removal
		// if (value==0) map.remove(xy);
		// else
		map.put(xy, value);
	}

}
