package com.winterwell.datalog;

import com.winterwell.utils.AString;
import com.winterwell.utils.StrUtils;

/**
 * A dataspace is a set of data sharing broad access rules
 *  -- e.g. data from an app or a specific CRM.
 *  
 * In DataLog, it is typically *your* app.
 * 
 * In Profiler, it is typically the website/app from which the data comes,
 * and matches with the data-controller
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
		// keep it safe. This will also spot if an XId was passed in the wrong place
		if ( ! StrUtils.isWordlike(this.name)) {
			throw new IllegalArgumentException("Invalid dataspace: "+name);
		}
	}

	public Dataspace(String name) {
		this((CharSequence)name);
	}
}
