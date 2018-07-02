package com.winterwell.depot.merge;

import java.util.Map;

import com.winterwell.utils.TodoException;
import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.utils.containers.Containers;

/**
 * Merge any object!
 * 
 * Currently, this is NOT auto selected -- you must request it via
 * merger.addMerge(class, new POJOMerger(merger));
 * 
 * @author daniel
 *
 */
public class POJOMerger extends AMerger<Object> implements IMerger<Object> {

	/**
	 * 
	 * @param merger The merger to be used for recursive calls on fields.
	 */
	public POJOMerger(Merger merger) {
		super(merger);
	}

	@Override
	public Diff diff(Object before, Object after) {
		if (before==null) before = new Object();
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
		if (v instanceof Diff) {
			Object v2 = ((Diff) v).diff;
			return stripDiffs(v2);
		}
		return recursiveMerger.stripDiffs(v);
	}

}
