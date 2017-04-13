package com.winterwell.maths.vector;

import no.uib.cipr.matrix.AbstractVector;
import no.uib.cipr.matrix.Vector;

/**
 * A 3-dimensional vector. For convenience and some efficiency in the ever
 * popular 3-dimensional case.
 * 
 * @author Daniel
 * 
 */
public final class XYZ extends AbstractVector {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	// Exposed for convenience
	public double x;
	public double y;
	public double z;

	public XYZ(double x, double y, double z) {
		super(3);
		this.x = x;
		this.y = y;
		this.z = z;
	}

	/**
	 * Copies coords from the array
	 * 
	 * @param xyz
	 */
	public XYZ(double[] xyz) {
		this(xyz[0], xyz[1], xyz[2]);
		assert xyz.length == 3;
	}

	@Override
	public Vector add(double alpha, Vector b) {
		if (alpha == 0)
			return this;
		assert b.size() == 3 : b.size();
		x += b.get(0) * alpha;
		y += b.get(1) * alpha;
		z += b.get(2) * alpha;
		return this;
	}

	@Override
	public XYZ copy() {
		return new XYZ(x, y, z);
	}

	@Override
	public double dot(Vector b) {
		assert b.size() == 3 : b.size();
		return x * b.get(0) + y * b.get(1) + z * b.get(2);
	}

	@Override
	public double get(int index) {
		assert index == 0 || index == 1 || index == 2;
		return index == 0 ? x : index == 1 ? y : z;
	}

	@Override
	public Vector set(double alpha, Vector b) {
		if (alpha == 0)
			return zero();
		assert b.size() == 3 : b.size();
		x = b.get(0) * alpha;
		y = b.get(1) * alpha;
		z = b.get(2) * alpha;
		return this;
	}

	@Override
	public void set(int index, double value) {
		assert index == 0 || index == 1 || index == 2;
		if (index == 0) {
			x = value;
		} else if (index == 1) {
			y = value;
		} else {
			z = value;
		}
	}

	@Override
	public String toString() {
		return "(" + x + ", " + y + ", " + z + ")";
	}

	@Override
	public Vector zero() {
		x = 0;
		y = 0;
		z = 0;
		return this;
	}
}
