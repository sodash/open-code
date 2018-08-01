package com.winterwell.web.data;

public interface IHasXId {

	XId getXId();

	public default String getService() {
		XId xid = getXId();
		return xid==null? null : xid.getService();
	}
	
}
