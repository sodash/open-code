package com.winterwell.utils.web;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.winterwell.utils.Utils;

import com.winterwell.utils.containers.AbstractMap2;
import com.winterwell.utils.containers.Pair2;

import com.winterwell.utils.log.Log;
import com.winterwell.utils.time.Dt;
import com.winterwell.utils.time.TUnit;
import com.winterwell.utils.time.Time;
import com.google.common.cache.CacheBuilder;
import com.winterwell.utils.ReflectionUtils;

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

	@Override
	public Set<K> keySet() throws UnsupportedOperationException {
		Set<K> keys = cache.keySet();
		Set<K> in = new HashSet();
		for (K k : keys) {
			if ( ! containsKey(k)) continue;
			in.add(k);
		}
		return in;
	}
	
	@Override
	public V remove(Object key) {
		Pair2<V, Time> was = cache.remove(key);
		return unwrap((K)key, was);
	}

	private V unwrap(K key, Pair2<V, Time> vt) {
		if (vt==null) return null;
		if (vt.second.isBefore(new Time())) {
			cache.remove(key);
			return null;
		}
		return vt.first;
	}

	@Override
	public boolean containsKey(Object key) {
		return get(key) != null;
	}
	
	/**
	 * Uses a Guava cache -- max storage: 1 day
	 */
	private Map<K, Pair2<V,Time>> cache;

	public ExpiringMap() {
		cache = (Map) CacheBuilder.newBuilder()
				// max storage: 1 day
			.expireAfterWrite(1, TimeUnit.DAYS)
			.build().asMap();
	}
	
	@Override
	public V get(Object arg0) {
		Pair2<V, Time> v = cache.get(arg0);
		return unwrap((K)arg0, v);
	}

	@Deprecated // The point it to give a time!
	@Override
	public V put(K key, V val) {
		return put(key, val, TUnit.MINUTE.dt);
	}

	public V put(K key, V val, Dt dt) {
		Pair2<V, Time> val_expiry = new Pair2<V, Time>(val, new Time().plus(dt));
		Pair2<V, Time> was = cache.put(key, val_expiry);
		// Suppose we displaced a more long-lasting set of the same value?
		if (was!=null && Utils.equals(val, was.first)) {
			if (was.second.isAfter(val_expiry.second)) {
				// put the old one back :(
				Log.d("ExpiringMap", "reput with shorter expiry for "+key+" = "+val+" "+ReflectionUtils.getSomeStack(8));
				cache.put(key, was);
			}
		}
		return unwrap(key, was);
	}

}
