package com.winterwell.depot.merge;

import java.lang.reflect.Array;
import java.util.List;
import java.util.Map;

import com.winterwell.depot.Desc;

/**
 * Useful base for {@link IMerger}s
 * @author daniel
 *
 * @param <X>
 */
public abstract class AMerger<X> implements IMerger<X> {

	/**
	 * Create a merger with it's own (initially empty) set of recursive mergers.
	 */
	public AMerger() {
		this(new ClassMap());
	}
	
	/**
	 * Handle a diff on a property by calling to another merger (or a recursive call to this merger).
	 * @param abit
	 * @param rDiff
	 * @return abit + rDiff
	 */
	protected <Y> Y applySubDiff(Y abit, Diff rDiff) {
		if (rDiff==null) return abit;
		IMerger m = getMerger(rDiff.mergerClass);
		return (Y) m.applyDiff(abit, rDiff);
	}
	
	/**
	 * Create a merger with a shared set of recursive mergers.
	 * This constructor does not add any mergers to those provided -- not even
	 * itself. E.g. if you pass in an empty map, it will not recurse.
	 * @param mergers
	 */
	public AMerger(ClassMap<IMerger> mergers) {
		this.mergers = mergers;
	}
	
	/**
	 * Number, Map and List
	 */
	protected void initStdMergers() {
		if (mergers.get(Number.class)==null) {
			addMerge(Number.class, new NumMerger());
		}
		if (mergers.get(Map.class)==null) {
			addMerge(Map.class, new MapMerger(mergers));
		}
		if (mergers.get(List.class)==null) {
			addMerge(List.class, new ListMerger(mergers));
		}
		if (mergers.get(Array.class)==null) {
			addMerge(Array.class, new ArrayMerger(mergers));
		}
	}

	public void addMerge(Class handles, IMerger merger) {
		mergers.put(handles, merger);
		mergers.put(merger.getClass(), merger);
	}

	@Override
	public boolean useMerge(Desc<? extends X> desc) {
		return true;
	}
	
	@Override
	public X doMerge(X before, X after, X latest) {
		// quick ones. latest shouldn't be null, but we may as well handle it
		if (latest==null || latest==after) return after;
		Diff diff = diff(before, after);
		// no edits
		if (diff==null) return latest;
		X merged = applyDiff(latest, diff);
		return merged;
	}
	
	protected final ClassMap<IMerger> mergers;
	
	protected IMerger getMerger(Class klass) {
		IMerger m = mergers.get(klass);
		return m;		
	}
}
