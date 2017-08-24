package com.winterwell.maths.datastorage;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import com.winterwell.utils.Printer;
import com.winterwell.utils.containers.Containers;
import com.winterwell.utils.containers.IntRange;
import com.winterwell.utils.io.XStreamBinaryConverter.BinaryXML;

import gnu.trove.map.hash.TObjectIntHashMap;

/**
 * An index maps objects to index numbers (starting at zero), and vice-versa. It
 * is just an ArrayList with a (trove) HashMap.
 * 
 * Should be threadsafe.
 * 
 * @author Daniel
 */
@BinaryXML
public final class Index<T> implements IIndex<T>, Serializable {

	private static final long serialVersionUID = 1L;
	private final ArrayList<T> i2o;
	/**
	 * Maps object to (index+1). Methods which use this subtract 1. This is to
	 * allow a zero-index while distinguishing "not in the index" (trove get()
	 * annoyingly returns zero).
	 */
	private final TObjectIntHashMap<T> o2i;

	public Index() {
		o2i = new TObjectIntHashMap<T>();
		i2o = new ArrayList<T>();
	}

	public Index(Collection<? extends T> dictionary) {
		int n = dictionary.size();
		o2i = new TObjectIntHashMap<T>(n);
		i2o = new ArrayList<T>(n);
		for (T obj : dictionary) {
			add(obj);
		}
	}

	@Override
	public synchronized int add(T obj) {
		assert !o2i.containsKey(obj);
		int i = o2i.size() + 1;
		o2i.put(obj, i);
		i2o.add(obj);
		return i - 1;
	}

	/**
	 * It's a no-op here.
	 */
	@Deprecated
	@Override
	public void addListener(IPruneListener listener) {
		// ignore
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see winterwell.maths.datastorage.IIndex#contains(T)
	 */
	@Override
	public boolean contains(T obj) {
		return o2i.containsKey(obj);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see winterwell.maths.datastorage.IIndex#get(int)
	 */
	@Override
	public T get(int i) {
		if (i >= i2o.size() || i<0)
			return null;
		return i2o.get(i);
	}

	@Override
	public Iterable<Integer> getIndexValues() {
		return new IntRange(0, i2o.size() - 1);
	}

	@Override
	public int indexOf(T obj) {
		int i = o2i.get(obj);
		return i - 1;
	}

	/**
	 * Convenience method to apply {@link #indexOf(Object)} to each object in
	 * turn.
	 * 
	 * @param keySet
	 * @return indices
	 */
	public List<Integer> indexOfAll(Collection<T> keySet) {
		List<Integer> is = new ArrayList(keySet.size());
		for (T t : keySet) {
			is.add(indexOf(t));
		}
		return is;
	}

	@Override
	public int indexOfWithAdd(T obj) {
		// do not synchronise for fast handling of the normal condition
		int i = indexOf(obj);
		if (i != -1)
			return i;
		synchronized (this) {
			// handle race condition
			i = indexOf(obj);
			if (i != -1)
				return i;
			i = add(obj);
			return i;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see winterwell.maths.datastorage.IIndex#iterator()
	 */
	@Override
	public synchronized Iterator<T> iterator() {
		// copy to allow other threads to modify whilst we iterate
		// return new ArrayList<T>(i2o).iterator();
		return new Iterator<T>() {
			int i;

			@Override
			public boolean hasNext() {
				return i < i2o.size();
			}

			@Override
			public T next() {
				T next = i2o.get(i);
				i++;
				return next;
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see winterwell.maths.datastorage.IIndex#size()
	 */
	@Override
	public int size() {
		return i2o.size();
	}

	@Override
	public Object[] toArray() {
		return i2o.toArray();
	}

	@Override
	public String toString() {
		return "Index[" + Printer.toString(Containers.subList(i2o, 0, 3), ",")
				+ "..." + size() + "]";
	}

	/**
	 * @param list Can contain dupes, which will only be indexed once of course. Not thread safe.
	 */
	public void addAll(Iterable<? extends T> list) {
		for (T t : list) {
			// skip dupes
			if (o2i.containsKey(t)) continue;
			int i = o2i.size() + 1;
			o2i.put(t, i);
			i2o.add(t);
		}
	}

}
