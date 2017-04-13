package com.winterwell.maths.vector;

import java.util.Iterator;

import com.winterwell.utils.containers.SingletonIterator;

import no.uib.cipr.matrix.AbstractVector;
import no.uib.cipr.matrix.Vector;
import no.uib.cipr.matrix.VectorEntry;

/**
 * A unit vector, which is 1 in a single dimension, 0 elsewhere.
 * @author daniel
 *
 */
public class UnitVector extends AbstractVector {
	
	private int on;

	public UnitVector(int dimOn, int dims) {
		super(dims);
		on = dimOn;
		assert on >= 0 : on;
		assert on < dims : on+" v "+dims;
	}
	
	@Override
	public String toString() {
		return "UnitVector_"+on;
	}

	@Override
	public double get(int index) {
		return index==on? 1 : 0;
	}

	@Override
	public UnitVector copy() {
		return new UnitVector(on, size);
	}
	
	@Override
	public Iterator<VectorEntry> iterator() {
		return new SingletonIterator(new VectorEntry() {
			@Override
			public double get() {
				return 1;
			}

			@Override
			public int index() {
				return on;
			}

			@Override
			public void set(double value) {
				throw new UnsupportedOperationException();
			}
		});

	}

	@Override
	public double dot(Vector b) {		
		return b.get(on);
	}
	
	private static final long serialVersionUID = 1L;

}
