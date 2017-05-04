package com.winterwell.depot.merge;

public class NullMerger extends AMerger<Object> {

	@Override
	public Diff diff(Object nullBefore, Object after) {
		if (nullBefore!=null) throw new IllegalArgumentException(nullBefore+" is not null");
		return new Diff(NullMerger.class, after);		
	}

	@Override
	public Object applyDiff(Object a, Diff diff) {
		if (a==null) {
			// the normal case
			return diff.diff;
		}
		// oh no, the null got filled in elsewhere. So merge.
		Object merged = recursiveMerger.doMerge(null, diff.diff, a);
		return merged;
	}

	@Override
	public Object stripDiffs(Object v) {
		return v;
	}

}
