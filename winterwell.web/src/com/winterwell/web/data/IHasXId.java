package com.winterwell.web.data;

/**
 * Interface for objects with an id.
 * 
 * Objects implementing this SHOULD have a field <code>String xid</code>
 * which uses the toString() value of an XId
 * 
 * @author daniel
 *
 */

public interface IHasXId {

	XId getXId();

	public default String getService() {
		XId xid = getXId();
		return xid==null? null : xid.getService();
	}
	
}
