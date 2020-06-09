package com.winterwell.utils.web;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.winterwell.utils.ReflectionUtils;
import com.winterwell.utils.Utils;
import com.winterwell.utils.containers.AbstractMap2;
import com.winterwell.utils.containers.Cache;
import com.winterwell.utils.containers.Pair2;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.time.Dt;
import com.winterwell.utils.time.TUnit;
import com.winterwell.utils.time.Time;

/**
 * If expiry is the same for all -- use a guava cache.
 * This is for having per-key expiry times.
 * 
 * @author daniel
 *
 * @param <K>
 * @param <V>
 */
public class ExpiringMap<K, V> extends AbstractMap2<K, V> {

	private int capacity = 1000;
	
	/**
	 * Uses a cache so we hopefully don't need to worry about memory.
	 * 
	 * NB: `z` prefix to keep this below capacity in the fields
	 */
	private Map<K, Pair2<V,Time>> zCache = new Cache<>(capacity);

	public ExpiringMap() {
	}

	@Override
	public boolean containsKey(Object key) {
		return get(key) != null;
	}
	
	@Override
	public V get(Object arg0) {
		Pair2<V, Time> v = zCache.get(arg0);
		return unwrap((K)arg0, v);
	}
	@Override
	public Set<K> keySet() throws UnsupportedOperationException {
		Set<K> keys = zCache.keySet();
		Set<K> in = new HashSet();
		for (K k : keys) {
			if ( ! containsKey(k)) continue;
			in.add(k);
		}
		return in;
	}

	
	@Deprecated // The point it to give a time!
	@Override
	public V put(K key, V val) {
		return put(key, val, TUnit.MINUTE.dt);
	}
	
	public V put(K key, V val, Dt dt) {
		Pair2<V, Time> val_expiry = new Pair2<V, Time>(val, new Time().plus(dt));
		Pair2<V, Time> was = zCache.put(key, val_expiry);
		// Suppose we displaced a more long-lasting set of the same value?
		if (was!=null && Utils.equals(val, was.first)) {
			if (was.second.isAfter(val_expiry.second)) {
				// put the old one back :(
				Log.d("ExpiringMap", "reput with shorter expiry for "+key+" = "+val+" "+ReflectionUtils.getSomeStack(8));
				zCache.put(key, was);
			}
		}
		return unwrap(key, was);
	}
	
	@Override
	public V remove(Object key) {
		Pair2<V, Time> was = zCache.remove(key);
		return unwrap((K)key, was);
	}

	public void setCapacity(int c) {
		this.capacity = c;
		Map<K, Pair2<V, Time>> old = zCache;
		this.zCache = new Cache(c);
		if (old!=null) {
			this.zCache.putAll(old);
		}
	}

	private V unwrap(K key, Pair2<V, Time> vt) {
		if (vt==null) return null;
		if (vt.second.isBefore(new Time())) {
			zCache.remove(key);
			return null;
		}
		return vt.first;
	}

}
