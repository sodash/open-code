package com.winterwell.web.data;

import java.util.Collections;
import java.util.List;

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

	/**
	 * @return list of alternative XIds (usually empty)
	 */
	default List<XId> getAka() {
		return Collections.EMPTY_LIST;
	}
	
	default void setAka(List<XId> aka) throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}
}
