package com.winterwell.utils.containers;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
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
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.winterwell.utils.BestOne;
import com.winterwell.utils.IFilter;
import com.winterwell.utils.IFn;
import com.winterwell.utils.IProperties;
import com.winterwell.utils.Key;
import com.winterwell.utils.MathUtils;
import com.winterwell.utils.NotUniqueException;
import com.winterwell.utils.Printer;
import com.winterwell.utils.ReflectionUtils;
import com.winterwell.utils.StrUtils;
import com.winterwell.utils.TodoException;
import com.winterwell.utils.Utils;
import com.winterwell.utils.log.Log;



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


final class DoubleArray extends AbstractList<Double> {
	private final double[] xs;

	public DoubleArray(double[] xs) {
		this.xs = xs;
	}

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
}


final class FloatArray extends AbstractList<Float> {
	private final float[] xs;

	public FloatArray(float[] xs) {
		this.xs = xs;
	}

	@Override
	public Float get(int index) {
		return xs[index];
	}

	@Override
	public Float set(int index, Float element) {
		float old = xs[index];
		xs[index] = element;
		return old;
	}

	@Override
	public int size() {
		return xs.length;
	}
}


final class CharArray extends AbstractList<Character> {
	private final char[] xs;

	public CharArray(char[] xs) {
		this.xs = xs;
	}

	@Override
	public Character get(int index) {
		return xs[index];
	}

	@Override
	public Character set(int index, Character element) {
		char old = xs[index];
		xs[index] = element;
		return old;
	}

	@Override
	public int size() {
		return xs.length;
	}
}

final class CompareAny implements Comparator {
	@Override
	public int compare(Object o1, Object o2) {
		return Containers.compare(o1, o2);
	}
}



/**
 * Helpful utility methods for working with Collections and arrays.
 * 
 * @author daniel
 * 
 */
public final class Containers  {

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

	public static final IProperties EMPTY_PROPERTY_BAG = Utils.yes(true)? new EmptyBag() : new IProperties() {
		@Override
		public <T> boolean containsKey(Key<T> key) {
			return false;
		}

		@Override
		public <T> T get(Key<T> key) {
			return null;
		}

		@Override
		public Collection<Key> getKeys() {
			return Collections.emptySet();
		}

		@Override
		public boolean isTrue(Key<Boolean> key) {
			return false;
		}

		@Override
		public <T> T put(Key<T> key, T value) {
			throw new UnsupportedOperationException();
		}
	};

	/**
	 * The class Java uses for synchronized maps
	 */
	private static final Class<? extends Map> SynchronizedMapClass = Collections.synchronizedMap(Collections.EMPTY_MAP).getClass();

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
	 * <p>
	 * This is equivalent to <code>list.stream().map(fn).collect(Collectors.toList())</code> 
	 * -- but less typing, easier to debug, and faster!
	 * 
	 * @param list Cannot be null
	 * @param fn
	 * 
	 * @return [fn applied to each member of list] 
	 * This is a fresh ArrayList, and can be modified afterwards.
	 */
	public static <I, O> List<O> apply(Iterable<? extends I> list, IFn<I, O> fn) {
		ArrayList after = list instanceof Collection? new ArrayList(((Collection) list).size()) : new ArrayList();
		try {
			for (I object : list) {
				O o = fn.apply(object);
				after.add(o);			
			}
		} catch(Exception ex) {
			throw Utils.runtime(ex);
		}
		return after;
	}
	
	/**
	 * Apply (map) fn to list
	 * @param list Must not be null
	 * @param fn
	 * @return
	 */
	public static <I, O> List<O> apply(I[] list, IFn<I, O> fn) {
		ArrayList after = new ArrayList(list.length);
		try {
			for (I object : list) {
				O o = fn.apply(object);
				after.add(o);			
			}
		} catch(Exception ex) {
			throw Utils.runtime(ex);
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
	 * @see #filterLazy(Iterable, IFilter)
	 */
	public static <I, O> Iterable<O> applyLazy(Iterable<? extends I> list,
			IFn<I, O> fn) {
		return new IterableFn<I, O>(list, fn);
	}

	/**
	 * Convenience for {@link #applyToMap(Map, java.util.function.BiFunction)}.
	 * Produces a new Map of key/values by mapping each entry value through a
	 * transformation function.
	 * 
	 * @param fn Can return null, which will remove that entry.
	 * @param map
	 * @return {key: fn applied to each value}
	 */
	public static <K, I, O> Map<K,O> applyToValues(final IFn<I, O> fn, Map<? extends K, ? extends I> map) {
		BiFunction<K,I,O> kvfn = (k, v) -> fn.apply(v);
		return applyToMap(map, kvfn);
	}

	/**
	 * Apply a function to change map *values* (and filter nulls)
	 * @param map
	 * @param fn
	 * @return {key: fn applied to each value}
	 */
	public static <K, I, O> Map<K,O> applyToMap(Map<? extends K, ? extends I> map, java.util.function.BiFunction<K, I, O> fn) {
		// HashMap is a good choice - but keep ArrayMap (with ordering) if that was the input
		Map after = map instanceof ArrayMap? new ArrayMap(map.size()) : new HashMap(map.size());
		for (K k : map.keySet()) {
			try {
				I v = map.get(k);				
				O o = fn.apply(k, v);
				if (o==null) continue;
				after.put(k, o);
			} catch(Exception ex) {
				throw Utils.runtime(ex);
			}
		}
		return after;
	}


	/**
	 * Walk the tree, creating a new tree
	 * 
	 * @param fn (old-value, path) -> new-value. Apply this to each node (leaf and branch). If it returns null, remove the node.
	 * 
	 */
	public static Map<String, Object> applyToJsonObject(
			Map<String, Object> jsonObject, BiFunction<Object, List<String>, Object> fn4valuePath) {
		List<String> path = new ArrayList<>();
		return applyToJsonObject2_map(path, jsonObject, fn4valuePath);
	}
	
	public static List applyToJsonObject(
			List jsonArray, BiFunction<Object, List<String>, Object> fn4PathValue) {
		List<String> path = new ArrayList<>();
		return applyToJsonObject2_list(path, jsonArray, fn4PathValue);
	}

	static Map<String, Object> applyToJsonObject2_map(
			List<String> path, Map<String, Object> jsonObject, BiFunction<Object, List<String>, Object> fn4valuePath) 
	{
		final ArrayMap<String,Object> copyMap = new ArrayMap();
		for (Entry<String, Object> e : jsonObject.entrySet()) {
			String k = e.getKey();
			path.add(k);
			Object inputv = e.getValue();
			// standardise on List over array
			if (Containers.isArray(inputv)) {
				inputv = Containers.asList(inputv);
			}
			// apply!
			Object outputv = fn4valuePath.apply(inputv,path);
			if (outputv==null) {
				// prune
				path.remove(path.size()-1);
				continue;
			}
			// standardise on List over array
			if (Containers.isArray(outputv)) {
				outputv = Containers.asList(outputv);
			}
			// recurse?
			if (outputv instanceof Map) {
				outputv = applyToJsonObject2_map(path, (Map)outputv, fn4valuePath);
			} else if (outputv instanceof List) {
				outputv = applyToJsonObject2_list(path, (List)outputv, fn4valuePath);
			} else {
				// no recurse
			}
			copyMap.put(k, outputv);
			path.remove(path.size()-1);
		}
		return copyMap;
	}


	/**
	 * Convenience for Class.isArray()
	 * @param inputv Can be null
	 * @return true if not null and an array
	 */
	public static final boolean isArray(Object inputv) {
		if (inputv==null) return false;
		return inputv.getClass().isArray();
	}


	static List applyToJsonObject2_list(
			List<String> path, List jsonArray, BiFunction<Object, List<String>, Object> fn4valuePath) 
	{
		final List copyList = new ArrayList();
		for(int i=0, n=jsonArray.size(); i<n; i++) {
			Object inputv = jsonArray.get(i);
			// standardise on List over array
			if (Containers.isArray(inputv)) {
				inputv = Containers.asList(inputv);
			}
			path.add(Integer.toString(i));	
			Object outputv = fn4valuePath.apply(inputv, path);
			if (outputv==null) {
				path.remove(path.size()-1);
				continue;
			}				
			// standardise on List over array
			if (Containers.isArray(outputv)) {
				outputv = Containers.asList(outputv);
			}
			if (outputv instanceof Map) {
				outputv = applyToJsonObject2_map(path, (Map)outputv, fn4valuePath);
			} else if (outputv instanceof List) {
				outputv = applyToJsonObject2_list(path, (List)outputv, fn4valuePath);
			} else {
				// no recurse
			}
			copyList.add(outputv);
			path.remove(path.size()-1);
		}
		return copyList;
	}


	/**
	 * Produces a new Map of key/values by mapping each key through a
	 * transformation function.
	 * 
	 * @param fn Can return null, which will remove that entry.
	 */
	public static <K, V, KO> Map<KO, V> applyToKeys(
			Map<? extends K, V> map, IFn<K, KO> fn) 
	{
		HashMap after = new HashMap(map.size());
		for (Entry<? extends K, V> e : map.entrySet()) {
			try {
				K k = e.getKey();
				KO o = fn.apply(k);
				if (o==null) continue;
				after.put(o, e.getValue());
			} catch(Exception ex) {
				throw Utils.runtime(ex);
			}
		}
		return after;		
	}

	public static List<Boolean> asList(final boolean[] xs) {
		return new BooleanArray(xs);	
	}
	public static List<Character> asList(final char[] xs) {
		return new CharArray(xs);	
	}
	
	public static List<Double> asList(final double[] xs) {
		return new DoubleArray(xs);
	}
	public static List<Float> asList(final float[] xs) {
		return new FloatArray(xs);
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
	
	/**
	 * Wrapper for {@link Arrays#asList(Object...)} which can handle arrays of primitives,
	 * e.g. int[] or double[]. 
	 * It can also handle Lists, so if you're not sure whether you've got a List or array
	 *  -- eg output from a json parser -- this is your method.
	 * Throws an IllegalArgumentException if 'array' is not an array or a List or Collection.
	 * 
	 * @param array array or List or Set or null (returns null)
	 * @return List
	 */
	public static <X> List<X> asList(final Object array) {		
		if (array==null) return null;
		if (array instanceof List) return (List) array;
		// NB: Iterable includes all of Collection eg Set 
		if (array instanceof Iterable) {
			return getList((Iterable)array);
		}
		if ( ! array.getClass().isArray())
			throw new IllegalArgumentException("Backing object is not an array: "+array.getClass());
		// the primitive types
		if (array instanceof int[]) {
			return (List) asList((int[])array);
		}
		if (array instanceof double[]) {
			return (List) asList((double[])array);
		}
		if (array instanceof float[]) {
			return (List) asList((float[])array);
		}
		if (array instanceof long[]) {
			return (List) asList((long[])array);
		}
		if (array instanceof boolean[]) {
			return (List) asList((boolean[])array);
		}
		if (array instanceof char[]) {
			return (List) asList((char[])array);
		}
		return (List) Arrays.asList((Object[])array);
	}

	/**
	 * Convert this ghost of Java past to a nice modern List
	 * @param fossilList
	 * @return rejuvenated copy of fossilList
	 */
	public static <X> List<X> asList(final Enumeration fossilList) {
		ArrayList list2 = new ArrayList();
		while(fossilList.hasMoreElements()) {
			Object ne = fossilList.nextElement();
			list2.add(ne);
		}
		return list2;
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
	 * @testedby  ContainersTest#testChop()}
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
	 * Compare any objects. Uses {@link #compare(Object, Object)}.
	 */
	public static Comparator comparator() {
		return Utils.yes(true)? new CompareAny() : new Comparator() {
			@Override
			public int compare(Object o1, Object o2) {
				return Containers.compare(o1, o2);
			}
		};
	}

	/**
	 * Compare any two objects. Falls back to hashcodes if need be. null counts
	 * as large (so they will be sorted to the end in a normal sort)
	 * 
	 * @param a
	 * @param b
	 * @return 1 if a>b, -1 if b<a, 0 if a equals b.
	 * @throws ClassCastException
	 *             if a is Comparable but objects to b
	 */
	public static int compare(Object a, Object b) {
		if (a == b)
			return 0;
		if (a == null)
			return 1;
		if (b == null)
			return -1;
		if (a instanceof Comparable && b instanceof Comparable)
			return ((Comparable) a).compareTo(b);
		if (a.equals(b))
			return 0;
		// Arbitrary
		int ahash = a.hashCode();
		int bhash = b.hashCode();
		if (ahash == bhash) {
			Log.d("containers", "compare(): hashCode() not unique for "
					+ a + ", " + b + "; sorting may go awry");
			return 0;
		}
		return ahash < bhash ? -1 : 1;
	}

	/**
	 * @param small
	 *            Can be empty but never null
	 * @param large
	 *            Can be empty but never null
	 * @return true if small is contained within large, false otherwise
	 */
	public static boolean contains(Collection small, Collection large) {
		for (Object object : small) {
			if (!large.contains(object))
				return false;
		}
		return true;
	}
	/**
	 * Convenience when working with arrays.
	 * 
	 * @param s
	 *            Can be null
	 * @param strings Cannot be null
	 * @return true if s (or an equals() String) is in strings
	 */
	public static <T> boolean contains(T s, T[] strings) {
		for (T string : strings) {
			if (s == null ? string == null : s.equals(string))
				return true;
		}
		return false;
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
	 * @param list Can be null (returns null)
	 * @param filter Must not be null
	 * @return a new list, which contains those elements that the filter
	 *         accepts. Can contain null if the filter accepts it! TODO should
	 *         we always filter out null??
	 */
	public static <X, X2 extends X> List<X2> filter(Iterable<X2> list,
			IFilter<X> filter) {
		if (list==null) return null;
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
	 * @param stream
	 *            Cannot contain acceptable nulls
	 * @param filter
	 * @return a filtered version of stream
	 * @see #applyLazy(Iterable, IFn)
	 */
	public static <X, X2 extends X> Iterable<X2> filterLazy(
			Iterable<X2> stream, IFilter<X> filter) {
		return new FilterStream<X, X2>(filter, stream);
	}

	/**
	 * Identical to {@link #removeNulls(Iterable)}! This is just an aide-memoire.
	 * @param list
	 * @return list without any nulls
	 */
	public static <X> List<X> filterNulls(Iterable<X> list) {
		List<X> list2 = new ArrayList();
		for (X x : list) {
			if (x!=null) list2.add(x);
		}
		return list2;
	}

	/**
	 * @param tree
	 * @param filter
	 * @return first node in tree which passes the filter
	 */
	public static <T extends Tree> T first(T tree, IFilter<T> filter) {
		List<T> nodes = tree.flatten();
		for (T tree2 : nodes) {
			if (filter.accept(tree2))
				return tree2;
		}
		return null;
	}

	/**
	 * Lenient access to the start of a collection
	 * @param collection
	 *            can be null.
	 * @param filter
	 *            can be null. If set, the method will pick the first element to
	 *            pass filter.accept().
	 * 
	 * @return the first element, if there is one, or null.
	 */
	public static <X> X first(Iterable<X> collection, IFilter<X> filter) {
		if (collection == null) return null;
		if (filter == null) {
			return first(collection);
		}
		for (X x : collection) {
			if (filter.accept(x))
				return x;
		}
		return null;
	}

	/**
	 * @param collection
	 * @return first element or null
	 * @see #only(Iterable)
	 */
	public static <X> X first(Iterable<X> collection) {
		if (collection==null) return null;
		return get(collection, 0);
	}

	/**
	 * WARNING: circular references will cause an infinite loop
	 * 
	 * @param <X>
	 * @param nestedCollections
	 * @return all the non-collection elements of nestedCollections
	 */
	public static <X> List<X> flatten(Collection nestedCollections) {
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
	 * Convenience for "get if collection is long enough"
	 * @param collection
	 * @param n
	 * @return the nth element in the collection, or null
	 */
	public static <X> X get(Iterable<X> collection, int n) {
		if (collection instanceof List) {
			List<X> list = (List<X>) collection;
			if (list.size() <= n) return null;
			return list.get(n);
		}
		Iterator<X> it = collection.iterator();
		for (int i = 0; i < n - 1; i++) {
			if ( ! it.hasNext()) return null;
			it.next();
		}
		return it.next();
	}

	/**
	 * Convenience for "get if array is long enough"
	 * @param row
	 * @param i
	 * @return row[i] or null if i is too high
	 */
	public static Object get(Object[] row, int i) {
		if (i>= row.length) return null;
		return row[i];
	}

	/**
	 * Convenience for "get if array is long enough"
	 * @param row
	 * @param i
	 * @return row[i] or null if i is too high
	 */
	public static String get(String[] row, int i) {
		if (i>= row.length) return null;
		return row[i];
	}

	/**
	 * @param list
	 *            Can be empty, must not be null
	 * @param klass
	 *            This covers sub-classes too
	 * @return The first object of the given class, or null
	 */
	public static <X> X firstClass(List<?> list, Class<X> klass) {
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

	/**
	 * 
	 * @param props
	 * @return a fresh (shallow-copy) map
	 */
	public static Map<String, Object> getMap(IProperties props) {
		Collection<Key> keys = props.getKeys();
		Map<String, Object> map = new HashMap<String, Object>(keys.size());
		for (Key k : keys) {
			Object v = props.get(k);
			if (v==null) continue;
			map.put(k.getName(), v);
		}
		return map;
	}

	/**
	 * @param map
	 * @param key
	 * @param factory If map.key is unset, call this to make a value. 
	 * @return map.key
	 */
	public static <K,V> V getput(Map<K,V> map, K key, Callable<V> factory) {
		V v = map.get(key);
		if (v!=null) return v;
		// Set the default
		try {
			v = factory.call();
		} catch (Exception e) {
			throw Utils.runtime(e);
		}
		map.put(key, v);		
		return v;
	}

	/**
	 * @param map
	 * @param key
	 * @param factory If map.key is unset, call this to make a value. 
	 * @return map.key
	 */
	public static <K,V> V getput(Map<K,V> map, K key, IFn<K, V> factory) {
		V v = map.get(key);
		if (v!=null) return v;
		// Set the default
		try {
			v = factory.apply(key);
		} catch (Exception e) {
			throw Utils.runtime(e);
		}
		map.put(key, v);		
		return v;
	}

	/**
	 * Also known as get-create.
	 * @param map
	 * @param key
	 * @param dflt If map.key is unset, set it as this object. 
	 * Warning: beware of shared structure - use a fresh dflt object, don't reuse a shared one object.
	 * @return map.key
	 */
	public static <K,V> V getput(Map<K,V> map, K key, V dflt) {
		V v = map.get(key);
		if (v!=null) return v;
		// Set the default
		map.put(key, dflt);		
		return dflt;
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
	 * Get the keys to a map, sorted by their values.
	 * 
	 * @NotThreadSafe: If another thread modifies the values, then this can throw an error.
	 * 
	 * @param map
	 * @return The keys of map, sorted by value. Lowest first. E.g. {carol:3,
	 *         alice:1, bob:2} would result in [alice, bob, carol]
	 */
	public static <K, V> List<K> getSortedKeys(final Map<K, V> map) {
		List<K> keys = new ArrayList<K>(map.keySet());
		Collections.sort(keys, getValueComparator(map));
		return keys;
	}

	/**
	 * Useful function for working with lists-of-pairs, or lists-of-lists. Use
	 * this with {@link #apply(Collection, IFn)} to do, e.g. given a list of
	 * pairs, get the first of each pair.
	 * 
	 * @param i
	 * @return a function which extracts the ith element from any iterable.
	 */
	public static IFn getter(final int i) {
		return new GetterFn(i);
	}

	private static <K, V> Comparator<K> getValueComparator(final Map<K, V> map) {
		return new ValueComparator<K>(map);
	}

	/**
	 * Copy of map whose keySet() method returns keys in order sorted by value,
	 * smallest(true)/largest(false) first.
	 * 
	 * @param map
	 * @param maxNum
	 *            -1 for all
	 * @return an ArrayMap with keys, sorted by value
	 * @NotThreadSafe
	 * @testedby  ContainersTest#testGetValueSortedMap()} Note: builds on
	 *           {@link #getSortedKeys(Map)}
	 */
	public static <K, V> Map<K, V> getValueSortedMap(final Map<K, V> map,
			final boolean smallestFirst, final int maxNum) 
	{
		// pick keys
		List<K> keys = getSortedKeys(map);
		// sub set
		if ( ! smallestFirst) {
			Collections.reverse(keys);
		}
		if (maxNum != -1) {
			keys = subList(keys, 0, maxNum);
		}
		// copy -- use an arraymap to preserve order
		Map sorted = new ArrayMap();
		for (K k : keys) {
			sorted.put(k, map.get(k));
		}
		return sorted;
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
	
	/**
	 * 
	 * @param list Can be null (returns null)
	 * @return last element or null if empty
	 */
	public static <X> X last(List<X> list) {
		if (list==null || list.isEmpty()) return null;
		return list.get(list.size() - 1);
	}


	/**
	 * Like Python's len(). Based on {@link #size(Iterable)}.
	 * @param thingy Iterable or Map
	 */
	public static int len(Object thingy) {
		if (thingy instanceof Map) {
			return ((Map) thingy).size();
		}
		return size((Iterable) thingy);
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
	 * key-wise add all of b to a.
	 * That is, a[key] += b[key] for each key in b.
	 * @param a
	 * @param b
	 */
	public static <X> void plus(Map<X,Double> a, Map<X, ? extends Number> b) {
		for(Map.Entry<X,? extends Number> me : b.entrySet()) {
			plus(a, me.getKey(), me.getValue().doubleValue());
		}		
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
	 * @deprecated replaced by {@link #filterNulls(Iterable)}
	 * @param list
	 * @return list without any null items
	 */
	public static <X> List<X> removeNulls(Iterable<X> list) {
//		return filter(IFilter<X>.NOT_NULL, list);
		return filterNulls(list);
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
	
	/**
	 * Same elements, ignoring order. 
	 * Optimised for small arrays -- this is O(n2). For large arrays, route via HashSet or similar.
	 * @param a
	 * @param b
	 * @return true if a and b have equals elements.
	 */
	public static <T> boolean same(T[] a, T[] b) {
		if (a==null) return b==null;
		if (b==null) return false;
		if (a.length != b.length) return false;
		for (T x : a) {
			if ( ! contains(x, b)) return false;
		}
		return true;
	}


	public static boolean same(Collection a, Object... b) {
		return differences(a, Arrays.asList(b)).isEmpty();
	}
	
	
	public static boolean same(Map a, Map b) {
		if (a==null) return b==null;
		if (b==null) return false;
		for(Object ka : a.keySet()) {
			Object va = a.get(ka);
			Object vb = b.get(ka);
			if ( ! Utils.equals(va, vb)) {
				return false;
			}
		}
		for(Object kb : b.keySet()) {
			if ( ! a.containsKey(kb)) {
				return false;
			}
		}
		return true;
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
	 * @testedby  ContainersTest#testSubList()}
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
	 * @testedby  ContainersTest#testSubList()}
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
	 * Identical to {@link MathUtils#toArray(Collection)}
	 */
	public static double[] toArray(Collection<? extends Number> numbers) {
		return MathUtils.toArray(numbers);
	}

	/**
	 * Just an alias for {@link MathUtils#toIntArray(Collection)}
	 */
	public static int[] toIntArray(Collection<Integer> values) {
		return MathUtils.toIntArray(values);
	}

	/**
	 * Convenience for plucking values from a list. This will skip nulls in both the input list and the output value.
	 * @param list Can be null, and can contain nulls
	 * @param fn
	 * @return "truthy" outputs from fn
	 * @see Utils#truthy(Object)
	 */
	public static <A,B> List<B> pluckNotNull(List<A> list, Function<A,B> fn) {
		if (list==null) return Collections.EMPTY_LIST;
		// Java 8 streams + lambdas
//		return list.stream().filter(a -> a != null).map(fn).filter(b -> Utils.truthy(b)).collect(Collectors.toList());
		// Hm: w/o streams is actually cleaner and faster
		List<B> bs = filterNulls(apply(list, a -> a==null? null : fn.apply(a)));
		return bs;
	}
	

	/**
	 * By class and sub-class
	 * @param list
	 * @param klass
	 * @return
	 */
	public static <X, Y extends X> List<Y> filterByClass(Iterable<X> list, Class<Y> klass) {
		return (List) filter(list, IFilter.byClass(klass));
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
	 * 
	 * @param items Can be null (-> false)
	 * @param filter
	 * @return true if filter matches any item
	 */
	public static <X> boolean contains(Collection<X> items, IFilter<X> filter) {
		if (items==null) return false;
		for (X x : items) {
			if (filter.accept(x)) return true;
		}
		return false;
	}


	public static <X, X2 extends X> List<X2> filter(X[] list, IFilter<X> filter) {
		if (list==null) return null;
		assert filter != null : list;
		ArrayList<X2> out = new ArrayList();
		for (X x : list) {
			if (filter.accept(x)) {
				out.add((X2) x);
			}
		}
		return out;
	}


	public static <X> int indexOf(List<X> list, IFilter<X> filter) {
		for (int i = 0; i < list.size(); i++) {
			if (filter.accept(list.get(i))) return i;
		}
		return -1;
	}


	/**
	 * @param list
	 * @return the first and only element of list. Throws an exception if list is empty or has >1 elements.
	 */
	public static <X> X only(Iterable<X> list) {
		Iterator<X> it = list.iterator();
		if ( ! it.hasNext()) throw new IndexOutOfBoundsException("Empty: "+list);
		X obj = it.next();
		if (it.hasNext()) throw new NotUniqueException("Too many elements: "+list);
		return obj;
	}


	/**
	 * For handling data that might be a single item, but might be a list
	 * (e.g. ES json results, or web forms)
	 * @param itemOrListOrArray An item, or a list, or an array, or a Collection. Can be null (returns empty-list).
	 * @return definitely a list, never null, can be empty. Do not modify.
	 */
	public static <X> List<X> list(Object itemOrListOrArray) {
		if (itemOrListOrArray == null) {
			return Collections.EMPTY_LIST; 
		}
		if (itemOrListOrArray instanceof List) {
			return Collections.unmodifiableList((List)itemOrListOrArray);
		}		
		if (itemOrListOrArray.getClass().isArray()) {
			return Collections.unmodifiableList(asList(itemOrListOrArray));
		}
		if (itemOrListOrArray instanceof Collection) {
			return Collections.unmodifiableList(getList((Collection)itemOrListOrArray));
		}		
		// wrap a solo object as a 1-item list
		return (List<X>) Collections.singletonList(itemOrListOrArray);
	}

	/**
	 * Case and whitespace insensitive get
	 * @param <X>
	 * @param map
	 * @param key
	 * @return
	 */
	public static <X> X getLenient(Map<String, X> map, String key) {
		// normal key?
		X v = map.get(key);
		if (v != null) return v;
		if (Utils.isBlank(key)) {
			return null; // don't match "" against anything
		}
		// a few canonical forms
		v = map.get(key.trim());
		if (v != null) return v;
		v = map.get(key.trim().toLowerCase());
		if (v != null) return v;
		v = map.get(key.trim().toUpperCase());
		if (v != null) return v;
		String ck = StrUtils.toCanonical(key);
		v = map.get(ck);
		if (v != null) return v;
		v = map.get(ck.replaceAll(" ", "-"));
		if (v != null) return v;
		// search the keys
		Set<String> keys = map.keySet();
		BestOne<String> bestKey = new BestOne();
		Pattern p = Pattern.compile("\\b"+Pattern.quote(ck)+"\\b");
		for (String string : keys) {
			String cs = StrUtils.toCanonical(string);
			if (Utils.isBlank(cs)) continue;
			if (cs.startsWith(ck)) {
				bestKey.maybeSet(string, 2);
				continue;
			}
			// word match
			// But avoid "row" ~ "growth"			
			// TODO camel case support
			Matcher m = p.matcher(cs);
			if (m.find()) {
				bestKey.maybeSet(string, 1);
				continue;
			}
		}
		String k = bestKey.getBest(); // can be null
		if (k==null) {
			return null;
		}
		return map.get(k);
	}


	/**
	 * 
	 * @param <K>
	 * @param <V>
	 * @param map
	 * @return a new map, filtering out any null or other falsy values (see Utils.truthy() for details)
	 */
	public static <K,V> Map<K, V> filterFalsy(Map<K, V> map) {
		Map<K, V> newMap = applyToValues(v -> Utils.truthy(v)? v : null, map);
		return newMap;
	}


	/**
	 * Sort in place using values returned by a function. Example use-case: sort objects by their name.
	 * @param <X>
	 * @param list
	 * @param getSortValue
	 */
	public static <X> void sortBy(List<X> list, IFn<X,?> getSortValue) {		
		Comparator comp = (a,b) -> Containers.compare(getSortValue.apply((X)a), getSortValue.apply((X)b));
		Collections.sort(list, comp);
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

final class EmptyBag implements IProperties {
	@Override
	public <T> boolean containsKey(Key<T> key) {
		return false;
	}

	@Override
	public <T> T get(Key<T> key) {
		return null;
	}

	@Override
	public Collection<Key> getKeys() {
		return Collections.emptySet();
	}

	@Override
	public boolean isTrue(Key<Boolean> key) {
		return false;
	}

	@Override
	public <T> T put(Key<T> key, T value) {
		throw new UnsupportedOperationException();
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

final class IterableWrapper<X> implements Iterable<X> {
	private final Iterator<X> iterator;
	private boolean used;

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


/**
 * Note: this comparator imposes orderings that are inconsistent with equals."
 *
 * @author daniel
 *
 * @param <K>
 */
final class ValueComparator<K> implements Comparator<K>, Serializable {
	private static final long serialVersionUID = 1L;
	private Map<K, ?> map;

	public ValueComparator(Map<K, ?> map) {
		this.map = map;
	}

	@Override
	public int compare(K k1, K k2) {
		if (k1.equals(k2))
			return 0;
		Object v1 = map.get(k1);
		Object v2 = map.get(k2);
		int comp = Containers.compare(v1, v2);
		if (comp == 0) {
			int kc = Containers.compare(k1, k2);
			if (kc == 0) {
				Log.e("sort", "Error: Cant sort equals keys "+k1+"="+k2+" & values "+v1+"="+v2+"in "+map+"?!");
				return 0;						
			}
			return kc;
		}
		return comp;
	}
}
