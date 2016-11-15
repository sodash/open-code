package com.winterwell.utils.containers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.stream.Collectors;

import com.winterwell.utils.MathUtils;

import winterwell.utils.IFn;
import winterwell.utils.IProperties;
import winterwell.utils.Key;
import winterwell.utils.Utils;
import winterwell.utils.containers.ArrayMap;
import winterwell.utils.reporting.Log;

final class CompareAny implements Comparator {
	@Override
	public int compare(Object o1, Object o2) {
		return Containers.compare(o1, o2);
	}
}


public class Containers extends winterwell.utils.containers.Containers {

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
			Log.report("containers", "compare(): hashCode() not unique for "
					+ a + ", " + b + "; sorting may go awry", Level.FINE);
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
	public static boolean contains(String s, String[] strings) {
		for (String string : strings) {
			if (s == null ? string == null : s.equals(string))
				return true;
		}
		return false;
	}

	/**
	 * Identical to {@link #removeNulls(Iterable)}! This is just an aide-memoire.
	 * @param list
	 * @return list without any nulls
	 */
	public static <X> List<X> filterNulls(Iterable<X> list) {
		return removeNulls(list);
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
		v = factory.apply(key);
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
	 * Get the keys to a map, sorted by their values.
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

	
	private static <K, V> Comparator<K> getValueComparator(final Map<K, V> map) {
		return new Comparator<K>() {
			@Override
			public int compare(K k1, K k2) {
				if (k1.equals(k2))
					return 0;
				Object v1 = map.get(k1);
				Object v2 = map.get(k2);
				int comp = Containers.compare(v1, v2);
				if (comp == 0) {
					int kc = Containers.compare(k1, k2);
					if (kc == 0) throw new IllegalStateException("Cant sort equals keys "+k1+" in "+map+"?!");
					return kc;
				}
				return comp;
			}
		};
	}
	
	/**
	 * Copy of map whose keySet() method returns keys in order sorted by value,
	 * smallest(true)/largest(false) first.
	 * 
	 * @param map
	 * @param maxNum
	 *            -1 for all
	 * @return an ArrayMap with keys, sorted by value
	 * @testedby {@link ContainersTest#testGetValueSortedMap()} Note: builds on
	 *           {@link #getSortedKeys(Map)}
	 */
	public static <K, V> Map<K, V> getValueSortedMap(final Map<K, V> map,
			final boolean smallestFirst, final int maxNum) {
		// pick keys
		List<K> keys = getSortedKeys(map);
		// sub set
		if (!smallestFirst) {
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
	 * @param list
	 * @return list without any null items
	 */
	public static <X> List<X> removeNulls(Iterable<X> list) {
//		return filter(IFilter<X>.NOT_NULL, list);
		List<X> list2 = new ArrayList();
		for (X x : list) {
			if (x!=null) list2.add(x);
		}
		return list2;
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
		// Java 8 streams + lambdas for the win
		return list.stream().filter(a -> a != null).map(fn).filter(b -> Utils.truthy(b)).collect(Collectors.toList());
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