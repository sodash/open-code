package winterwell.utils.containers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @deprecated Needs thread-safe list handling
 * 
 *             Map keys to lists of values.
 *             <p>
 *             Adds a couple of useful methods to Map. Of which
 *             {@link #add(Object, Object)} and {@link #getOne(Object)} are
 *             perhaps the most useful.
 * @author daniel
 * 
 * @param <K>
 * @param <V>
 */
@Deprecated
public final class ConcurrentListMap<K, V> extends
		ConcurrentHashMap<K, List<V>> {

	private static final long serialVersionUID = 1L;

	/**
	 * Add value to the list stored for key, creating a new list if needed. You
	 * can add multiple copies of the same value.
	 * 
	 * @param key
	 * @param value
	 * @see #addOnce(Object, Object)
	 */
	public void add(K key, V value) {
		List<V> list = get(key);
		if (list == null) {
			list = new ArrayList<V>(4);
			put(key, list);
		}
		list.add(value);
	}

	public <K2 extends K, V2 extends V> void addAll(
			ConcurrentListMap<K2, V2> listMap) {
		Set<Entry<K2, List<V2>>> entries = listMap.entrySet();
		for (Entry<K2, List<V2>> entry : entries) {
			addAll(entry.getKey(), entry.getValue());
		}
	}

	/**
	 * Add a lot of values to the list for this key.
	 * 
	 * @param key
	 * @param values
	 */
	public <V2 extends V> void addAll(K key, Collection<V2> values) {
		List<V> list = get(key);
		if (list == null) {
			list = new ArrayList<V>(values.size());
			put(key, list);
		}
		list.addAll(values);
	}

	/**
	 * Add value to the collection for key -- if it is not already present.
	 * 
	 * @param key
	 * @param value
	 * @return true if value was added
	 */
	public boolean addOnce(K key, V value) {
		List<V> list = get(key);
		if (list == null) {
			list = new ArrayList<V>(4);
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
		List<V> list = get(key);
		if (list == null)
			return null;
		return list.get(0);
	}

	/**
	 * @return set of all the values from all lists
	 */
	public Set<V> getValueSet() {
		HashSet<V> vs = new HashSet<V>();
		for (List<V> vl : values()) {
			vs.addAll(vl);
		}
		return vs;
	}

	public void putOne(K key, V value) {
		ArrayList<V> list = new ArrayList<V>(2);
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
		List<V> list = get(key);
		if (list == null)
			return false;
		int i = list.indexOf(value);
		if (i == -1)
			return false;
		list.remove(i);
		// Remove key if the list is now empty
		if (list.size() == 0) {
			remove(key);
		}
		return true;
	}

	/**
	 * @param key
	 * @return the size of the collection stored for a key
	 */
	public int size(K key) {
		List<V> vs = get(key);
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
		for (List<V> vl : values()) {
			total += vl.size();
		}
		return total;
	}

}
