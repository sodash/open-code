package com.winterwell.datalog;

import com.winterwell.data.AThing;
import com.winterwell.es.ESNoIndex;

/**
 * Wrapper for a csv file with some metadata (like an owner).
 * The csv itself is _not_ indexed.
 * @author daniel
 *
 */
public class CSV extends AThing {	
	
	@ESNoIndex
	String csv;
	
}
