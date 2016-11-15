package winterwell.utils.containers;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Map keys to lists of values.
 * <p>
 * Adds a couple of useful methods to Map. Of which {@link #add(Object, Object)}
 * and {@link #getOne(Object)} are perhaps the most useful.
 * 
 * @author daniel
 * 
 * @param <K>
 * @param <V>
 * @see SetMap
 */
public final class ListMap<K, V> extends CollectionMap<K, V, List<V>> implements Serializable {
	private static final long serialVersionUID = 1L;
	
	public ListMap() {
	}

	public ListMap(int initialCapacity) {
		super(initialCapacity);
	}

	
	ArrayList<V> newList(int sizeHint) {
		return new ArrayList<V>(sizeHint==-1? 4 : sizeHint);
	}
}

abstract class CollectionMap<K, V, CV extends Collection<V>> extends HashMap<K, CV> {

	private static final long serialVersionUID = 1L;

	/**
	 * A SLOW inverse operation. Check all of the map for key-to-value mappings.
	 * 
	 * @param value
	 * @return all keys which list this value
	 */
	public Collection<K> reverseLookup(V value) {
		Collection<K> keys = new HashSet();
		for (Map.Entry<K, CV> kv : entrySet()) {
			if (kv.getValue() != null && kv.getValue().contains(value)) {
				keys.add(kv.getKey());
			}
		}
		return keys;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * WARNING: Be careful if using Arrays.asList()! Users of this class
	 * typically expect to use List.add(), which isn't possible with a wrapped
	 * array. We suggest using new ArrayList(Arrays.asList()), or equivalently,
	 * Containers.list() to make a defensive copy.
	 */
	@Override
	public CV put(K key, CV value) {
		return super.put(key, value);
	}

	CollectionMap() {
	}

	CollectionMap(int initialCapacity) {
		super(initialCapacity);
	}

	/**
	 * Add value to the list stored for key, creating a new list if needed. You
	 * can add multiple copies of the same value.
	 * 
	 * @param key
	 * @param value
	 * @see #addOnce(Object, Object)
	 */
	public void add(K key, V value) {
		CV list = get(key);
		if (list == null) {
			list = newList(-1);
			put(key, list);
		}
		list.add(value);
	}
	/**
	 * 
	 * @param sizeHint Can be -1 for "default"
	 * @return
	 */
	abstract CV newList(int sizeHint);

	/**
	 * Add a lot of values to the list for this key.
	 * 
	 * @param key
	 * @param values
	 */
	public <V2 extends V> void addAll(K key, Collection<V2> values) {
		CV list = get(key);
		if (list == null) {
			list = newList(values.size());
			put(key, list);
		}
		list.addAll(values);
	}

	public <K2 extends K, V2 extends V, CV2 extends Collection<V2>> 
	void addAll(CollectionMap<K2, V2, CV2> listMap) {
		Set<Entry<K2, CV2>> entries = listMap.entrySet();
		for (Entry<K2, CV2> entry : entries) {
			addAll(entry.getKey(), entry.getValue());
		}
	}

	/**
	 * Add value to the collection for key -- if it is not already present.
	 * 
	 * @param key
	 * @param value
	 * @return true if value was added
	 */
	public boolean addOnce(K key, V value) {
		CV list = get(key);
		if (list == null) {
			list = newList(-1);
			put(key, list);
		} else if (list.contains(value))
			return false;
		list.add(value);
		return true;
	}

	

	
	/**
	 * Return the first item for a given key
	 * 
	 * @param key
	 * @return
	 */
	public V getOne(K key) {
		CV list = get(key);
		if (list == null || list.isEmpty()) {
			return null;
		}
		return list instanceof List? ((List<V>)list).get(0) : list.iterator().next();
	}

	/**
	 * @return a new set of all the values from all lists. Can be edited without
	 *         effect
	 */
	public Set<V> getValueSet() {
		HashSet<V> vs = new HashSet<V>();
		for (CV vl : values()) {
			vs.addAll(vl);
		}
		return vs;
	}

	/**
	 * Put key=value, replacing any previous list of values
	 * 
	 * @param key
	 * @param value
	 * @see #add(Object, Object)
	 */
	public void putOne(K key, V value) {
		CV list = newList(2);
		list.add(value);
		put(key, list);
	}

	/**
	 * Remove this value from the list for key. This will also remove the key if
	 * it now has no values.
	 * 
	 * @param key
	 * @param value
	 * @return true if the map was changed
	 */
	public boolean removeOne(K key, V value) {
		CV list = get(key);
		if (list == null)
			return false;
		boolean mod = list.remove(value);
//		int i = list.indexOf(value);
//		if (i == -1)
//			return false;
//		list.remove(i);
		// Remove key if the list is now empty
		if (list.isEmpty()) {
			remove(key);
		}
		return mod;
	}

	/**
	 * @param key
	 * @return the size of the collection stored for a key
	 */
	public int size(K key) {
		CV vs = get(key);
		return vs == null ? 0 : vs.size();
	}

	/**
	 * Return the total number of values stored for any key. This is costly. And
	 * perhaps of dubious value.
	 * 
	 * @return
	 */
	public int valueSize() {
		int total = 0;
		for (CV vl : values()) {
			total += vl.size();
		}
		return total;
	}

	/**
	 * @deprecated Perfectly fine, but this is probably not what you want.
	 * @see #containsValueItem(Object)
	 */
	@Override
	public boolean containsValue(Object value) {
		return super.containsValue(value);
	}

	/**
	 * @param value
	 * @return true if any of the lists contain this value
	 */
	public boolean containsValueItem(V value) {
		for (CV list : values()) {
			if (list.contains(value))
				return true;
		}
		return false;
	}

}
