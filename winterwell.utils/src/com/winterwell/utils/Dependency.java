package com.winterwell.utils;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Store common dependencies, like settings, database access objects, etc.
 * Stuff you might otherwise put in a static field somewhere.
 * A simple form of dependency injection - without forcing your code into a framework.
 * @author daniel
 *
 */
public final class Dependency {

	static ConcurrentHashMap<Class, Object> stash = new ConcurrentHashMap<>();
	
	static ConcurrentHashMap<Class, Supplier> factory = new ConcurrentHashMap<>();
	
	public static <X> void setSupplier(Class<X> klass, boolean singleton, Supplier<X> supplier) {
		if (singleton) {
			Supplier supplier2 = () -> {
				X x = supplier.get();
				stash.put(klass, x);
				return x;
			};
			factory.put(klass, supplier2);
		}
		factory.put(klass, supplier);
	}

	/**
	 * 
	 * @param klass
	 * @param value
	 * @return value
	 */
	public static <X> X set(Class<X> klass, X value) {
		stash.put(klass, value);
		return value;
	}
	

	/**
	 * This can create a new instance! Use #contains() to test for whether we have one.
	 * @param class1
	 * @return
	 */
	public static <X> X get(Class<X> class1) {
		X x = (X) stash.get(class1);
		if (x!=null) return x;
		Supplier<X> s = factory.get(class1);
		if (s!=null) {
			x = s.get();
			// don't store factory output. make it fresh. ??
//			stash.put(class1, x);
			return x;
		}
		// try a default constructor
		try {
			x = class1.newInstance();
			stash.put(class1, x);
			return x;
		} catch (InstantiationException | IllegalAccessException e) {
			throw Utils.runtime(e);
		}
	}
	
	public static boolean isSet(Class klass) {
		if (stash.containsKey(klass)) return true;
		if (factory.containsKey(klass)) return true;
		return false;
	}

}
