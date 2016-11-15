package winterwell.utils.containers;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * A Bi-Directional (bijective) map built on two HashMaps.
 * 
 * TODO bijectivity is not guaranteed. E.g. if we have {a:1, b:2} and set a:2,
 * we'll get {a:2, b:2}
 * 
 * @author daniel
 * 
 */
public final class BiDiMap<K, V> extends HashMap<K, V> {
	private static final long serialVersionUID = 1L;

	private final Map<V, K> inverse = new HashMap<V, K>();

	@Override
	public void clear() {
		super.clear();
		inverse.clear();
	}

	@Override
	public boolean containsValue(Object v) {
		return inverse.containsKey(v);
	}

	public K getInverse(V value) {
		return inverse.get(value);
	}

	public Map<V, K> getInverseMap() {
		return inverse;
	}

	@Override
	public V put(K key, V value) {
		V v = super.put(key, value);
		inverse.put(value, key);
		// note you can break bijectivity
		return v;
	}

	@Override
	public V remove(Object key) {
		V v = super.remove(key);
		inverse.remove(v);
		return v;
	}

	@Override
	public Collection<V> values() {
		return inverse.keySet();
	}

}
