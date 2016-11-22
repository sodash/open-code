/**
 * 
 */
package com.winterwell.utils.containers;

import java.io.Serializable;
import java.util.AbstractCollection;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

import com.winterwell.utils.Utils;

/**
 * Trio! Tri-i-i-o! I wanna trio and I want one now.
 * 
 * Ahem. Like a {@link Pair2}, but with three elements. aka Triple. Because I
 * needed one.
 * 
 * @author miles
 */
public final class Trio<A, B, C> extends AbstractCollection<Object> implements Comparable<Trio<A, B, C>>, Collection<Object>, Serializable {
	private static final long serialVersionUID = 1L;
	public final A first;
	public final B second;
	public final C third;

	public Trio(A a, B b, C c) {
		first = a;
		second = b;
		third = c;
	}

	/**
	 * Lexicographic comparison with null=last. The first member of the pair
	 * must implement {@link Comparable}. The second member does not have to (in
	 * which case hashcode will be used as a fallback for comparison).
	 * 
	 * @throws ClassCastException
	 *             if the first member does not implement Comparable
	 */
	@Override
	public final int compareTo(Trio<A, B, C> o) {
		if (first == o.first) {
			Utils.compare(second, o.second);
		}
		if (first == null)
			return 1;
		if (o.first == null)
			return -1;
		int c1 = ((Comparable) first).compareTo(o.first);
		if (c1 != 0)
			return c1;
		// 2nd
		int c2 = Utils.compare(second, o.second);
		if (c2 != 0)
			return c2;
		return Utils.compare(third, o.third);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Trio other = (Trio) obj;
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
		if (third == null) {
			if (other.third != null)
				return false;
		} else if (!third.equals(other.third))
			return false;
		return true;
	}

	public A getFirst() {
		return first;
	}

	public B getSecond() {
		return second;
	}

	public C getThird() {
		return third;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((first == null) ? 0 : first.hashCode());
		result = prime * result + ((second == null) ? 0 : second.hashCode());
		result = prime * result + ((third == null) ? 0 : third.hashCode());
		return result;
	}

	@Override
	public String toString() {
		return "(" + first + "," + second + "," + third + ")";
	}
	@Override
	public final boolean isEmpty() {
		return false;
	}

	@Override
	public final Iterator iterator() {
		return Arrays.asList(first, second, third).iterator();
	}

	@Override
	public int size() {
		return 3;
	}

	@Override
	public Object[] toArray() {
		return new Object[]{first, second, third};
	}

	@Override
	public Object[] toArray(Object[] a) {
		return new Object[] { first, second };
	}

}
