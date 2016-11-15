package winterwell.utils.containers;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import winterwell.utils.IFilter;
import winterwell.utils.IFn;

import com.winterwell.datalog.Doc;
import com.winterwell.datalog.Stat;
import com.winterwell.datalog.StatTag;
import com.winterwell.utils.containers.AbstractMap2;

/**
 * A thread-safe in-memory cache which keeps the most recently used values.
 * <p>
 * This uses hard-keys & soft-values (so keys should ideally be small, but
 * values may be large).
 * <p>
 * Note: This is mostly a convenience wrapper for using {@link LinkedHashMap}
 * with synchronized + Stat counters.
 * 
 * TODO remove persistence?? Add a remove listener framework instead. TODO use
 * reference queues to clean out those nulls
 * 
 * @author daniel
 * @testedby {@link CacheTest}
 * @param <Key>
 * @param <Value>
 */
public class Cache<Key, Value> extends AbstractMap2<Key, Value> {

	/**
	 * Extract the referent
	 */
	private static final IFn<Reference, Object> GET = new IFn<Reference, Object>() {
		@Override
		public Object apply(Reference value) {
			return value == null ? null : value.get();
		}
	};

	/**
	 * We use hard-keys, mainly because weak/soft keys use === instead of
	 * equals().
	 * <p>
	 * Soft values means the cache can drop values when memory is squeezed (this
	 * is on top of dropping key+value when at capacity).
	 * <p>
	 * Type: synchronized-wrapped LinkedHashMap
	 */
	private final Map<Key, SoftReference<Value>> backing;

	/**
	 * Create a cache with the given capacity
	 * 
	 * @param capacity
	 */
	public Cache(final int capacity) {
		assert capacity > 0;
		// Hm: could we use ConcurrentHashMap somehow -- wouldn't it be faster?
		// 0.75 is the default load factor
		LinkedHashMap<Key, SoftReference<Value>> map = new LinkedHashMap<Key, SoftReference<Value>>(
				capacity + 1, .75F, true) {
			private static final long serialVersionUID = 1L;

			@Override
			public boolean removeEldestEntry(
					Map.Entry<Key, SoftReference<Value>> eldest) {
				if (size() <= capacity)
					return false;
				SoftReference<Value> val = eldest.getValue();
				// already been garbage collected?				
				if (val == null) return true;
				Value v = val.get();
				if (v == null) return true;				
				boolean ok = preRemovalCheck(eldest.getKey(), v);
				// clean up?
				if (ok) {
					onRemove(eldest.getKey(), v);
				}
				return ok;
			}
		};
		backing = Collections.synchronizedMap(map);
	}

	/**
	 * Over-ride to implement any resource cleanup required.
	 * @param key
	 * @param value Could be null
	 */
	protected void onRemove(Key key, Value value) {
		
	}

	/**
	 * Convert the key into a canonical form. E.g. you might trim and lower-case
	 * strings. The semantics of this are: If canonical(a) = canonical(b) then
	 * get(a) = get(b).
	 * <p>
	 * This does nothing by default - override this as needed.
	 * 
	 * @param key
	 * @return canonical form of key.
	 */
	public Key canonical(Key key) {
		return key;
	}

	/**
	 * Drop everything from the cache.
	 */
	@Override
	public final void clear() {
		backing.clear();
	}

	/**
	 * @return the currently cached key => value mappings.
	 */
	@Override
	public Set<java.util.Map.Entry<Key, Value>> entrySet() {
		final Set<Entry<Key, SoftReference<Value>>> es = backing.entrySet();
		return new AbstractSet<Map.Entry<Key, Value>>() {
			@Override
			public Iterator<java.util.Map.Entry<Key, Value>> iterator() {
				final Iterator<Entry<Key, SoftReference<Value>>> it = es
						.iterator();
				return new Iterator<Map.Entry<Key, Value>>() {
					@Override
					public boolean hasNext() {
						return it.hasNext();
					}

					@Override
					public java.util.Map.Entry<Key, Value> next() {
						Entry<Key, SoftReference<Value>> n = it.next();
						return new MapEntry(n.getKey(), n.getValue().get());
					}

					@Override
					public void remove() {
						it.remove();
					}
				};
			}

			@Override
			public int size() {
				return es.size();
			}
		};
	}

	/**
	 * null for no stats. If not null, acts as the tracking name.
	 */
	private String stats;

	/**
	 * Switch on (or off) Stat logging.
	 * 
	 * @param statTag Stats will be logged under "Cache_hit/statTag", "Cache_miss/statTag" and "Cache_size/statTag" 
	 * @return this
	 */
	@Doc({ @StatTag(tag = "Cache_hit/tag", doc = "Count all Cache hits (i.e. how often the get request was found in cache) with the given tag.") })
	public Cache<Key, Value> setStats(String statTag) {
		this.stats = statTag;
		// check that we have datalog on the classpath
		if (stats != null)
			Stat.get("Cache_hit", stats);
		return this;
	}

	/**
	 * @param key
	 * @return cached value or null
	 */
	@Override
	public Value get(Object key) {
		Key k = canonical((Key) key);
		SoftReference<Value> ref = backing.get(k);
		Value v = ref == null ? null : ref.get();
		if (v == null) {
			if (stats != null) {
				Stat.count(1, "Cache_miss", stats);
			}
			return null;
		} else {
			if (stats != null) {
				Stat.count(1, "Cache_hit", stats);
			}
		}
		return v;
	}

	/**
	 * Provides direct access to the backing map. For low-level convenience
	 * only.
	 */
	@Deprecated
	public Map<Key, SoftReference<Value>> getBacking() {
		return backing;
	}

	/**
	 * @return the currently cached keys.
	 */
	@Override
	public Set<Key> keySet() {
		return backing.keySet();
	}

	/**
	 * Invoked when removing an entry due to capacity issues. This can be
	 * over-ridden to implement custom behaviour. This is NOT called by
	 * {@link #remove(Object)}.
	 * <p>
	 * NB1: preRemovalCheck must not poke at the cache itself -- in general
	 * "work" should be done elsewhere. This is quite easy to do, as we found
	 * out, if, for example, you have a cache inside a depot and want to
	 * save-on-cache-removal.
	 * <p>
	 * NB2: Returning false will lead to the cache growing beyond its prescribed
	 * capacity!
	 * 
	 * @param key
	 * @param value
	 * @return true if the removal should go ahead.
	 * 
	 */
	protected boolean preRemovalCheck(Key key, Value value) {
		return true;
	}

	@Override
	public final Value put(Key k, Value v) {
		SoftReference<Value> old = backing.put(canonical(k), new SoftReference(
				v));
		// maybePersist();
		if (stats != null) {
			Stat.mean(size(), "Cache_size", stats);
		}
		return old == null ? null : old.get();
	}

	/**
	 * {@inheritDoc}
	 * @param key of type Key
	 */
	@Override
	public final Value remove(Object key) {
		Key k = canonical((Key) key);
		SoftReference<Value> old = backing.remove(k);
		Value v = old==null? null : old.get();
		onRemove(k, v);
		return v;
	}

	/**
	 * @return the current number of cached objects
	 */
	@Override
	public final int size() {
		return backing.size();
	}

	/**
	 * @return the currently cached values
	 */
	@Override
	public Collection<Value> values() {
		Collection<SoftReference<Value>> values = backing.values();
		List<Object> vals = Containers.apply(GET, values);
		return (Collection) Containers.filter(IFilter.NOT_NULL, vals);
	}

}
