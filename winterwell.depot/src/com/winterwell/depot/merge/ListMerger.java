package com.winterwell.depot.merge;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.winterwell.utils.Utils;

/**
 * Diff and merge for lists.
 * @testedby  ListMergerTest}
 * @author daniel
 */
public class ListMerger<X> extends AMerger<List<X>> {

	private static final Object NULL = "null";

	public ListMerger(Merger merger) {
		super(merger);		
	}
	
	@Override
	public Diff<List> diff(List<X> before, List<X> after) {
		if (before==null) before = Collections.EMPTY_LIST;
		List diff = new ArrayList();
		int dCnt = 0;
		for(int i=0,n=after.size(); i<n; i++) {
			X bi = before.size()>i? before.get(i) : null;
			X ai = after.get(i);
			if (Utils.equals(ai, bi)) {
				diff.add(null);
				continue;
			}
			if (ai==null) {
				diff.add(NULL);
				dCnt++;
				continue;
			}
			if (bi==null) {
				diff.add(ai);
				dCnt++;
				continue;
			}
			// different class?
			if (ai.getClass() != bi.getClass()) {
				diff.add(ai);
				dCnt++;
				continue;
			}
			// recurse
			IMerger m = recursiveMerger;
			if (m!=null) {
				Object rDiff = m.diff(bi, ai);
				if (rDiff==null) {
					diff.add(null);
				} else {
					diff.add(rDiff);
					dCnt++;
				}
				continue;
			}
			
			// otherwise set
			dCnt++;
			diff.add(ai);
		}
		if (dCnt==0) return null;
		return new Diff<List>(getClass(),diff);
	}

	@Override
	public List<X> applyDiff(List<X> a, Diff _diff) {
		if (a==null) {
			a = new ArrayList();
		}
		List<Object> diff = (List) _diff.diff;		
		for (int i = 0; i < diff.size(); i++) {
			Object v = diff.get(i);
			if (v==null) continue;
			// special null marker?
			if (v==NULL) {
				a.set(i, null);
				continue;
			}			
			// recurse?
			if (v instanceof Diff) {
				Object incumbent = a.get(i);
				Object i2 = applySubDiff(incumbent, (Diff)v);
				a.set(i, (X) i2);
				continue;
			}			
			// otherwise set
			a.set(i, (X) v);
		}
		return a;
	}
	
	@Override
	public List<X> stripDiffs(List diffList) {
		ArrayList clean = new ArrayList(diffList.size());
		for(Object v : diffList) {			
			if (v instanceof Diff) {
				// Strip the diff
				v = ((Diff) v).diff;
				// ...and possibly recurse
			}			
			if (v==null) {
				// Well that's bogus!
				clean.add(null);
				continue;				
			}
			// recurse?
			IMerger m = recursiveMerger;
			if (m!=null) {
				v = m.stripDiffs(v);
			}
			
			clean.add(v);
		}
		return clean;
	}
	
}
