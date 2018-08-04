package com.winterwell.web.data;

<<<<<<< HEAD
/**
 * Interface for objects with an id.
 * 
 * Objects implementing this SHOULD have a field <code>String xid</code>
 * which uses the toString() value of an XId
 * 
 * @author daniel
 *
 */
=======
>>>>>>> 22858d9a0f31a99040192ffea1414b154014af3e
public interface IHasXId {

	XId getXId();

<<<<<<< HEAD
=======
	public default String getService() {
		XId xid = getXId();
		return xid==null? null : xid.getService();
	}
	
>>>>>>> 22858d9a0f31a99040192ffea1414b154014af3e
}
