/* (c) Winterwell 2008-2011
 * 
 */
package com.winterwell.utils.containers;

import java.util.Map;
import java.util.Map.Entry;

/**
 * Simple implementation of {@link java.util.Map.Entry}.
 * 
 * Supports reusing the same object via {@link #reset(Object, Object)}.
 * 
 * @author Daniel
 * 
 * @param <K>
 * @param <V>
 */
public final class MapEntry<K, V> implements Entry<K, V> {

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((k == null) ? 0 : k.hashCode());
		result = prime * result + ((v == null) ? 0 : v.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MapEntry other = (MapEntry) obj;
		if (k == null) {
			if (other.k != null)
				return false;
		} else if (!k.equals(other.k))
			return false;
		if (v == null) {
			if (other.v != null)
				return false;
		} else if (!v.equals(other.v))
			return false;
		return true;
	}

	private K k;

	private final Map<K, V> map;
	private V v;

	public MapEntry() {
		map = null;
	}

	public MapEntry(K key, V value) {
		this(key, value, null);
	}

	public MapEntry(Map<K, V> backingMap) {
		map = backingMap;
	}

	public MapEntry(K key, V value, Map<K, V> backingMap) {
		k = key;
		v = value;
		map = backingMap;
	}

	@Override
	public K getKey() {
		return k;
	}

	@Override
	public V getValue() {
		return v;
	}

	/**
	 * Reset the key and value of this object. This allows one MapEntry object
	 * to be reused. Not sure if this is a good idea or not.
	 * 
	 * @param k
	 * @param v
	 */
	public void reset(K key, V val) {
		this.k = key;
		this.v = val;
	}

	@Override
	public V setValue(V value) {
		V old = v;
		v = value;
		// will fail if map was not set
		assert map != null : this;
		map.put(k, value);
		return old;
	}

	@Override
	public String toString() {
		return k + ":" + v;
	}
}
