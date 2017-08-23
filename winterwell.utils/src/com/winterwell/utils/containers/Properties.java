package com.winterwell.utils.containers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.winterwell.utils.IProperties;
import com.winterwell.utils.Key;
import com.winterwell.utils.Printer;
import com.winterwell.utils.Utils;

/**
 * A type-safe multi-class sort-of-map. NB This backends onto a {@link HashMap}
 * so is not threadsafe.
 * 
 * @author daniel
 * 
 */
public final class Properties implements IProperties {

	/**
	 * Convenience convertor.
	 * @param keys
	 * @return keys, wrapped in Key objects
	 */
	public static List<Key> strings2keys(Collection<String> keys) {
		ArrayList<Key> _keys = new ArrayList();
		for (String string : keys) {
			_keys.add(new Key(string));
		}
		return _keys;
	}
	
	public Map<String,Object> asMap() {
		return map;		
	}
//	
//	class PMap extends AbstractMap2<String, Object> {
//		@Override
//		public Object get(Object key) {
//			return Properties.this.get(new Key((String)key));
//		}
//
//		@Override
//		public Object put(String key, Object value) {
//			return Properties.this.put(new Key(key), value);
//		}
//		@Override
//		public Set<String> keySet() throws UnsupportedOperationException {
//			// NOT quite correct - edits don't link up
//			return new HashSet(Containers.apply(getKeys(), k -> k.name));
//		}		
//		@Override
//		public int size() {
//			return Properties.this.size();
//		}
//	}
	
	/**
	 * This is a lop-sided matching test. The case (1st parameter) must be
	 * equals() to every property of the description (2nd parameter). E.g. {a:1,
	 * b:2} matches the description {a:1} -- but not vice-versa.
	 * 
	 * @param concreteCase
	 *            The more specific set of properties
	 * @param desc
	 *            The vague set of properties
	 * @return true if concreteCase matches desc.
	 */
	public static boolean matches(IProperties concreteCase, IProperties desc) {
		for (Key k : desc.getKeys()) {
			Object v = concreteCase.get(k);
			Object v2 = desc.get(k);
			if (!Utils.equals(v, v2))
				return false;
		}
		return true;
	}
	
	private final HashMap<String, Object> map = new HashMap();

	public Properties() {
		//
	}

	/**
	 * Shallow copy the original properties.
	 * 
	 * @param original
	 */
	public Properties(IProperties original) {
		for (Key<Object> key : original.getKeys()) {
			put(key, original.get(key));
		}
	}

	/**
	 * Copy the map key/value pairs in - without any type checking of course.
	 * 
	 * @param map
	 *            Keys may be Keys or Strings (which will be converted to Keys)
	 */
	public Properties(Map map) {
		for (Object k : map.keySet()) {
			Key key;
			if (k instanceof Key) {
				key = (Key) k;
			} else {
				key = new Key((String) k);
			}
			put(key, map.get(k));
		}
	}

	/**
	 * Convenience constructor
	 * 
	 * @param keyValuePairs
	 */
	public Properties(Object... keyValuePairs) {
		assert keyValuePairs.length % 2 == 0 : keyValuePairs;
		for (int i = 0; i < keyValuePairs.length; i += 2) {
			Key key = (Key) keyValuePairs[i];
			put(key, keyValuePairs[i + 1]);
		}
	}

	@Override
	public <T> boolean containsKey(Key<T> key) {
		return map.containsKey(key.getName());
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T get(Key<T> key) {
		Object v = map.get(key.getName());
		return (T) v;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Collection<Key> getKeys() {
		return strings2keys(map.keySet());
	}

	@Override
	public boolean isTrue(Key<Boolean> key) {
		Boolean v = get(key);
		return v != null && v;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T put(Key<T> key, T value) {
		assert key != null;
		if (value == null)
			return (T) map.remove(key.getName());
		else
			return (T) map.put(key.getName(), value);
	}

	/**
	 * @return the number of entries in this property bag.
	 */
	public int size() {
		return map.size();
	}

	@Override
	public String toString() {
		return Printer.toString(map);
	}
}
