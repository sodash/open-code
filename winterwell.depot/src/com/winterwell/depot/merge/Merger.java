package com.winterwell.depot.merge;

import java.lang.reflect.Array;
import java.util.List;
import java.util.Map;

import com.winterwell.depot.Desc;
import com.winterwell.utils.log.Log;

/**
 * Collect together all the standard mergers.
 * @author daniel
 *
 */
public class Merger implements IMerger<Object> {

	final ClassMap<IMerger> mergers = new ClassMap<>();
	
	public IMerger getMerger(Class klass) {
		IMerger m = mergers.get(klass);
		return m;		
	}
	
	private static final String TAG = "Merger";

	public Merger() {
		initStdMergers();
	}

	
	/**
	 * Number, Map and List
	 */
	protected void initStdMergers() {
		if (mergers.get(Number.class)==null) {
			addMerge(Number.class, new NumMerger());
		}
		if (mergers.get(Map.class)==null) {
			addMerge(Map.class, new MapMerger(this));
		}
		if (mergers.get(List.class)==null) {
			addMerge(List.class, new ListMerger(this));
		}
		// must be after ListMerger
		if (mergers.get(Array.class)==null) {
			addMerge(Array.class, new ArrayMerger(this));
		}
		if (mergers.get(Object.class)==null) {
			addMerge(Object.class, new POJOMerger(this));
		}
	}

	public void addMerge(Class handles, IMerger merger) {
		mergers.put(handles, merger);
		mergers.put(merger.getClass(), merger);
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
			return v;
		}
		return m.stripDiffs(v);
	}


	@Override
	public boolean useMerge(Desc<? extends Object> desc) {
		return true;
	}

}
