package com.winterwell.utils.containers;

/**
 * Handy immutable container for a pair of elements.
 * 
 * @author daniel
 * 
 * @param <T>
 */
public class Pair<T> extends Pair2<T, T> {

	private static final long serialVersionUID = 1L;

	/**
	 * Create a pair from a two-element list or set.
	 * 
	 * @param ab
	 */
	public Pair(Iterable<T> ab) {
		super(ab);
	}

	public Pair(T a, T b) {
		super(a, b);
	}

}
