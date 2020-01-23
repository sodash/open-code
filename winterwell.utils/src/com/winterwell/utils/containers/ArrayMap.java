package com.winterwell.utils.containers;

import java.io.Serializable;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.winterwell.utils.StrUtils;
import com.winterwell.utils.Utils;

/**
 * A map backed by an array. Fast for small collections, lousy for big ones. Has
 * a convenient constructor. Another nice property is that insertion order is
 * preserved (like LinkedHashMap). I.e. keySet() values() and entrySet() all return objects in the
 * order they were added.
 * <p>
 * ArrayMap provides iteration over keys which ALLOWS the map to be edited
 * during the iteration. ie. it saves you writing
 * <code>for(X x : map.keySet().toArray(new X[0]))</code> when editing a map.
 * <p>
 * ArrayMap is NOT thread safe. Multi-threaded writes could lead to data
 * corruption (which will throw a ConcurrentModificationException).
 * If this happens, you best discard the map!
 * However it is safer than HashMap: doing a get in thread 1 + a put in thread 2 is fine.
 * 
 * @author Daniel
 * @testedby ArrayMapTest
 * @testedby ArrayMapPerfTest
 * @param <K>
 * @param <V>
 */
public class ArrayMap<K, V> extends AbstractMap<K, V> implements
		Serializable, Iterable<K>, Cloneable {
	private static final long serialVersionUID = 1L;

	@Override
	protected ArrayMap<K, V> clone() throws CloneNotSupportedException {
		// NB: a more clone-based approach doesn't let us set final values
		ArrayMap clone = new ArrayMap();
		clone.putAll(this);
		return clone;
	}

	private final ArrayList<K> keys;
	private final ArrayList<V> values;

	/**
	 * Create a map optimised for small cases.
	 */
	public ArrayMap() {
		this(4);
	}

	/**
	 * Convenient access to the 1st key/value pair.
	 * 
	 * @throws IndexOutOfBoundsException
	 *             if the map is empty
	 */
	public Pair2<K, V> first() {
		return new Pair2(keys.get(0), values.get(0));
	}

	/**
	 * Create a map with the given starting capacity.
	 */
	public ArrayMap(int initialSize) {
		keys = new ArrayList<K>(initialSize);
		values = new ArrayList<V>(initialSize);
	}

	/**
	 * Is there any call for this??
	 * 
	 * @param pList
	 */
	public ArrayMap(List<Pair2<K, V>> pList) {
		keys = new ArrayList<K>(pList.size());
		values = new ArrayList<V>(pList.size());
		for (Pair2<K, V> kv : pList) {
			keys.add(kv.first);
			values.add(kv.second);
		}
	}

	/**
	 * Copy constructor
	 * 
	 * @param copyMe Can be null (produces an empty map)
	 */
	public ArrayMap(Map<? extends K, ? extends V> copyMe) {
		this(copyMe==null? 4 : copyMe.size());
		if (copyMe==null) return;
		for (K k : copyMe.keySet()) {
			put(k, copyMe.get(k));
		}
	}

	/**
	 * Awesomely convenient constructor:
	 * <code>new ArrayMap(key1, value1, key2, value2, ...)</code> Warning: Does
	 * not check for duplicates!
	 * 
	 * @param keyValuePairs
	 */
	public ArrayMap(Object... keyValuePairs) {
		int n = Math.max(keyValuePairs.length / 2, 1);
		keys = new ArrayList<K>(n);
		values = new ArrayList<V>(n);
		// wrong constructor?
		if (keyValuePairs.length==1 && keyValuePairs[0] instanceof Map) {
			// should have been a call to the copy constructor! Handle it here anyway
			Map<K,V> copyMe = (Map) keyValuePairs[0];
			for (K k : copyMe.keySet()) {
				put(k, copyMe.get(k));
			}	
			return;
		}
		assert keyValuePairs.length % 2 == 0;
		for (int i = 0; i < keyValuePairs.length; i += 2) {
			keys.add((K) keyValuePairs[i]);
			values.add((V) keyValuePairs[i + 1]);
		}
	}

	@Override
	public boolean containsKey(Object key) {
		return keys.contains(key);
	}

	@Override
	public boolean containsValue(Object value) {
		return values.contains(value);
	}

	@Override
	public Set<java.util.Map.Entry<K, V>> entrySet() {
		return new EntrySet();
	}
	
	/**
	 * key-value entry set.
	 * NB: we use a named class not an anonymous class for code stability 
	 */
	private final class EntrySet extends AbstractSet<Entry<K, V>> {
		public EntrySet() {		
		}
		
		@Override
		public Iterator<java.util.Map.Entry<K, V>> iterator() {
			return new Iterator<Entry<K, V>>() {
				MapEntry<K, V> entry = new MapEntry<K, V>(ArrayMap.this);
				int i = 0;

				@Override
				public boolean hasNext() {
					return i < keys.size();
				}

				@Override
				public java.util.Map.Entry<K, V> next() {
					entry.reset(keys.get(i), values.get(i));
					i++;
					return entry;
				}

				@Override
				public void remove() {
					keys.remove(i - 1);
					values.remove(i - 1);
					i--;
				}

			};
		}

		@Override
		public int size() {
			return keys.size();
		}
	};
	

	@Override
	public V get(Object key) {
		int i = keys.indexOf(key);
		if (i == -1)
			return null;
		return values.get(i);
	}

	public List<Pair2<K, V>> getPList() {
		List<Pair2<K, V>> pList = new ArrayList<Pair2<K, V>>(size());
		for (int i = 0, n = size(); i < n; i++) {
			pList.add(new Pair2<K, V>(keys.get(i), values.get(i)));
		}
		return pList;
	}

	/**
	 * Iterate over the map keys and ALLOW editing within the iteration. This is
	 * a convenience for copy-keys then return an iterator.
	 */
	@Override
	public Iterator<K> iterator() {
		// copy keys to allow editing
		return new ArrayList(keys).iterator();
	}

	/**
	 * The keys in this map. This is not modifiable!
	 */
	@Override
	public Set<K> keySet() {
		return Collections.unmodifiableSet(Containers.listAsSet(keys));
	}

	@Override
	public V put(K key, V value) {
		assert key != null : value;
		// should we quietly ignore null values? No, let's treat them like any other value
		mod++;
		char m = mod;
		int i = keys.indexOf(key);
		if (i != -1) {
			V old = values.set(i, value);
			// Warning: there's a race condition on the removal of an earlier
			// key/value pair
			if (mod != m) {
				// _some_ thread safety (the data-corruption damage has already
				// been done)
				throw new ConcurrentModificationException(StrUtils.ellipsize(
						key + "=" + value, 100));
			}
			return old;
		}
		keys.add(key);
		values.add(value);
		// Warning: there's a race condition on add or removal
		if (mod != m) {
			// _some_ thread safety (the data-corruption damage has already been
			// done)
			throw new ConcurrentModificationException(StrUtils.ellipsize(key
					+ "=" + value, 100));
		}
		return null;
	}

	/**
	 * Guard against threading issues
	 */
	private transient volatile char mod;

	@Override
	public V remove(Object key) {
		int i = keys.indexOf(key);
		if (i == -1)
			return null;
		mod++;
		char m = mod;
		keys.remove(i);
		V del = values.remove(i);
		// Warning: there's a race condition on the removal of an earlier
		// key/value pair
		if (mod != m) {
			// _some_ thread safety (the data-corruption damage has already been
			// done)
			throw new ConcurrentModificationException(StrUtils.ellipsize(
					String.valueOf(key), 100));
		}
		return del;
	}

	@Override
	public int size() {
		return keys.size();
	}

	/**
	 * The values in this map. This is not modifiable!
	 */
	@Override
	public List<V> values() {
		return Collections.unmodifiableList(values);
	}

	/**
	 * @param length
	 * @return ArrayMap or HashMap -- whichever is likely to be faster for the
	 *         given size.
	 * @see ArrayMapPerfTest
	 */
	public static Map useMe(int length) {
		return length < 5 ? new ArrayMap(length) : new HashMap(length);
	}

	/**
	 * @deprecated May be removed in future
	 * 
	 * Re-sort by value. If you want to sort by key, just use TreeMap instead.
	 * @param comparator Can be null, in which case do a default-order value sort
	 */
	public void sort(final Comparator<V> comparator) {
		ArrayList<Pair2<V,K>> vk = new ArrayList();
		for(int i=0; i<keys.size(); i++) {
			vk.add(new Pair2<V,K>(values.get(i), keys.get(i)));
		}
		if (comparator==null) {
			Collections.sort(vk);
		} else {
			Collections.sort(vk, new StdComparator(comparator));
		}
		// copy back
		for(int i=0; i<keys.size(); i++) {
			keys.set(i, vk.get(i).second);
			values.set(i, vk.get(i).first);
		}
	}

	
}

final class StdComparator<V,K> implements Comparator<Pair2<V,K>> {
	private final Comparator<V> comparator;
	public StdComparator(Comparator<V> comparator) {
		this.comparator = comparator;
	}

	@Override
	public int compare(Pair2<V, K> o1, Pair2<V, K> o2) {
		int c = comparator.compare(o1.first, o2.first);
		if (c!=0) return c;
		return Utils.compare(o1.second, o2.second);
	}	
}