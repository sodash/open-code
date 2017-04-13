/**
 * 
 */
package com.winterwell.maths.matrix;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import no.uib.cipr.matrix.AbstractMatrix;
import no.uib.cipr.matrix.Matrix;
import no.uib.cipr.matrix.MatrixEntry;

/**
 * A number!
 * @author daniel
 * TODO make this efficient
 */
public final class Matrix1D extends AbstractMatrix {

	private double x;

	@Override
	public double get(int row, int column) {
		assert row==0 && column==0;
		return x;
	}
	
	@Override
	public void set(int row, int column, double value) {
		assert row==0 && column==0;
		x = value;
	}
	
	public Matrix1D(double x) {
		super(1, 1);
		this.x = x;
	}			
	
	@Override
	public String toString() {
		return "[["+x+"]]";
	}
	
	@Override
	public Matrix copy() {
		return new Matrix1D(x);
	}
	
	@Override
	public Iterator<MatrixEntry> iterator() {
		List list = Collections.singletonList(new MEntry(0, 0, x, this));
		return list.iterator();
	}
}
