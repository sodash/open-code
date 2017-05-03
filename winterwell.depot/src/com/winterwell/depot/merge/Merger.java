package com.winterwell.depot.merge;

import java.util.Map;

import com.winterwell.utils.log.Log;

/**
 * Collect together all the standard mergers.
 * @author daniel
 *
 */
public class Merger extends AMerger<Object> implements IMerger<Object> {

	private static final String TAG = "Merger";

	public Merger() {
		addMerge(Map.class, new MapMerger());
	}

	public ClassMap<IMerger> getMergers() {
		return mergers;
	}

	/**
	 * 
	 * @param before
	 * @param after
	 * @param latest
	 * @return
	 */
	public Object doMerge(Object before, Object after, Object latest) {
		if (latest==null) return after;
		Class type = after.getClass();
		IMerger m = mergers.get(type);
		if (m==null) {
			Log.e(TAG, "No merger for "+type);
			return after;
		}
		return m.doMerge(before, after, latest);
	}

	@Override
	public Diff diff(Object before, Object after) {
		Class type = after.getClass();
		IMerger m = mergers.get(type);
		if (m==null) {
			throw new IllegalStateException("No merger for "+type);
		}
		return m.diff(before, after);
	}

	@Override
	public Object applyDiff(Object a, Diff diff) {
		if (diff==null) return a;
		Class type = a.getClass();
		IMerger m = mergers.get(type);
		if (m==null) {
			throw new IllegalStateException("No merger for "+type);
		}
		return m.applyDiff(a, diff);
	}

	@Override
	public Object stripDiffs(Object v) {
		// FIXME this is wrong!!!
		Class type = v.getClass();
		IMerger m = mergers.get(type);
		if (m==null) {
			throw new IllegalStateException("No merger for "+type);
		}
		return m.stripDiffs(v);
	}

}
