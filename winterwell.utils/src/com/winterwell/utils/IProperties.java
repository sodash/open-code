/**
 * 
 */
package com.winterwell.utils;

import java.util.Collection;

import com.winterwell.utils.containers.Containers;

/**
 * Like a Map, but with type-safe keys.
 * 
 * @see Containers#getMap
 * 
 *      <p>
 *      Example code with a static set of default properties.
 *      </p>
 *      <code><pre>
 	private final Map<Key,Object> properties;
 	private final static Map<Key,Object> defaultProperties = new HashMap<Key,Object>();
 
	public <T> T get(Key<T> key) {
		Object v = properties.get(key); 
		if (v==null) v = defaultProperties.get(key); 
		return (T) v; 
	}
	public boolean isTrue(Key<Boolean> key) {
		Boolean v = get(key);
		return v!=null && v;
	}

	public Collection<Key> getKeys() {
		return properties.keySet(); 
	}
	
	public <T> T put(Key<T> key, T value) { 
		if (value==null) return (T) properties.remove(key); 
		else return (T) properties.put(key, value); 
	}

	public static <T> void putDefault(Key<T> key, T value) { 
		if (value==null) defaultProperties.remove(key); 
		else defaultProperties.put(key, value); 
	}          
	
	public <T> boolean containsKey(Key<T> key) {
		return get(key) != null;
	}
	
	</pre></code>
 * 
 * @author daniel
 */
public interface IProperties {

	/**
	 * Determine whether or not a value is set for this key.
	 * For some implementations, this is equivalent to get()!=null
	 * However implementations may allow explicit null values, in which case
	 * containsKey /can be/ different.
	 * 
	 * @param <T>
	 * @param key
	 * @return
	 */
	<T> boolean containsKey(Key<T> key);

	/**
	 * @param key
	 * @return The property value for this key if set, or null if unset.
	 */
	<T> T get(Key<T> key);

	/**
	 * @return keys for all the properties which have been set.
	 */
	Collection<Key> getKeys();

	/**
	 * Convenience for (value!=null && value), to make boolean properties easier
	 * to work with.
	 * 
	 * @param key
	 * @return true if key is set and true, false if unset or false
	 */
	boolean isTrue(Key<Boolean> key);

	/**
	 * Set (or remove) the value for key
	 * 
	 * @param <T>
	 * @param key
	 *            Must not be null
	 * @param value
	 *            If null, the key will be removed (does not affect default
	 *            properties)
	 * @return The old mapping for this key - if that's convenient/efficient -
	 *         or null. null should not be interpreted as meaning anything!
	 */
	<T> T put(Key<T> key, T value);

}
