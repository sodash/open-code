package com.winterwell.depot.merge;

import java.util.List;

import com.winterwell.utils.containers.Containers;

/**
 * Diff and merge for arrays. Just wraps {@link ListMerger}
 * @testedby {@link ListMergerTest}
 * @author daniel
 */
public class ArrayMerger extends AMerger<Object> {

	private static final Object NULL = "null";
	private ListMerger lm;

	public ArrayMerger(ClassMap<IMerger> mergers) {
		super(mergers);
		lm = new ListMerger(mergers);
	}
	
	@Override
	public Diff diff(Object _before, Object _after) {
		List<Object> before = Containers.asList(_before);
		List<Object> after = Containers.asList(_after);
		// can't change size of an array
		if (before.size() != after.size()) {
			return new Diff(getClass(), _after);
		}
		Diff d = lm.diff(before, after);		
		return d==null? null : new Diff(getClass(), d.diff);
	}

	@Override
	public Object applyDiff(Object a, Diff _diff) {
		List<Object> la = Containers.asList(a);		
		if (_diff.diff instanceof List) {
			lm.applyDiff(la, _diff);
		} else {
			assert _diff.diff.getClass().isArray() : _diff;
			return _diff.diff;	
		}
		return a;
	}
	
	@Override
	public Object stripDiffs(Object v) {
		List<Object> lv = Containers.asList(v);
		List lclean = lm.stripDiffs(lv);
		for(int i=0; i<lclean.size(); i++) {
			lv.set(i, lclean.get(i));
		}
		return v;
	}
}
