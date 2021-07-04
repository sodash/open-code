package com.winterwell.utils.containers;

import java.io.Serializable;
import java.util.AbstractList;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import com.winterwell.utils.log.Log;

/**
 * A Set implementation backed by an {@link ArrayList}. Fast for small
 * collections, lousy for big ones. Has a convenient constructor. Another nice
 * property is that insertion order is preserved. Skips `null`.
 * 
 * FIXME {@link #equals(Object)} is List equals!
 * 
 * @author daniel
 * @testedby ArraySetTest
 * @param <T>
 */
public final class ArraySet<T> extends AbstractSet<T> 
	implements 
	Set<T>, 
//	SortedSet<T> ??
//	List<T> -- no, causes a conflict between Java 7 and 8
		Serializable {
	private static final long serialVersionUID = 1L;

//	// Arse: Java 7 vs 8 -- not 100% compatible	
//	static Method spliteratorFn;	
//	
//	public Spliterator<T> spliterator() {
//		if (spliteratorFn==null) {
//			try {
//				spliteratorFn = AbstractList.class.getMethod("spliterator");
//			} catch (Exception e) {
//				throw Utils.runtime(new NoSuchMethodException("No method spliterator() Is this Java 7?"));
//			}
//		}
//		try {
//			return (Spliterator<T>) spliteratorFn.invoke(this);
//		} catch (Exception e) {
//			throw Utils.runtime(e);
//		}
//	}
	
	private final ArrayList<T> backing;

	public ArraySet() {
		backing = new ArrayList<T>(4); // assume small (otherwise a HashSet is better)
	}

	/**
	 * Create a new ArraySet which is a shallow copy of elements.
	 */
	public ArraySet(Collection<? extends T> elements) {
		backing = new ArrayList<T>(elements.size());
		for (T e : elements) {
			add(e);
		}
	}

	public ArraySet(int initialSize) {
		backing = new ArrayList<T>(initialSize);
	}

	/**
	 * e.g. new ArraySet("item 1", "item 2", "item 3")
	 * @param elements
	 */
	public ArraySet(T... elements) {
		// Entered 1 element, which is an array or a number? This could be valid
		// -- but it's probably a mistake!
		if (elements.length == 1
				&& elements[0] != null
				&& (elements[0].getClass().isArray() || elements[0] instanceof Number)) {
			Log.w("ArraySet", "Probable bug! elements=[" + elements[0] + "] Use ArraySet.fromArray() instead.");
		}
		backing = new ArrayList<T>(elements.length);
		for (T e : elements) {
			add(e);
		}
	}
	
	public static ArraySet<String> fromArray(String[] array) {
		return new ArraySet(Arrays.asList(array));
	}
	public static ArraySet<Object> fromArray(Object[] array) {
		return new ArraySet(Arrays.asList(array));
	}

	/**
	 * {@inheritDoc}
	 * Will skip `null` (returns false)
	 */
	@Override
	public boolean add(T e) {
		if (backing.contains(e) || e==null)
			return false;
		return backing.add(e);
	}

	@Override
	public void clear() {
		backing.clear();
	}

	@Override
	public boolean contains(Object o) {
		// assert o == null || ((T) o) != null; // sanity check input class --
		// sadly doesn't do anything compile time
		return backing.contains(o);
	}

	/**
	 * Since this is backed by an array-list, you might as well have indexed
	 * access.
	 * 
	 * @param i
	 * @return
	 */
	public T get(int i) {
		return backing.get(i);
	}

	@Override
	public Iterator iterator() {
		return backing.iterator();
	}

	@Override
	public boolean remove(Object o) {
		assert o == null || ((T) o) != null; // sanity check input class
		return backing.remove(o);
	}

	@Override
	public int size() {
		return backing.size();
	}

	// NB: This method is from SortedSet -- we could implement the rest of that (but why?)
	public T first() {
		if (backing.isEmpty()) throw new NoSuchElementException();
		return backing.get(0);
	}

	/**
	 * @return a List view of this ArraySet  
	 */
	public List<T> asList() {
		// why not just return the `backing` list??
		return new SetAsList();
	}

	/**
	 * Provide a List view of the ArraySet.
	 */
	final class SetAsList extends AbstractList<T> {

		@Override
		public boolean add(T e) {
			return ArraySet.this.add(e);
		}
		
		/**
		 * This method does NOT implement set's no-duplicates check! Using this can
		 * allow duplicate entries.
		 */
	@Override
		@Deprecated
		public T set(int index, T element) {
			T old = backing.set(index, element);
			return old;
		}
		
		@Override
		public Iterator<T> iterator() {
			return backing.iterator();
		}
		
		@Override
		public T get(int index) {
			return backing.get(index);
		}
		
		@Override
		public boolean remove(Object o) {
			return backing.remove(o);
		}
		
		@Override
		public boolean contains(Object o) {
			return backing.contains(o);
		}
		
		@Override
		public int size() {
			return backing.size();
		}		
	}
}


