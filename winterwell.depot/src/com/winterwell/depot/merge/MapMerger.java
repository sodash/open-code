package com.winterwell.depot.merge;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import winterwell.utils.Utils;
import winterwell.utils.containers.Containers;

import com.winterwell.depot.Desc;
import com.winterwell.utils.ReflectionUtils;

/**
 * Diff and merge for maps
 * @testedby MapMergerTest
 * @author daniel
 */
public class MapMerger<K,V> extends AMerger<Map<K,V>> implements IMerger<Map<K,V>> {

	/**
	 * Special remove marker. 
	 * Note: we use object equality == to test for this.
	 * Which is NOT serialisation safe! But it does avoid accidents from clashes. 
	 * TODO use equals() with protection.
	 */
	private static final String REMOVE = "!remove!";
	static final Pattern REMOVEREGEX = Pattern.compile("_*"+REMOVE);
	private static final Class SINGLETON_MAP = Collections.singletonMap("k", "v").getClass();
	private static final Class EMPTY_MAP = Collections.emptyMap().getClass();
	
	public MapMerger(ClassMap<IMerger> mergers) {
		super(mergers);
	}
	
	public MapMerger() {
		addMerge(Map.class, this);
		initStdMergers();
	}



	@Override
	public boolean useMerge(Desc<? extends Map<K,V>> desc) {
		assert ReflectionUtils.isa(desc.getType(), Map.class);
		return true;
	}

	

	@Override
	public Diff<Map> diff(Map<K,V> before, Map<K,V> after) {
		if (before==null) return null;
		HashMap diff = new HashMap();
		// removals?
		for(Object k : before.keySet()) {
			if ( ! after.containsKey(k)) {
				diff.put(k, REMOVE);
			}
		}
		for(Map.Entry me : after.entrySet()) {
			Object k = me.getKey();
			Object v = me.getValue();
			Object bv = before.get(k);
			// No change?			
			if (Utils.equals(v, bv)) continue;
			if (v==null) {
				diff.put(k, REMOVE);
				continue;
			}
			if (bv==null) {				
				diff.put(k, doNotRemove(v));
				continue;
			}			
			
			// different class?
			if (v.getClass() != bv.getClass()) {
				diff.put(k, doNotRemove(v));
				continue;
			}

			// Give arrays some special treatment
			Class<? extends Object> vClass = v.getClass();
			if (vClass.isArray()) {
				// equals fails, so check
				// NB: we could get a slight speed gain by using Arrays.equals() with a big class-cast switch
				List<Object> vl = Containers.asList(v);
				List<Object> bvl = Containers.asList(bv);
				if (vl.equals(bvl)) {
					continue;
				}
				// recurse on Array
				vClass = java.lang.reflect.Array.class;
			}			
			
			// recurse?
			IMerger m = getMerger(vClass);
			if (m!=null) {
				Diff rDiff = m.diff(bv, v);				
				if (rDiff==null) continue;
				diff.put(k, rDiff);
				continue;
			}
			
			// otherwise set
			diff.put(k, doNotRemove(v));
		}
		return diff.isEmpty()? null : new Diff<Map>(getClass(),diff);
	}
	
	/**
	 * Escape REMOVE in the very unlikely event that it occurs.
	 * @param v
	 * @return
	 */
	private Object doNotRemove(Object v) {
		if (v instanceof String) {
			if (REMOVEREGEX.matcher((String)v).matches()) {
				return "_"+v;
			}
		}
		return v;
	}

	@Override
	public Map applyDiff(Map a, Diff _diff) {
		Map<Object,Object> diff = (Map) _diff.diff;
		// Handle odd small cases
		if (a==null) return diff;
		if (ReflectionUtils.isa(a.getClass(), EMPTY_MAP)) {
			return stripDiffs((Map)diff);
		}
		if (ReflectionUtils.isa(a.getClass(), SINGLETON_MAP)) {
			// annoyingly, singleton-map doesn't support put even when the keys match
			Object key = a.keySet().iterator().next();
			Map a2 = stripDiffs((Map)diff);
			// Now set the incumbent value
			if (diff.containsKey(key)) {
				// recurse?
				Object v = diff.get(key);
				if (v instanceof Diff) {
					Object incumbent = a.get(key);
					Object i2 = applySubDiff(incumbent, (Diff)v);
					a2.put(key, i2);
				}
			} else {
				a2.putAll(a);
			}
			return a2;
		}
		
		// normal map-merge loop
		for(Map.Entry me : diff.entrySet()) {
			Object k = me.getKey();
			Object v = me.getValue();
			// special remove marker?
			if (v instanceof String && REMOVEREGEX.matcher((String)v).matches()) {
				String sv = (String) v;
				if (sv.length()==REMOVE.length()) {
					a.remove(k);
					continue;
				} else {
					v = sv.substring(1);
				}
			} 
//			// add numbers Should we have this here? Using Containers.plus is a bit more thread safe.
			// But no: if you need thread safety, you must not do it half-heartedly.

			// recurse on others?
			if (v instanceof Diff) {
				Object incumbent = a.get(k);
				Object i2 = applySubDiff(incumbent, (Diff)v);
				a.put(k, i2);
				continue;
			}			
			// otherwise set
			a.put(k, v);
		}
		return a;
	}


	/**
	 * {@inheritDoc}
	 * 
	 * WARNING: MapMerger.REMOVE "commands" will be stripped out. 
	 * So if you want to handle removes, you must find them first yourself!
	 */
	@Override
	public Map<K, V> stripDiffs(Map<K, V> diffMap) {
		// Is this almost the same as applyDiff() to an empty map??
		Map copy = new HashMap(diffMap.size());
		for(Map.Entry me : diffMap.entrySet()) {
			Object v = me.getValue();			
			if (v instanceof Diff) {
				// Strip the diff
				v = ((Diff) v).diff;
				// ...and possibly recurse
			}
			if (v==null) continue;
			if (v instanceof Map) {
				v = stripDiffs((Map)v);
			}
			// recurse?			
			Class<? extends Object> vClass = v.getClass();
			IMerger m = getMerger(vClass);
			if (m!=null) {
				v = m.stripDiffs(v);
			}
			
			// Bleurgh: catch removes
			if (v instanceof String && REMOVEREGEX.matcher((String)v).matches()) {
				String sv = (String) v;
				if (sv.length()==REMOVE.length()) {
					// skip it! If you want to handle removes, you must do so yourself!
					continue;
				} else {
					v = sv.substring(1);
				}
			}
			
			copy.put(me.getKey(), v);
		}
		return copy;
	}

	/**
	 * Find the removed properties.
	 * WARNING: does not recurse into Lists, only nested Maps.
	 * @param diffMap as output by {@link #diff(Map, Map)}.diff
	 * @return e.g. if we had {a:"!remove!", b1: {b2:"!remove!"}}, then this would return [[a], [b1,b2]].
	 * Can be empty, never null.
	 */
	public List<Object[]> getRemovedProperties(Diff<Map> diff) {
		List<Object[]> removedProperties = new ArrayList();
		getRemovedProperties2(diff.diff, new ArrayList(), removedProperties);
		return removedProperties;
	}
	
	void getRemovedProperties2(Map<?, ?> diffMap, List<Object> prefix, List<Object[]> removedProperties) {
		for(Map.Entry me : diffMap.entrySet()) {
			Object v = me.getValue();
			if (v==null) continue;
			// Catch removes
			if (REMOVE.equals(v)) {
				prefix.add(me.getKey());
				removedProperties.add(prefix.toArray());
				prefix.remove(prefix.size()-1);
				continue;
			}
			if (v instanceof Diff) {
				// Strip the diff
				v = ((Diff) v).diff;
				// ...and possibly recurse
			}
			if (v instanceof Map) {
				prefix.add(me.getKey());
				getRemovedProperties2((Map)v, prefix, removedProperties);
				prefix.remove(prefix.size()-1);
				continue;
			}
			// FIXME recurse into Lists?			
		}
	}
	
}
