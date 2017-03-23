package com.winterwell.utils;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Stack;

import com.winterwell.utils.io.ISerialize;

/**
 * Store global (default) and background (thread-local) properties. A background
 * property being one that would be clumsy to pass round as a method argument.
 * <p>
 * Environment also provides support for creating stacks of properties via
 * {@link #push(Key, Object)} and {@link #pop(Key)}.
 * <p>
 * This is NOT linked to the OS's environment! For that, use {@link System#getProperty(String)} 
 * @see Dep ??Should we merge this with Dep??
 * @author daniel
 */
public final class Environment implements IProperties {

	private static final Map<Key, Object> defaultProperties = new HashMap<Key, Object>();

	private final static Environment dflt = new Environment();

	/**
	 * Convenience for class-based keys
	 * @param class1 
	 * @return object or null
	 */
	public <X> X get(Class<X> klass) {		
		return (X) get(new Key(klass.getName()));
	}
	
	public <X> X put(Class<X> klass, X value) {
		// NB we don't use value.getClass() in case of sub-classing then breaking the get().
		return (X) put(new Key(klass.getName()), value);
	}
	
	
	/**
	 * @return never null
	 */
	public static Environment get() {
		return dflt;
	}

	public static <T> void putDefault(Key<T> key, T value) {
		if (value == null) {
			defaultProperties.remove(key);
		} else {
			defaultProperties.put(key, value);
		}
	}

	private final ThreadLocal<Map<Key, Object>> localVars = new ThreadLocal<Map<Key, Object>>() {
		@Override
		protected Map<Key, Object> initialValue() {
			return new HashMap<Key, Object>();
		}
	};

	private boolean debug;

	@Override
	public <T> boolean containsKey(Key<T> key) {
		T result = get(key);
		return (result != null);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T get(Key<T> key) {
		assert key != null;
		Map<Key, Object> _properties = localVars.get();
		Object v = _properties.get(key);
		if (v == null) {
			v = defaultProperties.get(key);
		}
		return (T) v;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Collection<Key> getKeys() {
		Map<Key, Object> _properties = localVars.get();
		HashSet<Key> keys = new HashSet<Key>();
		keys.addAll(_properties.keySet());
		keys.addAll(defaultProperties.keySet());
		// TODO remove stack keys
		return keys;
	}

	@Override
	public boolean isTrue(Key<Boolean> key) {
		Boolean v = get(key);
		return v != null && v;
	}

	/**
	 * Remove the current value from the stack (which must exist!), and replace
	 * it with the previous value.
	 * 
	 * @param <X>
	 * @param key
	 * @return the old value
	 */
	public <X> X pop(Key<X> key) {
		assert key != null;
		Map<Key, Object> _properties = localVars.get();
		// adjust stack
		Key<Stack<X>> stackKey = new StackKey(key);
		Stack<X> stack = (Stack<X>) _properties.get(stackKey);
		assert stack != null;
		X oldValue = stack.pop();
		X newValue = stack.peek();
		// set top value (may be null)
		put(key, newValue);
		return oldValue;
	}

	public <X> void push(Key<X> key, X value) {
		assert key != null && value != null;
		Map<Key, Object> localProperties = localVars.get();
		// set top value
		put(key, value);
		// adjust stack
		Key<Stack<X>> stackKey = new StackKey(key);
		Stack<X> stack = (Stack<X>) localProperties.get(stackKey);
		if (stack == null) {
			stack = new Stack<X>();
			localProperties.put(stackKey, stack);
		}
		stack.push(value);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T put(Key<T> key, T value) {
		assert key != null;
		Map<Key, Object> _properties = localVars.get();
		if (value == null)
			return (T) _properties.remove(key);
		else
			return (T) _properties.put(key, value);
	}

	/**
	 * Clear (thread-) local overrides and reset to default.
	 */
	public void reset() {
		localVars.remove();
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (Key k : getKeys()) {
			sb.append(k + ": " + get(k));
		}
		return sb.toString();
	}

	public void setDebug(boolean b) {
		this.debug = b;		
	}

	/**
	 * A special flag for switching on debug mode.
	 * @return
	 */
	public boolean isDebug() {
		return debug;
	}

	private static final Properties properties = new Properties();
	
	/**
	 * A global static properties. Handy, if also dubious.
	 */
	public static Properties getProperties() {
		return properties;
	}
	
	/**
	 * Keep a global static property store.
	 * @param field
	 * @param dfltValue
	 * @return value or null
	 */
	public static <T> T getProperty(Key<T> field, T dfltValue) throws RuntimeException {
		String name = field.getName();
		String prop = properties.getProperty(name);
		if (prop==null) return dfltValue;
		if (field instanceof ISerialize) {
			try {
				return ((ISerialize<T>) field).fromString(prop);
			} catch (Exception e) {
				throw Utils.runtime(e);
			}
		}
		return (T) prop;
	}
	

}

final class StackKey<X> extends Key<Stack<X>> {
	private static final long serialVersionUID = 1L;

	public StackKey(Key<X> key) {
		super(key.getName() + ".envstack");
	}
}
