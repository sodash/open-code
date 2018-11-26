package com.winterwell.maths.datastorage;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.winterwell.utils.Printer;
import com.winterwell.utils.containers.AbstractMap2;
import com.winterwell.utils.containers.Containers;
import com.winterwell.utils.io.XStreamBinaryConverter.BinaryXML;
import com.winterwell.utils.log.Log;

/**
* A thread-safe Map which uses decaying access-counts to moderate its size.
* This means objects will fall out of the map if it gets too big!
 * <p>
 * Pruning is done automatically as the map is used. Users can be notified of
 * pruning events via {@link #addListener(IPruneListener)}
 * <p>
 * Size can be as large as 2 * idealSize (see {@link #add(Object)})
 * <p>
 * <h3>Motivating Use Case</h3>
 * This if for when:
 * (a) we can't build the perfect map, due to memory issues.
 * (b) data is not random-access (e.g. we're responding to a stream of data), so we can't
 * use a map-reduce approach. 
 * <p>
 * A good example is counting word frequencies from a Twitter stream.
 * <p>
 * HalfLifeMap balances new v old, allowing new items to enter the cache & compete with older ones.
 * This makes it "better" than...<br>
 * 1. Value-based eviction (i.e. keep the most important/used items) would not allow new items to be learned.<br>
 * 2. A most-recent-update (MRU) cache which would ruthlessly evict older items.
 * 
 * <p><i>
 * 	Warning: This class gets used in critical bottlenecks - be careful if editing the code to consider performance.
 * </i></p>
 * 
 * <p> 
 * TODO would it be better to use an inflationary unit, instead of decaying counts?
 * So "1" goes up over time, effectively devaluing old counts. 
 * However - profiling by Carson suggests that devalue() isn't a likely bottleneck. 
 * TODO are the synchonized blocks needed?
 * @author daniel
 */
@BinaryXML // Tell our XStream converter to do this as a binary blob.
public final class HalfLifeMap<K, V> extends AbstractMap2<K, V> implements
		Serializable, Cloneable, IForget<K, V> 
//ConcurrentMap<K, V> ??
{
	private static final long serialVersionUID = 1L;

	/**
	 * The clone will initially have the same per-item decay counts as the
	 * original. However these will diverge if subsequent usage is
	 * different.
	 * The keys and values are copied directly.
	 * Listeners are NOT copied into the clone.
	 */
	@Override
	public HalfLifeMap<K,V> clone() {
		HalfLifeMap<K, V> clone = new HalfLifeMap<K, V>(idealSize);
		clone.devalueCount = devalueCount;
		for(Map.Entry<K,HLEntry<K,V>> e : map.entrySet()) {
			HLEntry<K, V> hle = e.getValue();
			HLEntry hle2 = hle.clone();
			clone.map.put(e.getKey(), hle2);
		}
		return clone;
	}
	
	/**
	 * The entries at the time of calling.<br>
	 * Entries are copied out; some of them may subsequently be removed by
	 * the action of other threads.<br>
	 * Using this does not affect the decay counts.
	 * <p>
	 * This set does not support editing!
	 * Never throws ConcurrentModificationException. Unless it fails 10x!
	 */
	@Override
	public Set<java.util.Map.Entry<K, V>> entrySet() {		
		for(int i=0; i<10; i++) {
			try {
				Object[] es = map.entrySet().toArray();
				Set<Entry<K, V>> entries = new HashSet<Entry<K, V>>(es.length);
				for (Object _e : es) {
					Map.Entry<K, HLEntry<K, V>> e = (java.util.Map.Entry<K, HLEntry<K, V>>) _e;
		//			K k = e.getKey();
					HLEntry<K, V> v = e.getValue();
					if (v==null || v.getValue()==null) continue;
					entries.add(v); //new MapEntry<K, V>(k, v.getValue(), this));
				}
				// Does not support editing
				Set<Entry<K, V>> safeEntries = Collections.unmodifiableSet(entries);
				return safeEntries;
			} catch(ConcurrentModificationException ex) {
				// ignore
			}
		}
		throw new ConcurrentModificationException("Failed x10");
	}
	
	/**
	 * @Deprecated Access to the low-level
	 */
	public Map<K, HLEntry<K, V>> getBaseMap() {
		return map;
	}
	
	/**
	 * The values in the map. Calling this does not affect the decay counts.
	 * <p>
	 * WARNING: This copies the values when it is called.
	 * It will never throw a ConcurrentModificationException, but 
	 * you are therefore NOT protected against concurrent modifications.
	 */
	@Override
	public Collection<V> values() {
		for(int i=0; i<10; i++) {
			try {
				// _can_ this throw a conc-mod-ex ??
				HLEntry<K, V>[] hvs = map.values().toArray(new HLEntry[0]);
				ArrayList<V> vs = new ArrayList(hvs.length);
				for (HLEntry<K, V> hlEntry : hvs) {
					vs.add(hlEntry.getValue());
				}		
				return Collections.unmodifiableList(vs);
			} catch(ConcurrentModificationException ex) {
				// ignore
			}
		}		
		throw new ConcurrentModificationException("Failed 10x");		
	}
	
	
	volatile int devalueCount;

	final int devalueInterval;
	private final int idealSize;

	/** Allows users to respond to pruning events. */
	List<IPruneListener<K, V>> listeners;

	/** Which base map -- Use synchronized-HashMap or ConcurrentHashMap?
	 * We expect multi-threaded gets, single-thread puts.
	 * 
	 * Crucially, we expect quite a few of these! They're used in WWModel per-context. 
	 * ConcurrentHashMap has a sig higher memory footprint :(
	 * 
	 * HalfLifeMapTest.testSpeed() suggests there's not much cost to ConcurrentHashMap
	 * in single-threaded use.
	 */
	final Map<K, HLEntry<K,V>> map;
//				new ConcurrentHashMap<K, HLEntry<K,V>>();

	/**
	 * Number of dropped entries (can double /triple/etc count if
	 * an entry is added & dropped repeatedly).
	 */
	private int pruned;
	/**
	 * The amount of value that has been dropped.
	 * This is NOT tracked by default -- it only makes sense if the
	 * values are numerical -- e.g. in a probability-distro.
	 * 
	 * TODO maybe move this upto ObjectDistribution?? 
	 */
	private double prunedValue = Double.NaN;
	
	/* (non-Javadoc)
	 * @see winterwell.maths.datastorage.IForget#setTrackPrunedValue(boolean)
	 */
	@Override
	public void setTrackPrunedValue(boolean track) {
		if (track) {
			if (Double.isNaN(prunedValue)) prunedValue = 0;
		} else {
			prunedValue = Double.NaN;
		}
	}

	/**
	 * 
	 * @param idealSize Must be > 1.
	 *            Index size will be capped at 2 times this. Pruning reduces to
	 *            this
	 */
	public HalfLifeMap(int idealSize) {
		this(idealSize, Collections.synchronizedMap(new HashMap<K,HLEntry<K,V>>()));
	}
	
	/**
	 * Use this to override the default choice of synchronized HashMap
	 * @param idealSize
	 * @param map
	 */
	public HalfLifeMap(int idealSize, Map map) {
		assert idealSize > 0 : idealSize;
		this.idealSize = idealSize;
		// NB: Why 10x? Er, it seems like a sensible number that behaves well.
		// The "best" number would depend on the ratio of gets (which devalue)
		// and fresh puts (which can trigger prunes).
		// Every devalueInterval, the decay counts are reduced by 0.9 
		// (x0.9 gives a half-life of 6-7, ie 65 x idealSize).
		devalueInterval = 10 * idealSize;
		this.map = map;		
	}

	// Status: not used?
	@Override
	public synchronized void addListener(IPruneListener<K,V> listener) {
		if (listeners == null) {
			listeners = new ArrayList(1);
		}
		// Already listening?
		if (listeners.contains(listener)) return;
		listeners.add(listener);
	}

	/**
	 * Lower all the counts. This is triggered periodically by calls to get()
	 */
	public void devalue() {
		synchronized(this) {
			if (devalueCount < devalueInterval) {
				return; // race condition -- someone else has already devalued
			}
			devalueCount = 0;
		}
		// Copy to avoid concurrent mod errors
		HLEntry<K, V>[] hvs = map.values().toArray(new HLEntry[0]);
		for (HLEntry<K, V> e : hvs) {
			e.count *= 0.9;
		}
	}

	/**
	 * get(key) boosts key's decay count by 1.
	 * @return value for key, or null.
	 */
	@Override
	public V get(Object key) {
		HLEntry<K, V> hle = map.get(key);
		if (hle==null) return null;
		hle.count++;
		// devalue?
		devalueCount++;
		if (devalueCount > devalueInterval) {
			devalue();
		}
		return hle.getValue();
	}

	/**
	 * @param obj
	 * @return the current count/strength of this object. zero if the object is
	 *         not in the index (or has fallen out)
	 */
	public double getCount(K obj) {
		assert obj != null;
		HLEntry e = map.get(obj);
		if (e == null)
			return 0;
		return e.count;
	}

	public int getIdealSize() {
		return idealSize;
	}

	public double getTotalCount() {
		double sum = 0;
		for (HLEntry e : map.values()) {
			sum += e.count;
		}
		return sum;
	}

	@Override
	public boolean containsKey(Object key) {
		// override the base method 'cos keySet() is slow, so keySet().contains is a bad idea.
		// NB: null values are not allowed - see put()
		return map.get(key) != null;
	}
	
	/**
	 * WARNING: Edits will not write-through. <br>
	 * WARNING: This copies the keys when it is called.
	 * It will never throw a ConcurrentModificationException, but 
	 * you are therefore NOT protected against concurrent modifications.
	 * <p>
	 * Calling this does not affect the decay counts.
	 */
	@Override
	public Set<K> keySet() {
		for(int i=0; i<10; i++) {
			// Bleurgh -- this _can_ throw a conc-mod
			try {
				Object[] keys = map.keySet().toArray();
				return new HashSet(Arrays.asList(keys));
			} catch(ConcurrentModificationException ex) {
				// ignore
			}
		}		
		throw new ConcurrentModificationException("Failed 10x");
	}

	@Override
	public synchronized void clear() {
		pruned=0;
		if ( ! Double.isNaN(prunedValue)) prunedValue=0;
		map.clear();
		devalueCount=0;
	}

	/**
	 * Prune the index down to idealSize. There is no need to call this directly
	 * - it is called automatically by put(). This method is
	 * exposed for debugging/testing purposes.
	 */
	public void prune() {
		if (size() <= idealSize) return;
		Log.v("map", "prune!");
		// Copy out of the backing map
		List<HLEntry> entries = Arrays.asList(map.values().toArray(new HLEntry[0]));
		if (entries.size() <= idealSize) return; // race condition
		// sort by score
		// NB: HLEntry score could mutate under us, which could cause
		// > java.lang.IllegalArgumentException: Comparison method violates its general contract!
		boolean sorted = false;
		for(int i=0; i<5; i++) {
			try {
				Collections.sort(entries);
				sorted = true;
				break;
			} catch(IllegalArgumentException ex) {
				// try again...
				Log.d("HalfLifeMap", "Rare concurrency issue: "+ex);
			}
		}
		if ( ! sorted) {
			// failed x5?! Copy them out instead
			entries = entries.stream().map(hle -> hle.clone()).collect(Collectors.toList());
			Collections.sort(entries);
		}
		// What to prune?
		List<HLEntry> toPrune = entries.subList(0, entries.size() - idealSize);
		// prune
		boolean track = ! Double.isNaN(prunedValue);
		for (HLEntry e : toPrune) {
			// actually prune
			HLEntry<K, V> v = map.remove(e.key);			
			// track what we forget
			if (track && v != null && v.getValue()!=null) {
				double pv = ((Number)v.getValue()).doubleValue();
				// At overflow, just sit on max-value
				// TODO do we want thread safety here?
				prunedValue = Math.min(Double.MAX_VALUE, prunedValue + pv);
				assert ! Double.isNaN(prunedValue);
			}
		}
		
		// ...track this anyway
		pruned += toPrune.size();			
	
		// emit an event
		if (listeners == null)
			return;		
		for (int i = 0; i < listeners.size(); i++) {
			IPruneListener pl = listeners.get(i);
			pl.pruneEvent(toPrune);
		}
	}

	@Override
	public int getPrunedCount() {
		return pruned;
	}

	@Override
	public double getPrunedValue() {
		return prunedValue;
	}
	
	/**
	 * {@inheritDoc}
	 * 
	 * This may trigger a prune event.
	 * put(k,v) boosts k's decay count by 1.
	 * Null values are NOT allowed
	 */
	@Override
	public V put(K key, V val) {
		assert key != null;
		assert val != null : key;
		HLEntry ie = new HLEntry(key, val);
		HLEntry<K, V> old = map.put(key, ie);		
		if (old!=null) {
			// Add the old count (so a put gives a +1 to the key)
			// ?? is this desirable? Would it be "better" to just transfer the count?
			ie.count += old.count;
		}
		// Prune?
		if (size() > 2 * idealSize) {
			prune();
		}
		return old == null? null : old.getValue();
	}
	
	@Override
	public V remove(Object key) {
		assert key != null;
		HLEntry<K, V> v = map.remove(key);
		return v==null? null : v.getValue();
	}


	
	@Override
	public synchronized void removeListener(IPruneListener listener) {
		boolean ok = listeners.remove(listener);
		return;
	}

	@Override
	public int size() {
		return map.size();
	}

	@Override
	public String toString() {
		return getClass().getSimpleName()
				+ "["
				+ Printer.toString(Containers.subList(
						new ArrayList<K>(map.keySet()), 0, 3), ",") + "... "
				+ size() + "]";
	}

}

/**
 * The value wrapper for HalfLifeMap.
 * Key & value are immutable, but the decay-count can change.
 * @author daniel
 */
final class HLEntry<K, V> implements Comparable<HLEntry>, Serializable, Cloneable, Map.Entry<K, V> {
 
	@Override
	protected HLEntry<K,V> clone() {
		HLEntry hle = new HLEntry(key, val);
		hle.count = count;
		return hle;
	}
	
	private static final long serialVersionUID = 1L;
	volatile double count = 1;
	final K key;

	// should this be volatile for setValue?? 
	private V val;

	public HLEntry(K key, V val) {
		this.key = key;
		this.val = val;
		assert key != null && val != null : key+":"+val;
	}

	@Override
	public int compareTo(HLEntry o) {
		return Double.compare(count, o.count);
	}

	public V getValue() {
		return val;
	}

	@Override
	public String toString() {
		return "HLEntry [count=" + count + ", val=" + val + "]";
	}

	@Override
	public K getKey() {
		return key;
	}


	@Override
	public V setValue(V value) {
		V old = val;
		this.val = value;
		return old;
	}

}
