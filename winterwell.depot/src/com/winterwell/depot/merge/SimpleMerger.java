package com.winterwell.depot.merge;

import java.util.Collection;

import com.winterwell.utils.Utils;

/**
 * The diff value wins if set. i.e. object + diff = diff.value.
 * 
 *  This is the default patch merger (its the opposite of {@link UseLatestMerger}).
 * @author daniel
 *
 */
public class SimpleMerger extends AMerger<Object> {

	@Override
	public Diff diff(Object before, Object after) {
		return new Diff(SimpleMerger.class, after);		
	}

	@Override
	public Object applyDiff(Object latest, Diff diff) {
		// value wins if set
		if (diff.diff!=null) return diff.diff;
		return latest;
	}

	@Override
	public Object stripDiffs(Object v) {
		return v;
	}

}
