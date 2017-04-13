package com.winterwell.maths.stats.distributions.discrete;

import java.util.Collections;
import java.util.Iterator;

/**
 * Distribution with only one non-zero element.
 * 
 * @author daniel
 * 
 * @param <T>
 */
public final class SingletonDistribution<T> extends AFiniteDistribution<T> {

	private final T v;

	public SingletonDistribution(T v) {
		this.v = v;
		normalised = true;
	}

	@Override
	public T getMostLikely() {
		return v;
	}

	@Override
	public Iterator<T> iterator() throws UnsupportedOperationException {
		return Collections.singleton(v).iterator();
	}

	@Override
	public double prob(T x) {
		return v.equals(x) ? 1 : 0;
	}

	@Override
	public int size() {
		return 1;
	}

	@Override
	public String toString() {
		return "SingletonDistribution[" + v + "]";
	}
}
