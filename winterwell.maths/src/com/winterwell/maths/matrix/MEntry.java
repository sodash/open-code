package com.winterwell.maths.matrix;

import no.uib.cipr.matrix.Matrix;

final class MEntry implements no.uib.cipr.matrix.MatrixEntry {

	final private int row;
	final private int col;
	private double v;
	final Matrix base;
	
	public MEntry(int row, int col, double v, Matrix base) {
		this.row = row;
		this.col = col;
		this.v = v;
		this.base = base;
	}

	@Override
	public int row() {
		return row;
	}

	@Override
	public int column() {
		return col;
	}

	@Override
	public double get() {
		return v;
	}

	@Override
	public void set(double value) {
		this.v = value;
		base.set(row, col, value);
	}

}
