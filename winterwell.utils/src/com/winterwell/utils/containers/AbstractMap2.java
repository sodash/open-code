/* (c) Winterwell Associates Ltd 2009
 * This class is released under MIT license. This does not imply any license 
 * to other Winterwell classes.
 */
package com.winterwell.utils.containers;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.winterwell.utils.Printer;

/**
 * Base class for creating maps. You need to define get and put, which is easier
 * than {@link AbstractMap}'s entrySet. You are <i>strongly encouraged</i> to
 * define {@link #keySet()} to get full Map support. Warning: some methods are
 * inefficient unless over-ridden.
 * 
 * @author daniel
 */
public abstract class AbstractMap2<K, V> implements Map<K, V> {

	@Override
	public void clear() {
		for (Object k : keySet().toArray()) {
			remove(k);
		}
	}

	@Override
	public boolean containsKey(Object key) {
		// note: key=null is different from no-key, hence we check the keyset
		return keySet().contains(key);
	}

	@Override
	public boolean containsValue(Object value) {
		return values().contains(value);
	}

	@Override
	public Set<java.util.Map.Entry<K, V>> entrySet() {
		Set<Entry<K, V>> entries = new HashSet<Entry<K, V>>(size());
		for (K k : keySet()) {
			entries.add(new MapEntry<K, V>(k, get(k)));
		}
		// Does not support editing
		Set<Entry<K, V>> safeEntries = Collections.unmodifiableSet(entries);
		return safeEntries;
	}

	@Override
	public boolean isEmpty() {
		return size() == 0;
	}

	@Override
	public Set<K> keySet() throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void putAll(Map<? extends K, ? extends V> m) {
		for (K k : m.keySet()) {
			put(k, m.get(k));
		}
	}

	@Override
	public V remove(Object key) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int size() {
		return keySet().size();
	}

	@Override
	public String toString() {
		return Printer.toString(this, ", ", ": ");
	}

	@Override
	public Collection<V> values() {
		Set<V> values = new HashSet<V>(size());
		for (K k : keySet()) {
			values.add(get(k));
		}
		// Does not support editing
		Set<V> safeValues = Collections.unmodifiableSet(values);
		return safeValues;
	}
}
