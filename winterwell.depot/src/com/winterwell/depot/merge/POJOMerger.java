package com.winterwell.depot.merge;

import java.util.Map;

import com.winterwell.depot.Desc;
import com.winterwell.utils.TodoException;
import com.winterwell.utils.containers.Containers;

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
		// TODO Auto-generated method stub
		throw new TodoException();
	}

	@Override
	public Object stripDiffs(Object v) {
		// TODO Auto-generated method stub
		throw new TodoException();
	}

}
