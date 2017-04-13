package com.winterwell.maths.matrix;

import java.util.Arrays;
import java.util.Iterator;

import org.junit.Test;

import no.uib.cipr.matrix.MatrixEntry;
import no.uib.cipr.matrix.Vector;
import no.uib.cipr.matrix.Vector.Norm;

public class SparseMatrixTest {

	@Test
	public void testGetColumn() {
		SparseMatrix m = new SparseMatrix();
		m.set(1, 2, 12);
		m.set(200, 11, 20011);
		m.set(100, 11, 10011);
		Vector col = m.getColumn(11);
		assert col.get(100) == 10011;
		assert col.get(200) == 20011;
		assert col.norm(Norm.One) == 30022;
	}

	@Test
	public void testIterator() {
		SparseMatrix m = new SparseMatrix();
		m.set(1, 2, 12);
		m.set(200, 11, 20011);
		Iterator<MatrixEntry> it = m.iterator();

		MatrixEntry a = it.next();
		int[] rows = new int[2];
		rows[0] = a.row();
		if (a.row() != 1) {
			assert a.row() == 200;
			assert a.column() == 11;
			assert a.get() == 20011;
		} else {
			assert a.row() == 1 : a;
			assert a.column() == 2;
			assert a.get() == 12;
		}

		MatrixEntry b = it.next();
		rows[1] = b.row();
		if (b.row() != 1) {
			assert b.row() == 200;
			assert b.column() == 11;
			assert b.get() == 20011;
		} else {
			assert b.row() == 1 : b;
			assert b.column() == 2;
			assert b.get() == 12;
		}

		Arrays.sort(rows);
		assert rows[0] == 1;
		assert rows[1] == 200;
	}

}
