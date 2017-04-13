package com.winterwell.maths.vector;

import com.winterwell.utils.time.Time;

import no.uib.cipr.matrix.AbstractVector;
import no.uib.cipr.matrix.Vector;

/**
 * A 2-dimensional vector. For convenience and some efficiency in the popular
 * 2-dimensional case. Note: unlike most vectors, this does over-ride equals()
 * 
 * @author Daniel
 * 
 */
public final class XY extends AbstractVector {
	private static final long serialVersionUID = 1L;

	// Exposed for convenience
	public double x;

	public double y;

	public XY(double x, double y) {
		super(2);
		this.x = x;
		this.y = y;
	}

	/**
	 * Convenience for the standard-ish way to use time series data. The time
	 * will be encoded as milliseconds-from-epoch.
	 * 
	 * @param t
	 *            x-value
	 * @param y
	 */
	public XY(Time t, double y) {
		this(t.longValue(), y);
	}

	@Override
	public Vector add(double alpha, Vector b) {
		if (alpha == 0)
			return this;
		assert b.size() == 2 : b.size();
		x += b.get(0) * alpha;
		this.y += b.get(1) * alpha;
		return this;
	}

	@Override
	public XY copy() {
		return new XY(x, y);
	}

	@Override
	public double dot(Vector b) {
		assert b.size() == 2 : b.size();
		return x * b.get(0) + y * b.get(1);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		XY other = (XY) obj;
		if (Double.doubleToLongBits(x) != Double.doubleToLongBits(other.x))
			return false;
		if (Double.doubleToLongBits(y) != Double.doubleToLongBits(other.y))
			return false;
		return true;
	}

	@Override
	public double get(int index) {
		assert index == 0 || index == 1;
		return index == 0 ? x : y;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		long temp;
		temp = Double.doubleToLongBits(x);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(y);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		return result;
	}

	@Override
	public Vector set(double alpha, Vector y) {
		if (alpha == 0)
			return zero();
		assert y.size() == 2 : y.size();
		x = y.get(0) * alpha;
		this.y = y.get(1) * alpha;
		return this;
	}

	@Override
	public void set(int index, double value) {
		assert index == 0 || index == 1;
		if (index == 0) {
			x = value;
		} else {
			y = value;
		}
	}

	@Override
	public String toString() {
		return "(" + x + ", " + y + ")";
	}

	@Override
	public Vector zero() {
		x = 0;
		y = 0;
		return this;
	}
}
