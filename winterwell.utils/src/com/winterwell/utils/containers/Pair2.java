package com.winterwell.utils.containers;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import com.winterwell.utils.Utils;

/**
 * Inspite of the name, this is the base class for Pair. It's more general wrt
 * types.
 * 
 * @author daniel
 * 
 * @param <A>
 * @param <B>
 */
public class Pair2<A, B> implements Comparable<Pair2<A, B>>, Collection,
		Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * Utility method to extract the first value of a pair
	 * 
	 * @param pairs
	 * @return the first element of each pair.
	 */
	public static <X> List<X> firsts(Collection<Pair<X>> pairs) {
		List<X> list = new ArrayList(pairs.size());
		for (Pair<X> pair : pairs) {
			list.add(pair.first);
		}
		return list;
	}

	public final A first;

	public final B second;

	public Pair2(A a, B b) {
		first = a;
		second = b;
	}

	/**
	 * Create a pair from a two-element list or set.
	 * 
	 * @param ab
	 */
	@SuppressWarnings("unchecked")
	public Pair2(Iterable<?> ab) {
		Iterator<?> it = ab.iterator();
		first = (A) it.next();
		second = (B) it.next();
		assert !it.hasNext() : ab;
	}

	@Deprecated
	@Override
	public final boolean add(Object e) throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

	@Deprecated
	@Override
	public final boolean addAll(Collection c)
			throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

	@Deprecated
	@Override
	public final void clear() throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

	/**
	 * Lexicographic comparison -- i.e. sort by first-member first -- with
	 * null=last. The first member of the pair must implement {@link Comparable}
	 * . The second member does not have to (in which case hashcode will be used
	 * as a fallback for comparison).
	 * 
	 * @throws ClassCastException
	 *             if the first member does not implement Comparable
	 */
	@Override
	public final int compareTo(Pair2<A, B> o) {
		int c = Utils.compare(first, o.first);
		if (c != 0)
			return c;
		return Utils.compare(second, o.second);
	}

	@Override
	public boolean contains(Object o) {
		return Utils.equals(o, first) || Utils.equals(o, second);
	}

	@Override
	public final boolean containsAll(Collection c) {
		for (Object object : c) {
			if (!contains(object))
				return false;
		}
		return true;
	}

	@Override
	public final boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final Pair2 other = (Pair2) obj;
		if (first == null) {
			if (other.first != null)
				return false;
		} else if (!first.equals(other.first))
			return false;
		if (second == null) {
			if (other.second != null)
				return false;
		} else if (!second.equals(other.second))
			return false;
		return true;
	}

	public final A getFirst() {
		return first;
	}

	public final B getSecond() {
		return second;
	}

	@Override
	public final int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((first == null) ? 0 : first.hashCode());
		result = prime * result + ((second == null) ? 0 : second.hashCode());
		return result;
	}

	@Override
	public final boolean isEmpty() {
		return false;
	}

	@Override
	public final Iterator iterator() {
		return Arrays.asList(first, second).iterator();
	}

	@Deprecated
	@Override
	public final boolean remove(Object o) throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

	@Deprecated
	@Override
	public final boolean removeAll(Collection c)
			throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

	@Deprecated
	@Override
	public final boolean retainAll(Collection c)
			throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

	@Override
	public final int size() {
		return 2;
	}

	@Override
	public final Object[] toArray() {
		return new Object[] { first, second };
	}

	@Override
	public final Object[] toArray(Object[] a) {
		return new Object[] { first, second };
	}

	@Override
	public String toString() {
		return "(" + first + ", " + second + ")";
	}

}
