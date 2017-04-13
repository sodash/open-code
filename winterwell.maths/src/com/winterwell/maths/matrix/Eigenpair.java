package com.winterwell.maths.matrix;

import java.util.Iterator;

import no.uib.cipr.matrix.AbstractVector;
import no.uib.cipr.matrix.Vector;
import no.uib.cipr.matrix.VectorEntry;

/**
 * A real-valued eigenvalue/vector pair. This is just a container class, it
 * doesn't do anything fancy.
 * 
 * It implements Vector (wrapping the eigenvector) for convenience, so a list of
 * Eigenpairs is also a list of Eigenvectors.
 * 
 */
public final class Eigenpair extends AbstractVector implements
		Comparable<Eigenpair> {
	private static final long serialVersionUID = 1L;

	private final double value;

	private final Vector vector;

	Eigenpair(Vector vector, double value) {
		super(vector.size());
		this.vector = vector;
		this.value = value;
	}

	@Override
	public int compareTo(Eigenpair o) {
		if (o.value > value)
			return 1;
		if (o.value < value)
			return -1;
		return 0;
	}

	@Override
	public Vector copy() {
		return vector.copy();
	}

	@Override
	public double get(int index) {
		return vector.get(index);
	}

	public double getValue() {
		return value;
	}

	public Vector getVector() {
		return vector;
	}

	@Override
	public Iterator<VectorEntry> iterator() {
		return vector.iterator();
	}

	@Override
	public void set(int index, double value) {
		vector.set(index, value);
	}

	@Override
	public String toString() {
		return value + " " + vector;
	}
}
