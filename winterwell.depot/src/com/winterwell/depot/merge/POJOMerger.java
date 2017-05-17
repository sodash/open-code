package com.winterwell.depot.merge;

import java.util.Map;

import com.winterwell.depot.Desc;
import com.winterwell.utils.TodoException;
import com.winterwell.utils.containers.Containers;

/**
 * Merge any object!
 * @author daniel
 *
 */
public class POJOMerger extends AMerger<Object> implements IMerger<Object> {

	public POJOMerger(Merger merger) {
		super(merger);
	}

	@Override
	public Diff diff(Object before, Object after) {
		if (before.equals(after)) return null;
		// treat as map
		Map<String, Object> bmap = Containers.objectAsMap(before);
		Map<String, Object> amap = Containers.objectAsMap(after);		
		Diff diff = recursiveMerger.diff(bmap, amap);
		if (diff==null) return null;
		return new Diff(POJOMerger.class, diff);
	}

	@Override
	public Object applyDiff(Object a, Diff diff) {
		Map<String, Object> amap = Containers.objectAsMap(a);
		Object mmap = recursiveMerger.applyDiff(amap, (Diff) diff.diff);
		if (mmap==amap) return a;
		throw new TodoException();
	}

	@Override
	public Object stripDiffs(Object v) {
		// TODO Auto-generated method stub
		throw new TodoException();
	}

}
