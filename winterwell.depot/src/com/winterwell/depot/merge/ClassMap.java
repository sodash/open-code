package com.winterwell.depot.merge;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.winterwell.utils.containers.AbstractMap2;

/**
 * A map which can check up the class hierarchy, and will memoize some look-ups for speed.
 * @author daniel
 *
 * @param <V>
 */
public class ClassMap<V> extends AbstractMap2<Class, V> {

	private static final Object NULL = "null";

	@Override
	public V get(Object key) {
		Class klass = (Class) key;
		Object v = base.get(klass);
		if (v!=null) {
			return r(v);
		}		
		// by interface
		Class[] ifaces = klass.getInterfaces();
		for (Class ik : ifaces) {
			v = base.get(ik);
			if (v!=null) { // && check(v, klass)) ??do check on all returns??
				// memoize
				base.put(klass, v);
				return r(v);
			}
		}
		// by super
		Class spr = klass.getSuperclass();
		if (spr==null) return null;
		v = get(spr);
		if (v!=null) {
			// memoize
			base.put(klass, v);
			return r(v);
		}
		// memoize fails? Could get big!
//		base.put(klass, NULL);
		return null;
	}

	private boolean check(Object val, Class key) {
		if (val==NULL) return true;
		V v = (V) val;
		// TODO blacklist some class/value combinations?
		// Or is that best done via adding extra values to intercept a bad result?
		return true;
	}

	@Override
	public V put(Class key, V value) {
		Object old = base.put(key, value);
		return r(old);
	}
	
	private V r(Object v) {
		return v==NULL? null : (V)v;
	}

	@Override
	public Set<Class> keySet() throws UnsupportedOperationException {
		return base.keySet();
	}

	final Map<Class,Object> base = new HashMap();
	
}
