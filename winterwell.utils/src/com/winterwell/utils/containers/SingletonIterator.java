/**
 * 
 */
package com.winterwell.utils.containers;

import java.util.Iterator;

/**
 * Iterate over just one value.
 * 
 * @author Daniel
 */
public final class SingletonIterator<X> implements Iterator<X> {
	private boolean used;
	private final X x;

	/**
	 * @param x Iterate over just this value.
	 */
	public SingletonIterator(X x) {
		this.x = x;
	}

	@Override
	public boolean hasNext() {
		return !used;
	}

	@Override
	public X next() {
		used = true;
		return x;
	}

	@Override
	public void remove() throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

}
