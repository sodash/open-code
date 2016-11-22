package com.winterwell.utils.containers;

import java.util.Collection;
import java.util.Iterator;

/**
 * Iterate over a collection in reverse. Note: performs a copy of the collection
 * upon construction!
 * 
 * @author daniel
 */
public class ReverseIterator<X> implements Iterator<X> {

	private final Object[] array;
	private int i;

	public ReverseIterator(Collection<X> stuff) {
		array = stuff.toArray();
		i = array.length;
	}

	@Override
	public boolean hasNext() {
		return i > 0;
	}

	@Override
	public X next() {
		i--;
		return (X) array[i];
	}

	/**
	 * @throws UnsupportedOperationException
	 */
	@Override
	public void remove() throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

}
