package com.winterwell.utils;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * A memory-safe alternative to Java's String.intern.
 * 
 * Unlike intern, this can forget entries so it does NOT guarantee == implies equals()
 * 
 * @author daniel
 * @testedby InternTest
 *
 */
public class Intern {

	
	/**
	 * Hm: Or should we use a cache, e.g. CacheBuilder or HalfLifeMap?
	 * Note: do not use CacheBuilder's weakKeys -- this relies on == testing.
	 */
	static final Map<String, WeakReference<String>> strings = Collections.synchronizedMap(new WeakHashMap<>());
		
	
	public static String get(String s) {
		Mutable.Ref<String> result = new Mutable.Ref();
		strings.compute(s, (k,wr) -> {
			// cached?
			if (wr!=null) {
				String v = wr.get();
				if (v!=null) {
					result.value = v; 
					return wr;
				}
			}
			// set fresh
			result.value = s;
			return new WeakReference(s);
		});
		return result.value;
	}
	
	public static boolean contains(String s) {
		WeakReference<String> wr = strings.get(s);
		return wr!=null && wr.get()!=null;
	}

	public static void clear() {
		strings.clear();
	}
}
