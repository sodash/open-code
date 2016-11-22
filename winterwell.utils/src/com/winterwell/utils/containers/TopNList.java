package com.winterwell.utils.containers;

import java.util.AbstractList;
import java.util.Arrays;
import java.util.Comparator;

import com.winterwell.utils.Utils;

/**
 * The top-n items from a collection. This finds the unique top-n - ie there are
 * no duplicates (it's a Set). 
 * <p>
 * If using {@link #maybeAdd(Object)}, these are the top-n according to sort order, e.g.
 * A, B, C or 1, 2, 3 (dropping the high numbers). Change the comparator to
 * alter this. However, if using {@link #maybeAdd(Object, double)}, these are the highest-scoring.
 * 
 * TODO support using in unbounded mode as a general-purpose sorted list.
 * 
 * @testedby {@link TopNListTest}
 * @author daniel
 * 
 */
public class TopNList<X> extends AbstractList<X> 
//implements SortedSet<X> 
{

	// How do we do this?
//	public Spliterator<X> spliterator() {
//		return super.spliterator();
//	}
	
	/**
	 * A safety check on the use of {@link #maybeAdd(Object, double)}
	 */
	transient Boolean scored;
	
	final Comparator comp;
	/** ordered list of the top n. may contain nulls intially */
	private final Object[] objects;

	/**
	 * The actual size of the list
	 */
	private int size = 0;

	/**
	 * The top-n items (by sort order -- so low numbers win!) from a collection.
	 */
	public TopNList(int n) {
		this(n, null);
	}

	/**
	 * The top-n items (by sort order) from a collection.
	 * @param n
	 * @param comp Warning: This assumes the comparator provides a total-ordering.
	 */
	public TopNList(int n, Comparator<X> comp) {
		assert n > 0;
		this.comp = comp;
		objects = new Object[n];
	}

	/**
	 * *Maybe* add the element. This breaks the true contract for List, as the
	 * element may not get added. Explicitly call {@link #maybeAdd(Object)} for
	 * preference.
	 */
	@Override
	public boolean add(X element) {
		return maybeAdd(element);
	}

	/**
	 * Code copied from {@link Arrays#binarySearch(Object[], object)} - which
	 * annoyingly doesn't accept a pluggable comparison operator.
	 * <p>
	 * Searches the specified array for the specified object using the binary
	 * search algorithm. The array must be sorted into ascending order.
	 * 
	 * @param a
	 *            the array to be searched
	 * @param key
	 *            the value to be searched for
	 * @return index of the search key, if it is contained in the array;
	 *         otherwise, <tt>(-(<i>insertion point</i>) - 1)</tt>. The
	 *         <i>insertion point</i> is defined as the point at which the key
	 *         would be inserted into the array: the index of the first element
	 *         greater than the key, or <tt>a.length</tt> if all elements in the
	 *         array are less than the specified key. Note that this guarantees
	 *         that the return value will be &gt;= 0 if and only if the key is
	 *         found.
	 * @throws ClassCastException
	 *             if the search key is not comparable to the elements of the
	 *             array.
	 */
	private int binarySearch(X obj) {
		int low = 0;
		int high = size() - 1;
		while (low <= high) {
			int mid = (low + high) >>> 1;
			Object midVal = objects[mid];
			int cmp = comp == null ? ((Comparable) midVal).compareTo(obj)
					: comp.compare(midVal, obj);
			if (cmp < 0) {
				low = mid + 1;
			} else if (cmp > 0) {
				high = mid - 1;
			} else
				return mid; // key found
		}
		return -(low + 1); // key not found.
	}

	@Override
	public X get(int index) {
		X obj = (X) objects[index];
		if (obj instanceof TopNList.Scored)
			return ((Scored) obj).x;
		return obj;
	}

	/**
	 * Add obj if it's score is high enough -- this *does* prefer higher numbers (e.g. 3 beats 2). 
	 * <br>
	 * You CANNOT mix this and {@link #maybeAdd(Object)} within a single list.
	 * 
	 * @param obj
	 * @param score
	 * @return true if added
	 */
	public boolean maybeAdd(X obj, double score) {
		// defend against mis-use
		assert scored == null || scored==true;
		scored = true;
		TopNList me = this;
		return me.maybeAdd(new Scored(obj, score));
	}

	/**
	 * Add obj if it ranks high enough in the comparison
	 * 
	 * @param obj
	 * @return true if added
	 */
	public boolean maybeAdd(X obj) {
		// defend against mis-use
		assert scored == null || scored==false || obj instanceof TopNList.Scored;
		if (scored==null) scored = false;
		assert obj != null;
		int i = binarySearch(obj);
		if (i >= 0)
			// already a member of the list
			return false;
		// insert?
		i = -i - 1;
		if (i >= objects.length)
			return false;
		// yes, insert
		Object prev = obj;
		for (int j = i; j < objects.length; j++) {
			Object newPrev = objects[j];
			objects[j] = prev;
			prev = newPrev;
		}
		size++;
		if (size == objects.length + 1) {
			size = objects.length;
		}
		return true;
	}

	@Override
	public int size() {
		return size;
	}

	/**
	 * Used for slightly hacky support of scored (not compared) objects.
	 */
	private class Scored implements Comparable<Scored> {
		final double score;
		final X x;

		Scored(X x, double score) {
			this.score = score;
			this.x = x;
		}
		
		@Override
		public int compareTo(Scored o) {
			if (score != o.score) {
				// Prefer HIGHER numbers!
				return score > o.score ? -1 : 1;
			}
			// Never return 0 -- it leads to dropped items!
			// Prefer not null
			if (x==null) return 1; if (o.x==null) return -1;
			int xhc = x.hashCode(), oxhc = o.x.hashCode();
			if (xhc==oxhc) {
				// Same hashcode?!
				return Utils.getRandomChoice(0.5)? 1 : -1;
			}
			return xhc>oxhc? 1 : -1; 

		}
		
		@Override
		public String toString() {
			return x+"("+score+")";
		}
	}

	public Comparator<? super X> comparator() {
		return comp;
	}

//	@Override
//	public SortedSet<X> subSet(X fromElement, X toElement) {
//		throw new TodoException();
//	}
//
//	@Override
//	public SortedSet<X> headSet(X toElement) {
//		throw new TodoException();
//	}
//
//	@Override
//	public SortedSet<X> tailSet(X fromElement) {
//		throw new TodoException();
//	}

//	@Override
	public X first() {
		return get(0);
	}

//	@Override
	public X last() {
		return get(size() - 1);
	}

}
