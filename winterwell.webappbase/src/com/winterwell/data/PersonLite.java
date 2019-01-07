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

	public String username;
	
	// NB: name and id are defined in AThing
	
	public PersonLite(XId from) {
		this.id = from.toString();
	}
	
	public PersonLite(Map<String,Object> jobj) {
		this(new XId(
				(String) Utils.or(jobj.get("xid"), jobj.get("id"), jobj.get("@id"))
				));
		setInfo(jobj);
	}

	
	
	/**
	 * NB: id= xid is already set from "xid" or "id" in the constructor.
	 * 
	 * @param jobj
	 * @return
	 */
	public PersonLite setInfo(Map<String, Object> jobj) {
		// NB: don't overwrite existing data with null
		if (jobj.containsKey("img")) img = (String) jobj.get("img");
		if (jobj.containsKey("description")) description = (String) jobj.get("description");
		if (jobj.containsKey("name")) name = (String) jobj.get("name");
		if (jobj.containsKey("username")) {
			username = (String) jobj.get("username");
		}		
		if (jobj.containsKey("url")) url = (String) jobj.get("url");
		return this;
	}

	public String img;
	
	public String locn;
	
	public String description;

	/**
	 * Convenience for {@link #getId()} with new XId(id, false).
	 * @return
	 */
	public XId getXId() {
		return new XId(getId(), false);
	}
	
}
