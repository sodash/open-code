package com.winterwell.depot.merge;

import java.util.ArrayList;

import com.winterwell.depot.Desc;
import com.winterwell.utils.containers.Containers;

/**
 * Useful base for {@link IMerger}s
 * @author daniel
 *
 * @param <X>
 */
public abstract class AMerger<X> implements IMerger<X> {

	protected IMerger recursiveMerger;

	/**
	 * Create a merger with NO recursive mergers.
	 * This is generally NOT the constructor you want.
	 * @see #AMerger(ClassMap)
	 */
	public AMerger() {
		this(null);
	}
	
	/**
	 * Handle a diff on a property by calling to another merger (or a recursive call to this merger).
	 * @param abit
	 * @param rDiff
	 * @return abit + rDiff
	 */
	protected <Y> Y applySubDiff(Y abit, Diff rDiff) {
		if (rDiff==null) return abit;
		IMerger m = recursiveMerger;
		return (Y) m.applyDiff(abit, rDiff);
	}
	
	/**
	 * Create a merger with a shared set of recursive mergers.
	 * This constructor does not add any mergers to those provided -- not even
	 * itself. E.g. if you pass in an empty map, it will not recurse.
	 * @param recursiveMerger Can be null (unusual)
	 */
	public AMerger(IMerger recursiveMerger) {
		this.recursiveMerger = recursiveMerger;
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
		// HACK
		if (latest.getClass().isArray()) {
			// turn into an editable list
			latest = (X) new ArrayList(Containers.asList(latest));
		}
		X merged = applyDiff(latest, diff);
		return merged;
	}
	
}
