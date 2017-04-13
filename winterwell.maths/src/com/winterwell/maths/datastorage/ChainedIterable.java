package com.winterwell.maths.datastorage;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class ChainedIterable<T> implements Iterable<T>, Iterator<T> {

	private int i = 0;
	private Iterator<T> it;
	private final Iterable<T>[] iterables;

	public ChainedIterable(Iterable<T>... iterables) {
		this.iterables = iterables;
		it = iterables[i].iterator();
	}

	@Override
	public boolean hasNext() {
		if (it.hasNext())
			return true;
		i++;
		if (i >= iterables.length)
			return false;
		it = iterables[i].iterator();
		return hasNext();
	}

	@Override
	public Iterator<T> iterator() {
		return this;
	}

	@Override
	public T next() {
		if (it.hasNext())
			return it.next();
		i++;
		if (i >= iterables.length)
			throw new NoSuchElementException();
		it = iterables[i].iterator();
		return next();
	}

	@Override
	public void remove() {
		it.remove();
	}

}
