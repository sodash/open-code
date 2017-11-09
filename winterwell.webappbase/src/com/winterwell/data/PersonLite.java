package com.winterwell.data;

import com.winterwell.web.data.XId;

/**
 * A little info on a person, suitable for caching in other objects for fast display
 * @author daniel
 *
 */
public class PersonLite extends AThing {

	// NB: name and id are defined in AThing
	
	public PersonLite(XId from) {
		this.id = from.toString();
	}

	public String img;
	
	public String locn;
	
	public String description;
}
