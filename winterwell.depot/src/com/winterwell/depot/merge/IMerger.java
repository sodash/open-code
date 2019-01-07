package com.winterwell.depot.merge;

import com.winterwell.depot.Desc;
import com.winterwell.depot.INotOverwritable;

/**
 * Merge = merge concurrent edits.
 * 
 * This is for cluster-wide systems, so that multiple servers
 * can edit an object at once. However, the standard mergers do not have any dependencies,
 * so they can be used for other purposes too.
 * 
 * @author daniel
 *
 * @param <X>
 */
public interface IMerger<X> {

	/**
	 * You can assume that this merger has already been selected to fit with desc.getType().
	 * @param desc
	 * @return normally true. But this does allow for fine-grained merger switching.
	 */
	boolean useMerge(Desc<? extends X> desc);
	
	/**
	 * Perform a merge operation.
	 * @param before Snapshot of the object when it was fetched. Can be null.
	 * @param after The object that is being saved. Never null.
	 * @param latest A freshly fetched version. Never null.
	 * @return The merge = latest + (after - before). <br>
	 * This can be a modified version of `after`
	 *  (and must be for objects implementing {@link INotOverwritable}).
	 */
	X doMerge(X before, X after, X latest);
	
	/**
	 * 
	 * @param before Can be null
	 * @param after Can be null
	 * @return Diff or null if no difference
	 */
	Diff diff(X before, X after);
	
	/**
	 * 
	 * @param a
	 * @param diff
	 * @return a+diff. This can be a modified version of `a`
	 *  (and must be for objects implementing {@link INotOverwritable}).
	 */
	X applyDiff(X a, Diff diff);

	/**
	 * Remove any Diff wrappers this merger creates.
	 * @param v
	 * @return v without the Diff wrapper objects
	 */
	X stripDiffs(X v);
}
