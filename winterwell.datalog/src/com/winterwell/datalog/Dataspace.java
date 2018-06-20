package com.winterwell.datalog;

import java.io.Serializable;

import com.winterwell.utils.AString;
import com.winterwell.utils.web.IHasJson;

/**
 * Just a String wrapper for some type safety
 * @author daniel
 *
 */
public final class Dataspace extends AString {
	private static final long serialVersionUID = 1L;

	/**
	 * 
	 * @param name String or Dataspace
	 */
	public Dataspace(CharSequence name) {
		super(name);
		assert ! this.name.startsWith("datalog.") : name;
	}
	
}
