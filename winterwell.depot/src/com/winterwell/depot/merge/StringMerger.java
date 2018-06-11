package com.winterwell.depot.merge;

import com.winterwell.utils.TodoException;

/**
 * TODO
 * @author daniel
 *
 */
public class StringMerger extends AMerger<String> {

	public StringMerger() {
		super(null); 
	}

	@Override
	public Diff diff(String before, String after) {		
		throw new TodoException();
	}

	@Override
	public String applyDiff(String a, Diff diff) {
		throw new TodoException();
	}

	@Override
	public String stripDiffs(String v) {
		return v;
	}

}
