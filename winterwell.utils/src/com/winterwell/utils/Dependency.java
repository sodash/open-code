package com.winterwell.utils;

import java.util.NoSuchElementException;
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

	static ConcurrentHashMap<DKey, Object> stash = new ConcurrentHashMap<>();
	
	static ConcurrentHashMap<DKey, Supplier> factory = new ConcurrentHashMap<>();
	
	public static <X> void setSupplier(Class<X> klass, boolean singleton, Supplier<X> supplier) {
		DepContext ctxt = getContext();
		if (singleton) {
			Supplier supplier2 = () -> {
				X x = supplier.get();
				stash.put(key(klass, ctxt), x);
				return x;
			};
			factory.put(key(klass, ctxt), supplier2);
		}
		factory.put(key(klass, ctxt), supplier);
	}

	
	
	private static DKey key(Class klass, DepContext ctxt) {
		return new DKey(klass, ctxt);
	}



	/**
	 * 
	 * @param klass
	 * @param value
	 * @return value
	 */
	public static <X> X set(Class<X> klass, X value) {
		DepContext ctxt = getContext();
		stash.put(key(klass, ctxt), value);
		return value;
	}
	

	/**
	 * This can create a new instance! Use #contains() to test for whether we have one.
	 * @param class1
	 * @return
	 */
	public static <X> X get(Class<X> class1) {
		DepContext ctxt = getContext();
		return get(class1, ctxt);
	}

	public static <X> X get(Class<X> class1, DepContext ctxt) {
		assert ! ctxt.closed;
		while(ctxt!=null) {			
			X x = (X) stash.get(key(class1, ctxt));
			if (x!=null) return x;
			Supplier<X> s = factory.get(key(class1, ctxt));
			if (s!=null) {
				x = s.get();
				// don't store factory output. make it fresh. ??
	//			stash.put(class1, x);
				return x;
			}
		}
		return null;
	}
	
	private static ThreadLocal<DepContext> context = new ThreadLocal<DepContext>() {
		protected DepContext initialValue() {
			return new DepContext(null, null);
		};
	};
	
	public static DepContext setContext(Object contextKey) {
		DepContext ctxt = getContext();
		DepContext newContext = new DepContext(ctxt, contextKey);
		context.set(newContext);
		return newContext;
	}
	
	public static DepContext getContext() {
		DepContext ctxt = context.get();
		while(true) {			
			if ( ! ctxt.closed) return ctxt;
			ctxt = ctxt.parent;
			context.set(ctxt);
			// since the null-parent top contexts can't be closed, this should always finish
		}
	}
	
	public static boolean isSet(Class class1) {
		DepContext ctxt = getContext();
		while(ctxt != null) {
			DKey klass = key(class1, ctxt);
			if (stash.containsKey(klass)) return true;
			if (factory.containsKey(klass)) return true;
			ctxt = ctxt.parent;
		}
		return false;
	}

}


final class DKey {

	private final DepContext context;
	private final Class klass;

	public DKey(Class klass, DepContext context) {
		this.klass = klass;
		this.context = context;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((context == null) ? 0 : context.hashCode());
		result = prime * result + ((klass == null) ? 0 : klass.hashCode());
		return result;
	}

	@Override
	public String toString() {
		return "DKey [context=" + context + ", klass=" + klass + "]";
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DKey other = (DKey) obj;
		if (context == null) {
			if (other.context != null)
				return false;
		} else if (!context.equals(other.context))
			return false;
		if (klass == null) {
			if (other.klass != null)
				return false;
		} else if (!klass.equals(other.klass))
			return false;
		return true;
	}
	
}