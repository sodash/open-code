package com.winterwell.maths.datastorage;

import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

import com.winterwell.utils.Printer;
import com.winterwell.utils.containers.Containers;
import com.winterwell.utils.io.XStreamBinaryConverter.BinaryXML;
import com.winterwell.utils.log.Log;

/**
 * A thread-safe index which uses decaying access-counts to moderate its size.
 * <p>
 * The index values for removed entries are not recycled.
 * <p>
 * Pruning is done automatically as the index is used. Users can be notified of
 * pruning events via {@link #addListener(IPruneListener)}
 * <p>
 * Size can be as large as 2 * idealSize (see {@link #add(Object)})
 * 
 * @author daniel
 * @testedby {@link HalfLifeIndexTest}
 * @param <T>
 */
@BinaryXML
public final class HalfLifeIndex<T> implements IIndex<T>, Serializable {
	private static final long serialVersionUID = 1L;

	int devalueCount;

	int devalueInterval;
	private final int idealSize;
	final AtomicInteger index = new AtomicInteger();

	/**
	 * Having this as non-transient creates issues for the binary Java
	 * serialiser which this class uses. It uses {@link WeakReference}s to avoid
	 * keeping listeners alive.
	 */
	transient List<WeakReference<IPruneListener>> listeners;
	final Map<T, IndexEntry> map = Collections
			.synchronizedMap(new HashMap<T, IndexEntry>());// new
															// ConcurrentHashMap<T,IndexEntry>();

	final Map<Integer, T> rev = Collections
			.synchronizedMap(new HashMap<Integer, T>());// new
														// ConcurrentHashMap<Integer,T>();

	/**
	 * 
	 * @param idealSize
	 *            index size will be capped at 2 times this. Pruning reduces to
	 *            this
	 */
	public HalfLifeIndex(int idealSize) {
		this.idealSize = idealSize;
		devalueInterval = 10 * idealSize;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * This may trigger a prune event.
	 */
	@Override
	public synchronized int add(T obj) {
		assert obj != null;
		// in multi-threaded use, it's reasonable for the object
		// to sneak in during an "indexOf = -1? then add" section
		IndexEntry ie = map.get(obj);
		if (ie != null)
			return ie.index;
		int i = index.getAndIncrement();
		// add is synchronized so these should always be ==
		assert Math.abs(map.size() - rev.size()) == 0 : map.size() + " != "
				+ rev.size();

		map.put(obj, new IndexEntry(i));
		rev.put(i, obj);

		if (size() > 2 * idealSize) {
			prune();
		}
		return i;
	}

	@Override
	public synchronized void addListener(IPruneListener listener) {
		if (listeners == null) {
			listeners = new ArrayList<WeakReference<IPruneListener>>(1);
		}
		// Already listening?
		// WeakReference is an annoying class - no equals() method
		for (WeakReference<IPruneListener> wr : listeners) {
			IPruneListener v = wr.get();
			if (listener.equals(v))
				return;
		}
		listeners.add(new WeakReference(listener));
	}

	@Override
	public boolean contains(T obj) {
		return map.containsKey(obj);
	}

	void devalue() {
		// called by time or by calls to indexOf?
		for (IndexEntry e : map.values()) {
			e.count *= 0.9;
		}
		devalueCount = 0;
	}

	/**
	 * Return the element corresponding to the specified index. This may be null
	 * if the index is invalid, or has been purged.
	 */
	@Override
	public T get(int i) {
		return rev.get(i);
	}

	/**
	 * @param obj
	 * @return the current count/strength of this object. zero if the object is
	 *         not in the index (or has fallen out)
	 */
	public double getCount(T obj) {
		assert obj != null;
		IndexEntry e = map.get(obj);
		if (e == null)
			return 0;
		return e.count;
	}

	public int getIdealSize() {
		return idealSize;
	}

	@Override
	public Iterable<Integer> getIndexValues() {
		return rev.keySet();
	}

	/**
	 * CRISIS debugging memory leak
	 * 
	 * @return
	 */
	public List<WeakReference<IPruneListener>> getListeners() {
		// prune nulls
		for (WeakReference<IPruneListener> wr : listeners
				.toArray(new WeakReference[0])) {
			IPruneListener v = wr.get();
			if (v == null) {
				listeners.remove(wr);
			}
		}
		// ok
		return listeners;
	}

	/**
	 * @return the most common/strongest entries are listed first
	 */
	public List<T> getSortedEntries() {
		List<IndexEntry> entries = new ArrayList<IndexEntry>(map.values());
		Collections.sort(entries, Collections.reverseOrder());
		List<T> words = new ArrayList<T>(entries.size());
		for (IndexEntry indexEntry : entries) {
			words.add(get(indexEntry.index));
		}
		return words;
	}

	// TODO test behaviour
	public double getTotalCount() {
		double sum = 0;
		for (IndexEntry e : map.values()) {
			sum += e.count;
		}
		return sum;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * This will increase the count for this entry, and may trigger a devalue
	 * event.
	 */
	@Override
	public int indexOf(T obj) {
		if (obj == null)
			return -1;
		IndexEntry e = map.get(obj);
		if (e == null)
			return -1;
		e.count++;
		// devalue?
		devalueCount++;
		if (devalueCount > devalueInterval) {
			devalue();
		}
		return e.index;
	}

	@Override
	public int indexOfWithAdd(T obj) {
		int i = indexOf(obj);
		if (i != -1)
			return i;
		return add(obj);
	}

	/**
	 * Return an iterator over the object in this index. NB if pruning occurs
	 * the iterator will break.
	 */
	@Override
	public Iterator<T> iterator() {
		return map.keySet().iterator();
	}

	/**
	 * Prune the index down to idealSize. There is no need to call this directly
	 * - it is called automatically by {@link HalfLifeIndex}. This method is
	 * exposed for debugging/testing purposes.
	 */
	public void prune() {
		if (size() <= idealSize)
			return;
		assert map.size() == rev.size();
		Log.report("nlp", "prune index!", Level.FINE);
		// sort by score
		List<IndexEntry> entries = new ArrayList<IndexEntry>(map.values());
		if (entries.size() <= idealSize)
			return; // race condition
		Collections.sort(entries);
		List<IndexEntry> toPrune = entries.subList(0, entries.size()
				- idealSize);
		for (IndexEntry e : toPrune) {
			T obj = rev.remove(e.index);
			if (obj != null) {
				map.remove(obj);
			}
		}

		assert map.size() == rev.size();

		// emit an event
		if (listeners == null)
			return;
		List<Integer> prunedIndexes = new ArrayList(toPrune.size());
		// ?? List<T> pruned = new ArrayList<T>(toPrune.size());
		for (int i = 0; i < toPrune.size(); i++) {
			prunedIndexes.add(toPrune.get(i).index);
			// pruned.add(e);
		}
		for (int i = 0; i < listeners.size(); i++) {
			IPruneListener pl = listeners.get(i).get();
			if (pl == null) {
				continue;
			}
			pl.pruneEvent(prunedIndexes);
		}
	}

	public synchronized void removeListener(IPruneListener listener) {
		// Find if present
		WeakReference<IPruneListener> fnd = null;
		for (WeakReference<IPruneListener> wr : listeners) {
			IPruneListener v = wr.get();
			if (listener.equals(v)) {
				fnd = wr;
				break;
			}
		}
		if (fnd == null)
			return;
		fnd.clear();
		listeners.remove(fnd);
	}

	@Override
	public int size() {
		return map.size();
	}

	@Override
	public Object[] toArray() {
		return map.keySet().toArray();
	}

	@Override
	public String toString() {
		return getClass().getSimpleName()
				+ "["
				+ Printer.toString(Containers.subList(
						new ArrayList<T>(map.keySet()), 0, 3), ",") + "... "
				+ size() + "]";
	}

}

final class IndexEntry implements Comparable<IndexEntry>, Serializable {
	private static final long serialVersionUID = 1L;
	double count = 1;
	final int index;

	public IndexEntry(int i) {
		this.index = i;
	}

	@Override
	public int compareTo(IndexEntry o) {
		return Double.compare(count, o.count);
	}

	@Override
	public String toString() {
		return "IndexEntry [count=" + count + ", index=" + index + "]";
	}

}