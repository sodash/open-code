package com.winterwell.data;

import java.util.Map;

import com.winterwell.utils.Utils;
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
	
	public PersonLite(Map<String,Object> jobj) {
		this(new XId(
				(String) Utils.or(jobj.get("xid"), jobj.get("id"), jobj.get("@id"))
				));
		img = (String) jobj.get("img");
		description = (String) jobj.get("description");
		name = (String) jobj.get("name");
		url = (String) jobj.get("url");
	}

	public String img;
	
	public String locn;
	
	public String description;
}
