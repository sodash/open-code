/* (c) Winterwell 2008-2011
 * 
 */
package winterwell.utils.containers;

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
public final class MapEntry<K, V> extends com.winterwell.utils.containers.MapEntry<K, V> {
	
	public MapEntry() {
		super();
	}
	public MapEntry(K k, V v) {
		super(k,v);
	}	
}
