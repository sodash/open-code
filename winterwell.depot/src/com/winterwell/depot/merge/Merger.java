package com.winterwell.depot.merge;

import java.lang.reflect.Array;
import java.util.List;
import java.util.Map;

import com.winterwell.depot.Desc;
import com.winterwell.utils.containers.Containers;
import com.winterwell.utils.log.Log;

/**
 * Collect together all the standard mergers.
 * @author daniel
 * @testedby  MergerTest}
 */
public class Merger implements IMerger<Object> {

	/**
	 * Convenience for use in unit-tests. assert two objects are the same
	 *  -- and use the diff as a helpful error message if they are not.
	 * @param a
	 * @param b
	 */
	public void assertSame(Object a, Object b) {
		Diff diff = diff(a, b);
		if (diff==null) return;
		assert false : diff;
	}
	
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
		if (mergers.get(Boolean.class)==null) {
			addMerge(Boolean.class, new SimpleMerger());
		}
		if (false && mergers.get(String.class)==null) {
			addMerge(String.class, new StringMerger());
		}
		// TODO POJOMerger can cause problems by intercepting eg Boolean
		if (false && mergers.get(Object.class)==null) {
			addMerge(Object.class, new POJOMerger(this));
		}
	}

	public void addMerge(Class handles, IMerger merger) {
		IMerger was = mergers.put(handles, merger);
		mergers.put(merger.getClass(), merger);
		if (was !=null && was != merger) {
			Log.d(TAG, "Replaced merger for "+handles+" "+was+" -> "+merger);
		}
	}

	public ClassMap<IMerger> getMergers() {
		return mergers;
	}

	/**
	 * 
	 * @param before
	 * @param after
	 * @param latest
	 * @return latest + diff(before -> after)
	 */
	public Object doMerge(Object before, Object after, Object latest) {
		if (latest==null) return after;
		Class type = after.getClass();
		if (type.isArray()) {
			type = List.class;
			before = Containers.asList(before);			
			after = Containers.asList(after);
			latest = Containers.asList(latest);
		}
		IMerger m = mergers.get(type);
		if (m==null) {
			Log.e(TAG, "No merger for "+type+" - returning unmodified after");
			return after;
		}
		return m.doMerge(before, after, latest);
	}

	@Override
	public Diff diff(Object before, Object after) {
		Class type = after.getClass();
		IMerger m = mergers.get(type);
		if (m==null) {
			m = new SimpleMerger();
			addMerge(type, m);
			Log.w(TAG, new IllegalStateException("No merger for "+type
					+" (swallowed error, and using SimpleMerger)"
					+" after: "+after
					));
		}
		return m.diff(before, after);
	}

	@Override
	public Object applyDiff(Object a, Diff diff) {
		if (diff==null) return a;
		if (a==null) {
			Object v = stripDiffs(diff);
			return v;
		}
		Class type = a.getClass();
		IMerger m = mergers.get(diff.mergerClass);		
		if (m==null) {
			m = mergers.get(type);
			Log.e(TAG, "No merger for "+diff);
			if (m==null) {
				throw new IllegalStateException("No merger for "+type+" in "+diff);
			}
		}
		return m.applyDiff(a, diff);
	}

	@Override
	public Object stripDiffs(Object v) {
		if (v instanceof Diff) {
			return stripDiffs(((Diff) v).diff);
		}
		// ??Is this right??
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
