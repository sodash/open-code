package com.winterwell.maths.vector;

import java.util.Iterator;

import com.winterwell.utils.Printer;
import com.winterwell.utils.containers.SingletonIterator;

import no.uib.cipr.matrix.AbstractVector;
import no.uib.cipr.matrix.Vector;
import no.uib.cipr.matrix.VectorEntry;

/**
 * A 1-dimensional vector, otherwise known as a number. For convenience and some
 * efficiency in the popular 1-dimensional case. Note: unlike most vectors, this
 * does over-ride equals()
 * 
 * @author Daniel
 * 
 */
public final class X extends AbstractVector {
	private static final long serialVersionUID = 1L;

	// Exposed for convenience
	public double x;

	/**
	 * A 1-dimensional vector, otherwise known as a number. For convenience and
	 * some efficiency in the popular 1-dimensional case.
	 */
	public X(double x) {
		super(1);
		this.x = x;
	}

	@Override
	public Vector add(double alpha, Vector b) {
		if (alpha == 0)
			return this;
		assert b.size() == 1 : b.size();
		x += b.get(0) * alpha;
		return this;
	}

	@Override
	public X copy() {
		return new X(x);
	}

	@Override
	public double dot(Vector b) {
		assert b.size() == 1 : b.size();
		return x * b.get(0);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		X other = (X) obj;
		if (Double.doubleToLongBits(x) != Double.doubleToLongBits(other.x))
			return false;
		return true;
	}

	@Override
	public double get(int index) {
		assert index == 0;
		return x;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		long temp;
		temp = Double.doubleToLongBits(x);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		return result;
	}

	@Override
	public Iterator<VectorEntry> iterator() {
		return new SingletonIterator(new VectorEntry() {
			@Override
			public double get() {
				return x;
			}

			@Override
			public int index() {
				return 0;
			}

			@Override
			public void set(double value) {
				x = value;
			}
		});
	}

	@Override
	public Vector set(double alpha, Vector y) {
		if (alpha == 0)
			return zero();
		assert y.size() == 1 : y.size();
		x = y.get(0) * alpha;
		return this;
	}

	@Override
	public void set(int index, double value) {
		assert index == 0;
		x = value;
	}

	@Override
	public String toString() {
		return Printer.toString(x);
	}

	@Override
	public Vector zero() {
		x = 0;
		return this;
	}
}
