package com.winterwell.web.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.winterwell.utils.containers.Containers;

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
	 * @return list of alternative XIds (usually empty, never null)
	 */
	default List<XId> getAka() {
		return Collections.EMPTY_LIST;
	}
	
	default void setAka(List<XId> aka) throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}
	
	default void addId(XId xid) {
		assert xid != null : "No xid to add! " +this;
		List<XId> aka = getAka();
		if (aka==null) {
			aka = new ArrayList();
		} else {
			// no dupes
			if (aka.contains(xid)) {
				return;
			}
			// safety copy, in case aka is e.g. Arrays.asList() which doesnt allow add()
			aka = new ArrayList(aka);
		}
		// add and set
		aka.add(xid);
		setAka(aka);	
	}

}
