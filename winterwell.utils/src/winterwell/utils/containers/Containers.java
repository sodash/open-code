/* (c) Winterwell 2008-2011
 * 
 */
package winterwell.utils.containers;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.AbstractList;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;

import com.winterwell.utils.Printer;
import com.winterwell.utils.containers.AbstractMap2;
import com.winterwell.utils.containers.ContainersTest;

import winterwell.utils.IFilter;
import winterwell.utils.IFn;
import winterwell.utils.IProperties;
import winterwell.utils.Key;

import com.winterwell.utils.ReflectionUtils;

import winterwell.utils.TodoException;
import winterwell.utils.Utils;
import winterwell.utils.reporting.Log;

/**
 * Helpful utility methods for working with Collections and arrays.
 * 
 * @author daniel
 * 
 */
public class Containers {

	/**
	 * More self-documenting version of Pair<Collection<X>> for returning
	 * added/deleted pairs.
	 * 
	 * @author miles
	 * 
	 * @param <X>
	 */
	public static final class Changes<X> {
		private final List<X> added;
		private final List<X> deleted;

		public Changes(List<X> added, List<X> deleted) {
			this.added = added;
			this.deleted = deleted;
		}

		public List<X> getAdded() {
			return added;
		}

		public List<X> getDeleted() {
			return deleted;
		}

		public boolean isEmpty() {
			return added.isEmpty() && deleted.isEmpty();
		}

		public int size() {
			return added.size() + deleted.size();
		}

		@Override
		public String toString() {
			return "[Added: " + Printer.toString(added) + "	Deleted: "
					+ Printer.toString(deleted) + "]";
		}
	}


	public static final <X> IFilter<X> And(final IFilter<X> filterA,
			final IFilter<X> filterB) 
	{		
		// backwards compatibility :(
		if (Utils.yes(null)) {
			return new IFilter<X>() {
				@Override
				public boolean accept(X x) {
					return filterA.accept(x) && filterB.accept(x);
				}
			};
		}
		return new And(filterA, filterB);
	}

	/**
	 * Produces a new List of values by mapping each value in list through a
	 * transformation function.
	 * 
	 * @param fn
	 * @param list
	 * @return [fn applied to each member of list]
	 */
	public static <I, O> List<O> apply(IFn<I, O> fn, Collection<? extends I> list) {
		ArrayList after = new ArrayList(list.size());
		for (I object : list) {
			try {
				O o = fn.apply(object);
				after.add(o);
			} catch(Exception ex) {
				throw Utils.runtime(ex);
			}
		}
		return after;
	}
	

	/**
	 * Produces a new Map of key/values by mapping each value in list through a
	 * transformation function.
	 * 
	 * @param fn Can return null, which will remove that entry.
	 * @param list
	 * @return [fn applied to each member of list]
	 */
	public static <K, I, O> Map<K,O> applyToValues(IFn<I, O> fn, Map<? extends K, ? extends I> map) {
		HashMap after = new HashMap(map.size());
		for (K k : map.keySet()) {
			try {
				I v = map.get(k);				
				O o = fn.apply(v);
				if (o==null) continue;
				after.put(k, o);
			} catch(Exception ex) {
				throw Utils.runtime(ex);
			}
		}
		return after;
	}

	/**
	 * Lazily produces a new Iterable of values by mapping each value in through
	 * a transformation function.
	 * 
	 * @param fn
	 * @param list
	 * @return [fn applied to each member of list]
	 * @see #filterLazy(IFilter, Iterable)
	 */
	public static <I, O> Iterable<O> applyLazy(Iterable<? extends I> list,
			IFn<I, O> fn) {
		return new IterableFn<I, O>(list, fn);
	}

	/**
	 * Convert obsolete-Java Enumeration into List
	 */
	public static <X> List<X> getList(final Enumeration<X> e) {		
		ArrayList<X> list = new ArrayList<X>();
		while(e.hasMoreElements()) {
			list.add(e.nextElement());
		}
		return list;
	}
	
	
	/**
	 * Wrapper for {@link Arrays#asList(Object...)} which can handle arrays of primitives,
	 * e.g. int[] or double[].
	 * Throws an IllegalArgumentException if 'array' is not an array (eg. an int
	 * value)
	 */
	public static List<Object> asList(final Object array) {
		if ( ! array.getClass().isArray())
			throw new IllegalArgumentException("Backing object is not an array");
				
		if (array instanceof int[]) {
			return (List) asList((int[])array);
		}
		if (array instanceof double[]) {
			return (List) asList((double[])array);
		}
		if (array instanceof long[]) {
			return (List) asList((long[])array);
		}
		if (array instanceof boolean[]) {
			return (List) asList((boolean[])array);
		}		
		return Arrays.asList((Object[])array);
	}

	public static List<Boolean> asList(final boolean[] xs) {
		return new BooleanArray(xs);	
	}

	public static List<Double> asList(final double[] xs) {
		return new AbstractList<Double>() {
			@Override
			public Double get(int index) {
				return xs[index];
			}

			@Override
			public Double set(int index, Double element) {
				double old = xs[index];
				xs[index] = element;
				return old;
			}

			@Override
			public int size() {
				return xs.length;
			}
		};
	}

	public static List<Integer> asList(final int[] xs) {
		return new AbstractList<Integer>() {
			@Override
			public Integer get(int index) {
				return xs[index];
			}

			@Override
			public Integer set(int index, Integer element) {
				int old = xs[index];
				xs[index] = element;
				return old;
			}

			@Override
			public int size() {
				return xs.length;
			}
		};
	}

	public static List<Long> asList(final long[] xs) {
		return new AbstractList<Long>() {
			@Override
			public Long get(int index) {
				return xs[index];
			}

			@Override
			public Long set(int index, Long element) {
				long old = xs[index];
				xs[index] = element;
				return old;
			}

			@Override
			public int size() {
				return xs.length;
			}
		};
	}

	@Deprecated
	public static List<Object> asList(final Object[] objects) {
		return new AbstractList<Object>() {
			@Override
			public Object get(int index) {
				return objects[index];
			}

			@Override
			public Object set(int index, Object element) {
				Object old = objects[index];
				objects[index] = element;
				return old;
			}

			@Override
			public int size() {
				return objects.length;
			}
		};
	}

	@Deprecated // Arrays.asList() works, provided the argument has compile time type Object[] or String[]
	public static List<String> asList(final String[] strings) {
		if (Utils.yes(false)) {
			return new AbstractList<String>() {
				@Override
				public String get(int index) {
					return strings[index];
				}
	
				@Override
				public String set(int index, String element) {
					String old = strings[index];
					strings[index] = element;
					return old;
				}
	
				@Override
				public int size() {
					return strings.length;
				}
			};
		}
		return Arrays.asList(strings);
	}


	/**
	 * Chop up a list into (almost) equal-sized pieces. The last piece may be
	 * longer than the others.
	 * 
	 * @param <X>
	 * @param data
	 * @param pieces
	 * @return
	 * @testedby {@link ContainersTest#testChop()}
	 */
	public static <X> List<List<X>> chop(List<X> data, int pieces) {
		assert !data.isEmpty();
		assert pieces > 0;
		int n = data.size() / pieces;
		assert n > 0;
		int i = 0;
		List<List<X>> slices = new ArrayList<List<X>>(pieces);
		for (int p = 0; p < pieces; p++) {
			List<X> slice = new ArrayList<X>(n);
			for (int sn = (p + 1) * n; i < sn; i++) {
				X x = data.get(i);
				slice.add(x);
			}
			slices.add(slice);
		}
		List<X> lastSlice = last(slices);
		for (; i < data.size(); i++) {
			X x = data.get(i);
			lastSlice.add(x);
		}
		return slices;
	}

	/**
	 * Returns a pair (or rather, an object) containing the set differences
	 * between current and old, ie (added: current \ old, deleted: old \
	 * current)
	 * 
	 * @param current
	 *            Cannot be null
	 * @param old
	 *            Cannot be null
	 * @return
	 */
	public static <X> Changes<X> differences(Collection<X> current,
			Collection<X> old) {
		return new Changes<X>(setDifference(current, old), setDifference(old,
				current));
	}

	/**
	 * Filter out duplicates. Get the distinct/unique items from a collection.
	 * This is like running the collection through a Set, but it preserves the
	 * order.
	 * 
	 * @param items
	 * @return items (in the order they came) with any duplicates removed.
	 */
	public static <X> List<X> distinct(Collection<X> items) {
		ArrayList<X> distinct = new ArrayList<X>(items.size());
		HashSet<X> set = new HashSet<X>(items.size());
		for (X x : items) {
			if (set.contains(x)) {
				continue;
			}
			set.add(x);
			distinct.add(x);
		}
		return distinct;
	}

	/**
	 * Allows easy loop-and-modify :) E.g. <code><pre>
	 * for(X x : list) {
	 * 		if (isUgly(x)) list.remove(x);
	 * }
	 * </pre></code>
	 * 
	 * If you are just doing one loop, then you may as well use:
	 * <code>for(X x : new ArrayList&lt;X&gt;(list))</code> instead.
	 * <p>
	 * NB: This is done by wrapping the list, and providing iterators which loop
	 * over a copy of the list. So the iterator will ignore any modificatins
	 * that happen while it's looping. This also allows for looping without
	 * locks in the context of multi-threaded modifications (though be careful
	 * with that!).
	 * 
	 * @param listToModify
	 * @return listToModify'
	 */
	public static <X> List<X> easy(final List<X> listToModify) {
		if (Utils.yes(null)) { // backwards compatibility Dec 2012
		return new AbstractList<X>() {
			@Override
			public void add(int index, X element) {
				listToModify.add(index, element);
			}

			@Override
			public X get(int index) {
				return listToModify.get(index);
			}

			@Override
			public Iterator<X> iterator() {
				// copy to allow modification
				// FIXME replace the iterator with one that does remove from
				// listToModify
				return new ArrayList(listToModify).iterator();
			}

			@Override
			public ListIterator<X> listIterator(int index) {
				// copy to allow modification
				// FIXME replace the iterator with one that does remove from
				// listToModify
				return new ArrayList(listToModify).listIterator(index);
			};

			@Override
			public X remove(int i) {
				return listToModify.remove(i);
			};

			@Override
			public boolean remove(Object o) {
				return listToModify.remove(o);
			}

			@Override
			public X set(int index, X element) {
				return listToModify.set(index, element);
			}

			@Override
			public int size() {
				return listToModify.size();
			}
		};		
		} // end old code
		
		return new EasyList(listToModify);
	}

	/**
	 * Allows easy loop-and-modify :) E.g. <code><pre>
	 * for(X x : set) {
	 * 		if (isUgly(x)) set.remove(x);
	 * }
	 * </pre></code>
	 * 
	 * NB: This is done by wrapping the set, and providing iterators which loop
	 * over a copy of the set. So the iterator will ignore any modifications
	 * that happen while it's looping. This also allows for looping without
	 * locks in the context of multi-threaded modifications (though be careful
	 * with that!).
	 * 
	 * @param listToModify
	 * @return listToModify'
	 */
	public static <X> Set<X> easy(final Set<X> listToModify) {
		return new AbstractSet<X>() {
			@Override
			public boolean add(X element) {
				return listToModify.add(element);
			}

			@Override
			public Iterator<X> iterator() {
				// copy to allow modification
				return new ArrayList(listToModify).iterator();
			};

			@Override
			public boolean remove(Object o) {
				return listToModify.remove(o);
			}

			@Override
			public int size() {
				return listToModify.size();
			}
		};
	}

	/**
	 * Return the set of values in a collection (probably a list...)
	 * 
	 * @param data
	 * @return
	 */
	public static <X> Set<X> entrySet(Collection<X> data) {
		HashSet<X> values = new HashSet<X>();
		for (X datum : data) {
			values.add(datum);
		}
		return values;
	}

	/**
	 * @param filter Must not be null
	 * @param list Can be null (returns null)
	 * @return a new list, which contains those elements that the filter
	 *         accepts. Can contain null if the filter accepts it! TODO should
	 *         we always filter out null??
	 */
	public static <X, X2 extends X> List<X2> filter(IFilter<X> filter,
			Collection<X2> list) {
		if (list==null) return null;
		assert filter != null : list;
		ArrayList<X2> out = new ArrayList();
		for (X2 x : list) {
			if (filter.accept(x)) {
				out.add(x);
			}
		}
		return out;
	}

	/**
	 * Lazy filtering of a stream.
	 * @param filter
	 * @param stream
	 *            Cannot contain acceptable nulls
	 * @return a filtered version of stream
	 * @see #applyLazy(Iterable, IFn)
	 */
	public static <X, X2 extends X> Iterable<X2> filterLazy(
			IFilter<X> filter, Iterable<X2> stream) {
		return new FilterStream<X, X2>(filter, stream);
	}

	/**
	 * @param filter
	 * @param tree
	 * @return first node in tree which passes the filter
	 */
	public static <T extends Tree> T first(IFilter<T> filter, T tree) {
		List<T> nodes = tree.flatten();
		for (T tree2 : nodes) {
			if (filter.accept(tree2))
				return tree2;
		}
		return null;
	}

	/**
	 * Lenient access to the start of a collection
	 * 
	 * @param filter
	 *            can be null. If set, the method will pick the first element to
	 *            pass filter.accept().
	 * @param collection
	 *            can be null.
	 * @return the first element, if there is one, or null.
	 */
	public static <X> X first(IFilter<X> filter, Collection<X> collection) {
		if (collection == null || collection.isEmpty())
			return null;
		if (filter == null)
			return collection.iterator().next();
		for (X x : collection) {
			if (filter.accept(x))
				return x;
		}
		return null;
	}

	public static <X> X first(Iterable<X> collection)
			throws NoSuchElementException {
		return get(collection, 0);
	}

	/**
	 * WARNING: circular references will cause an infinite loop
	 * 
	 * @param <X>
	 * @param nestedCollections
	 * @return all the non-collection elements of nestedCollections
	 */
	public static <X> List<X> flatten(Collection<? extends X> nestedCollections) {
		ArrayList list = new ArrayList();
		flatten2(list, nestedCollections);
		return list;
	}

	static void flatten2(ArrayList list, Collection nestedCollections) {
		for (Object object : nestedCollections) {
			if (object instanceof Collection) {
				flatten2(list, (Collection) object);
			} else {
				list.add(object);
			}
		}
	}

	/**
	 * @param collection
	 * @param n
	 * @return the nth element in the collection.
	 */
	public static <X> X get(Iterable<X> collection, int n)
			throws NoSuchElementException {
		if (collection instanceof List)
			return ((List<X>) collection).get(n);
		Iterator<X> it = collection.iterator();
		for (int i = 0; i < n - 1; i++) {
			it.next();
		}
		return it.next();
	}

	/**
	 * @param klass
	 *            This covers sub-classes too
	 * @param list
	 *            Can be empty, must not be null
	 * @return The first object of the given class, or null
	 */
	public static <X> X getByClass(Class<X> klass, List<?> list) {
		for (Object object : list) {
			if (ReflectionUtils.isa(object.getClass(), klass))
				return (X) object;
		}
		return null;
	}

	/**
	 * Get a list version of an iterable - possibly copying it out to do so.
	 * 
	 * @param iterable
	 * @return A List version of the input. If the input is itself a List this
	 *         will return the input!
	 */
	public static <X> List<X> getList(Iterable<X> iterable) {
		// Why waste effort?
		if (iterable instanceof List)
			return (List<X>) iterable;
		// if (iterable instanceof Collection)
		// return new ArrayList<X>((Collection) iterable);
		ArrayList<X> list = iterable instanceof Collection ? new ArrayList<X>(
				((Collection) iterable).size()) : new ArrayList<X>();
		for (X x : iterable) {
			list.add(x);
		}
		return list;
	}

	public static Map<String, Object> getMap(IProperties props) {
		Collection<Key> keys = props.getKeys();
		Map<String, Object> map = new HashMap<String, Object>(keys.size());
		for (Key k : keys) {
			map.put(k.getName(), props.get(k));
		}
		return map;
	}

	/**
	 * Get a set version of an iterable - possibly copying it out to do so.
	 * 
	 * @param iterable
	 * @return A Set version of the input. If the input is itself a List this
	 *         will return the input! Otherwise you get a new HashSet for fast
	 *         access.
	 */
	public static <X> Set<X> getSet(Iterable<X> iterable) {
		// Why waste effort?
		if (iterable instanceof Set)
			return (Set<X>) iterable;
		if (iterable instanceof Collection)
			return new HashSet<X>((Collection) iterable);
		HashSet<X> list = new HashSet<X>();
		for (X x : iterable) {
			list.add(x);
		}
		return list;
	}


	/**
	 * Useful function for working with lists-of-pairs, or lists-of-lists. Use
	 * this with {@link #apply(IFn, Collection)} to do, e.g. given a list of
	 * pairs, get the first of each pair.
	 * 
	 * @param i
	 * @return a function which extracts the ith element from any iterable.
	 */
	public static IFn getter(final int i) {
		return new GetterFn(i);
	}



	/**
	 * Find an object in an array, using equals() for comparison. Faster methods
	 * exist for sorted arrays of known type:
	 * {@link Arrays#binarySearch(Object[], Object)} and friends.
	 * 
	 * @param x
	 * @param array
	 * @return index of x in array, or -1 if not present
	 */
	public static int indexOf(Object x, Object array) {
		assert array.getClass().isArray() : array + " is not an array";
		int n = Array.getLength(array);
		for (int i = 0; i < n; i++) {
			Object y = Array.get(array, i);
			if (x == null ? y == null : x.equals(y))
				return i;
		}
		return -1;
	}

	/**
	 * Find an object in an array, using equals() for comparison. Faster methods
	 * exist for sorted arrays of known type:
	 * {@link Arrays#binarySearch(Object[], Object)} and friends.
	 * 
	 * @param x
	 * @param array
	 * @return index of x in array, or -1 if not present
	 */
	public static int indexOf(String x, String[] array) {
		for (int i = 0; i < array.length; i++) {
			if (x == null ? array[i] == null : x.equals(array[i]))
				return i;
		}
		return -1;
	}
	
	/**
	 * Find an object in an array, using equals() for comparison. Faster methods
	 * exist for sorted arrays of known type:
	 * {@link Arrays#binarySearch(Object[], Object)} and friends.
	 * 
	 * @param x
	 * @param array
	 * @return index of x in array, or -1 if not present
	 */
	public static int indexOf(int x, int[] array) {
		for (int i = 0; i < array.length; i++) {
			if (x == array[i]) return i;
		}
		return -1;
	}


	/**
	 * @param a
	 * @param b
	 * @return a new set which is the intersection of a and b (i.e. contains
	 *         only those elements which were in both of them). The original
	 *         sets are unchanged. Can be empty, never null.
	 */
	public static <X> Set<X> intersection(Collection<? extends X> a,
			Collection<? extends X> b) {
		HashSet overlap = new HashSet(a);
		overlap.retainAll(b);
		return overlap;
	}

	/**
	 * @param a
	 * @param b
	 * @return true if a and b share at least one element
	 * @Deprecated use built in Collections.disjoint(a, b); Left here as an
	 *             aide-memoire.
	 */
	@Deprecated
	public static boolean intersects(Collection<?> a, Collection<?> b) {
		return !Collections.disjoint(a, b);
	}

	public static <X> Iterable<X> iterable(final Iterator<X> iterator) {
		// Backwards compatibility :(
		if (Utils.yes(null)) {
			return new Iterable<X>() {
				private boolean used;
	
				@Override
				public Iterator<X> iterator() {
					assert !used;
					used = true;
					return iterator;
				}
			};
		}
		
		return new IterableWrapper(iterator);
	}

	public static <X> X last(List<X> list) {
		return list.get(list.size() - 1);
	}

	/**
	 * Wrap a List as a Set. All edits will write through to the list. The add()
	 * method will check to avoid duplicates. NB This cannot be used to retrieve
	 * the set of list of entries for iteration see
	 * {@link Containers#entrySet(Collection)} for that. This should not be used
	 * for big lists as the add method iterates over the entire list...
	 */
	public static <X> Set<X> listAsSet(final List<X> list) {
		return new AbstractSet<X>() {
			@Override
			public boolean add(X x) {
				// no duplicates
				if (list.contains(x))
					return false;
				return list.add(x);
			}

			@Override
			public Iterator<X> iterator() {
				return list.iterator();
			}

			@Override
			public boolean remove(Object x) {
				return list.remove(x);
			}

			@Override
			public int size() {
				return list.size();
			}
		};
	}

	public static final <X> IFilter<X> Not(final IFilter<X> filter) {
		// Keep old class for backwards compatibility
		if (Utils.yes(null)) {
			return new IFilter<X>() {
				@Override
				public boolean accept(X x) {
					return !filter.accept(x);
				}
			};
		}
		
		return new Not(filter);
	}

	/**
	 * Treat an object like a map. One must question the sanity of this, but it
	 * can be useful.
	 */
	public static Map<String, Object> objectAsMap(Object x) {
		return new ObjectMap(x);
	}

	public static final <X> IFilter<X> Or(final IFilter<X> filterA, final IFilter<X> filterB) {
		// backwards compatibility :(
		if (Utils.yes(null)) {
			return new IFilter<X>() {
				@Override
				public boolean accept(X x) {
					return filterA.accept(x) || filterB.accept(x);
				}
			};
		}
		
		return new Or(filterA, filterB);
	}

	public static <X> double plus(IProperties obj, Key<Double> key, double dx) {
		Double x = obj.get(key);
		if (x == null) {
			x = 0.0;
		}
		x += dx;
		obj.put(key, x);
		return x;
	}

	public static <X> int plus(IProperties obj, Key<Integer> key, int dx) {
		Integer x = obj.get(key);
		if (x == null) {
			x = 0;
		}
		x += dx;
		obj.put(key, x);
		return x;
	}

	/**
	 * Thread safety: YES for ConcurrentMap, NO for other maps!
	 * 
	 * @param counts
	 * @param key
	 * @param dx
	 * @return new-value
	 */
	public static <X> double plus(Map<X, Double> counts, X key, double dx) {
		assert counts != null : key;
		assert key != null : counts;
		if (dx==0) {
			Double kv = counts.get(key);
			return kv==null? 0 : kv;
		}
		// thread safe?
		if (counts instanceof ConcurrentMap) {
			ConcurrentMap<X, Double> key2counts = (ConcurrentMap<X, Double>) counts;
			// lock free
			for (int i = 0; i < 10; i++) {
				Double v = key2counts.get(key);
				if (v == null) {
					Double prev = key2counts.putIfAbsent(key, dx);
					if (prev == null)
						return dx;
				} else {
					double y = v + dx;
					boolean ok = key2counts.replace(key, v, y);
					if (ok)
						return y;
				}
			}
			// oh well, use a lock...
		}
		// A synchronized map? (or a failed lock-free add on a concurrent map). 
		// Then you want thread-safety:
		if (counts instanceof ConcurrentMap || counts.getClass() == SynchronizedMapClass) {
			// TODO Use an EqualsLocker instead for more fine-grained locking??
			synchronized (counts) {
				Double x = counts.get(key);
				double y = x == null ? dx : x + dx;
				counts.put(key, y);
				return y;				
			}
		}
		// normal single-thread case
		Double x = counts.get(key);
		double y = x == null ? dx : x + dx;
		counts.put(key, y);
		return y;
	}
	
	/**
	 * The class Java uses for synchronized maps
	 */
	private static final Class<? extends Map> SynchronizedMapClass = Collections.synchronizedMap(Collections.EMPTY_MAP).getClass();

	/**
	 * 
	 * @param counts
	 * @param key
	 * @param dx
	 * @return the NEW value for key
	 */
	public static <X> int plus(Map<X, Integer> counts, X key, int dx) {
		Integer x = counts.get(key);
		if (x == null) {
			x = 0;
		}
		x += dx;
		counts.put(key, x);
		return x;
	}

	/**
	 * Replace all instances that are equals to old with replacement.
	 * 
	 * @param list
	 *            This is modified in place
	 * @param old
	 *            Can be null
	 * @param replacement
	 *            Can be null
	 * @return count of the number of edits
	 */
	public static <X> int replace(List<X> list, X old, X replacement) {
		int cnt = 0;
		for (int i = 0, n = list.size(); i < n; i++) {
			if (Utils.equals(list.get(i), old)) {
				list.set(i, replacement);
				cnt++;
			}
		}
		return cnt;
	}

	/**
	 * Convenience method, mainly for use with asserts. More
	 * reliable/straightforward for comparing Collections than equals()
	 * 
	 * @param a
	 * @param b
	 * @return true if a and b have the same members. Ignores ordering and any
	 *         duplicates.
	 */
	public static boolean same(Collection a, Collection b) {
		if (a==null) return b==null;
		if (b==null) return false;
		return differences(a, b).isEmpty();
	}

	public static boolean same(Collection a, Object... b) {
		return differences(a, Arrays.asList(b)).isEmpty();
	}

	/**
	 * Set difference, current \ old.
	 * 
	 * @param current
	 *            Cannot be null
	 * @param old
	 *            Cannot be null
	 * @return An ArrayList of differences ignoring order.
	 */
	public static <X> List<X> setDifference(Collection<X> current,
			Collection<X> old) {
		assert current != null && old != null;
		ArrayList<X> added = new ArrayList<X>();
		Set<X> oldSet = getSet(old);
		for (X currentX : current) {
			if (!old.contains(currentX)) { // X was not there before
				added.add(currentX);
			}
		}
		return added;
	}

	/**
	 * Quick if iterable is actually a Collection. Otherwise we need to run
	 * through the iterable to count its elements. So be careful with this.
	 * 
	 * @param iterable Can be null (returns 0)
	 * @return the size of this iterable
	 */
	public static int size(Iterable iterable) {
		if (iterable == null) return 0;
		if (iterable instanceof Collection)
			return ((Collection) iterable).size();
		int cnt = 0;
		int max = Integer.MAX_VALUE;
		for (Object x : iterable) {
			cnt++;
			if (cnt == max)
				throw new IllegalArgumentException(iterable + " is too big");
		}
		return cnt;
	}

	/**
	 * A more flexible (and dangerous) version of subList.
	 * <p>
	 * Unlike List.subList(), this will NOT share structure with the original
	 * list.
	 * 
	 * @param list
	 *            Can be null in which case null is returned.
	 * 
	 * @param start
	 *            Inclusive. Can be negative for distance from the end. E.g. -1
	 *            indicates "all but the last character" (zero indicates
	 *            "from the beginning"). Can be longer than the actual list, in
	 *            which case an empty list is returned.
	 * @param end
	 *            Exclusive. Can be negative for distance from the end. E.g. -1
	 *            indicates "all but the last character" (zero indicates
	 *            "up to the end"). Can be longer than the actual list, in which
	 *            case it is reduced. If end is negative and too large, an empty
	 *            list will be returned.
	 * 
	 * @return The sublist. Can be empty. Only ever null if the list is null.
	 *         Can be safely edited without affecting the original list.
	 * 
	 * 
	 * @testedby {@link ContainersTest#testSubList()}
	 */
	public static <X> List<X> subList(List<X> list, int start, int end) {
		if (list == null)
			return null;
		// start from end?
		if (start < 0) {
			start = list.size() + start;
			if (start < 0) {
				start = 0;
			}
		}
		// from end?
		if (end <= 0) {
			end = list.size() + end;
			if (end < start)
				return new ArrayList(0);
		}
		// too long?
		if (end > list.size()) {
			end = list.size();
		}
		// OK (copy to avoid shared structure)
		if (start == 0 && end == list.size())
			return new ArrayList(list);
		return new ArrayList(list.subList(start, end));
	}

	/**
	 * A more flexible (and dangerous) version of subList.
	 * 
	 * @param list
	 *            Not null
	 * 
	 * @param start
	 *            Inclusive. Can be negative for distance from the end. E.g. -1
	 *            indicates "all but the last character" (zero indicates
	 *            "from the beginning"). Can be longer than the actual list, in
	 *            which case an empty list is returned.
	 * @param end
	 *            Exclusive. Can be negative for distance from the end. E.g. -1
	 *            indicates "all but the last character" (zero indicates
	 *            "up to the end"). Can be longer than the actual list, in which
	 *            case it is reduced. If end is negative and too large, an empty
	 *            list will be returned.
	 * 
	 * @return A copied out subarray. Can be empty.
	 * 
	 * @testedby {@link ContainersTest#testSubList()}
	 */
	public static long[] subList(long[] list, int start, int end) {
		// start from end?
		if (start < 0) {
			start = list.length + start;
			if (start < 0) {
				start = 0;
			}
		}
		// from end?
		if (end <= 0) {
			end = list.length + end;
			if (end < start)
				return new long[0];
		}
		// too long?
		if (end > list.length) {
			end = list.length;
		}
		// OK
		// if (start == 0 && end == list.length)
		// return list;
		return Arrays.copyOfRange(list, start, end);
	}

	/**
	 * Turn a NxM list of lists into MxN. Must all be same length, or
	 * IllegalArgumentException will be thrown (this is checked early with
	 * size() before doing the transpose)
	 */
	public static <A> List<List<A>> transpose(List<List<A>> in) {
		if (in.isEmpty())
			return new ArrayList<List<A>>();
		Integer size = in.get(0).size();
		if (size == 0)
			throw new IllegalArgumentException(
					"Input lists must not be of size zero -- the transpose representation is impossible");
		for (List<A> in_row : in) {
			if (in_row.size() != size)
				throw new IllegalArgumentException(
						"Input lists must all be of same size");
		}
		List<List<A>> out = new ArrayList<List<A>>();
		for (Integer idx = 0; idx < size; idx++) {
			List<A> out_row = new ArrayList<A>(in.size());
			for (Integer jdx = 0; jdx < in.size(); jdx++) {
				out_row.add(in.get(jdx).get(idx));
			}
			out.add(out_row);
		}
		return out;
	}

	protected Containers() {
		// static
	}

	/**
	 * HashMap is a lovely class -- but put it not thread safe, not even with a synchronized block (it can lead to 
	 * the thread hanging if a get happens at the same time).
	 *  
	 * @param map Can be null (a new empty map is made) 
	 * @param key
	 * @param value
	 * @return a copy of map, with the new key=value setting
	 */
	public static <K,V> HashMap<K,V> copyPut(Map<K,V> map, K key, V value) {
		HashMap copy;
		if (map==null) {
			copy = new HashMap();
		} else {
			// still some thread safety issues??
			synchronized (map) {
				copy = new HashMap(map);	
			}
		}
		copy.put(key, value);
		return copy;
	}

}

/**
 * Lazy filtering of a stream
 * @author daniel
 *
 * @param <X>
 * @param <X2>
 */
final class FilterStream<X, X2 extends X> implements Iterable<X2> {

	private final IFilter<X> filter;
	private final Iterable<X2> stream;

	FilterStream(IFilter<X> filter, Iterable<X2> stream) {
		this.stream = stream;
		this.filter = filter;
	}

	@Override
	public Iterator<X2> iterator() {
		final Iterator<X2> it = stream.iterator();
		return new AbstractIterator<X2>() {
			@Override
			protected X2 next2() throws Exception {
				while (it.hasNext()) {
					X2 nxt = it.next();
					if ( ! filter.accept(nxt))
						continue;
					if (nxt == null)
						throw new TodoException("null in " + filter + " "
								+ stream);
					return nxt;
				}
				return null;
			}
		};
	}

}

/**
 * Treat the fields of an object like a map. One must question the sanity of
 * this, but it can be useful.
 * 
 * Skips static and transient fields.
 * @author daniel
 * 
 */
final class ObjectMap extends AbstractMap2<String, Object> {

	private final Class klass;
	private final Object object;

	public ObjectMap(Object x) {
		this.object = x;
		klass = x.getClass();
	}

	@Override
	public Object get(Object key) {
		// TODO search down for fields in super-classes
		String k = (String) key;
		try {
			Field f = klass.getField(k);
			return f.get(object);
		} catch (NoSuchFieldException ex) {
			try {
				Field f = klass.getDeclaredField(k);
				f.setAccessible(true);
				return f.get(object);
			} catch (NoSuchFieldException e) {
				return null;
			} catch (Exception e) {
				throw Utils.runtime(e);
			}
		} catch (Exception e) {
			throw Utils.runtime(e);
		}
	}

	@Override
	public Set<String> keySet() throws UnsupportedOperationException {
		Field[] fields = klass.getFields();
		Set<String> keys = new HashSet<String>(fields.length);
		for (Field field : fields) {
			int mods = field.getModifiers();
			if (Modifier.isTransient(mods)) continue;
			if (Modifier.isStatic(mods)) continue;
			keys.add(field.getName());
		}
		return keys;
	}

	@Override
	public Object put(String key, Object value) {
		Object prev = get(key);
		ReflectionUtils.setPrivateField(object, key, value);
		return prev;
	}
}

/**
 * Lazily apply a function to an iterable.
 * 
 * @author daniel
 * 
 * @param <I>
 * @param <O>
 */
final class IterableFn<I, O> implements Iterable<O> {

	private final Iterable<? extends I> base;
	private final IFn<I, O> fn;

	public IterableFn(Iterable<? extends I> list, IFn<I, O> fn) {
		this.base = list;
		this.fn = fn;
	}

	@Override
	public Iterator<O> iterator() {
		final Iterator<? extends I> bit = base.iterator();
		return new Iterator<O>() {
			@Override
			public boolean hasNext() {
				return bit.hasNext();
			}

			@Override
			public O next() {
				try {
					return fn.apply(bit.next());
				} catch (Exception e) {
					throw Utils.runtime(e);
				}
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}
}


final class Not<X> implements IFilter<X> {
	private final IFilter<X> base;
	public Not(IFilter<X> filter) {
		this.base = filter;
	}

	@Override
	public boolean accept(X x) {
		return ! base.accept(x);
	}
	@Override
	public String toString() {
		return "NOT "+base;
	}
}


final class And<X> implements IFilter<X> {
	private final IFilter<X> a;
	private final IFilter<X> b;
	public And(IFilter<X> filterA, IFilter<X> filterB) {
		assert filterA != null && filterB!=null : filterA+" "+filterB;
		this.a = filterA; this.b = filterB;
	}

	@Override
	public boolean accept(X x) {
		return a.accept(x) && b.accept(x);
	}
	@Override
	public String toString() {
		return a+" AND "+b;
	}
}


final class Or<X> implements IFilter<X> {
	private final IFilter<X> a;
	private final IFilter<X> b;
	public Or(IFilter<X> filterA, IFilter<X> filterB) {
		a = filterA; b = filterB;
	}

	@Override
	public boolean accept(X x) {
		return a.accept(x) || b.accept(x);
	}
	@Override
	public String toString() {
		return a+" OR "+b;
	}
}

final class IterableWrapper<X> implements Iterable<X> {
	private boolean used;
	private final Iterator<X> iterator;

	public IterableWrapper(Iterator<X> iterator) {
		this.iterator = iterator;
	}

	@Override
	public Iterator<X> iterator() {
		assert !used;
		used = true;
		return iterator;
	}
}


final class EasyList<X> extends AbstractList<X> {
	private final List<X> listToModify;

	public EasyList(List<X> listToModify) {
		this.listToModify = listToModify;
	}
	
	@Override
	public void add(int index, X element) {
		listToModify.add(index, element);
	}

	@Override
	public X get(int index) {
		return listToModify.get(index);
	}

	@Override
	public Iterator<X> iterator() {
		// copy to allow modification
		// FIXME replace the iterator with one that does remove from
		// listToModify
		return new ArrayList(listToModify).iterator();
	}

	@Override
	public ListIterator<X> listIterator(int index) {
		// copy to allow modification
		// FIXME replace the iterator with one that does remove from
		// listToModify
		return new ArrayList(listToModify).listIterator(index);
	};

	@Override
	public X remove(int i) {
		return listToModify.remove(i);
	};

	@Override
	public boolean remove(Object o) {
		return listToModify.remove(o);
	}

	@Override
	public X set(int index, X element) {
		return listToModify.set(index, element);
	}

	@Override
	public int size() {
		return listToModify.size();
	}
}


/**
 * Takes Arrays or Lists or any iterable
 * @author daniel
 *
 */
final class GetterFn implements IFn<Object, Object> {
	final int i;
	
	public GetterFn(int i) {
		this.i = i;
	}

	@Override
	public Object apply(Object value) {
		Iterable it;
		if (value.getClass().isArray()) {
			it = Containers.asList(value);
		} else {
			it = (Iterable) value;
		}
		if (it instanceof List) {
			return ((List) it).get(i);
		}
		int n = 0;
		for (Object object : it) {
			if (n == i)
				return object;
			n++;
		}
		throw new IndexOutOfBoundsException(i + " in " + value);
	}
}


final class BooleanArray extends AbstractList<Boolean> {
	private final boolean[] xs;

	public BooleanArray(boolean[] xs) {
		this.xs = xs;
	}

	@Override
	public Boolean get(int index) {
		return xs[index];
	}

	@Override
	public Boolean set(int index, Boolean element) {
		boolean old = xs[index];
		xs[index] = element;
		return old;
	}

	@Override
	public int size() {
		return xs.length;
	}
}