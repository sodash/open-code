
import java.util.WeakHashMap;
import java.util.function.BiFunction;

import com.google.common.cache.CacheBuilder;

/**
 * A memory-safe alternative to Java's String.intern.
 * 
 * Unlike intern, this can forget entries so it does NOT guarantee == implies equals()
 * 
 * @author daniel
 *
 */
public class Intern {

	
	/**
	 * Hm: Or should we use a cache, e.g. CacheBuilder or HalfLifeMap?
	 * Note: do not use CacheBuilder's weakKeys -- this relies on == testing.
	 */
	static final WeakHashMap<String, Boolean> strings = new WeakHashMap<>();
		
	
	public static String get(String s) {
		Mutable.Ref result = new Mutable.Ref();
		strings.compute(s, (k,v) -> {
			// ??
		});
		return result.value;
	}
	
}
