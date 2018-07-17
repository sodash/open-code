package com.winterwell.datalog;

import java.io.Serializable;

import com.winterwell.utils.AString;
import com.winterwell.utils.StrUtils;
import com.winterwell.utils.web.IHasJson;

/**
 * A dataspace is a set of data sharing broad access rules
 *  -- e.g. data from an app or a specific CRM.
 *  
 * In DataLog, it is typically *your* app.
 * 
 * In Profiler, it is typically the website/app from which the data comes.
 * 
 * Just a String wrapper for some type safety.
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
		assert ! this.name.equals("_list") : name;
		// keep it safe
		assert StrUtils.isWordlike(this.name) : name;
	}
	
}
