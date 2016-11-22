package com.winterwell.web;

import com.winterwell.web.data.XId;

/**
 * Exception for when we don't have an authorisation token / password, or our token is invalid.
 * @author daniel
 *
 */
public class AuthException extends ConfigException {
	
	private XId xid;
	
	public XId getXid() {
		return xid;
	}
	
	public AuthException(XId xid) {
		this(xid, null);
	}

	/**
	 * 
	 * @param xid Can be null
	 * @param ex Can be null
	 */
	public AuthException(XId xid, Throwable ex) {
		this(xid+" auth failure: "+ex, xid==null? null : xid.getService());
		this.xid = xid;
	}
	
	public AuthException(String msg, String service) {
		super(msg, service);
	}
	
	public AuthException(String msg, String service, Throwable error) {
		super(msg, service, error);
	}

	public AuthException setXid(XId xid) {
		this.xid = xid;
		return this;
	}

	private static final long serialVersionUID = 1L;

	/**
	 * No auth-token / password
	 */
	public static class Unset extends AuthException {
		public Unset(XId xid) {
			super("No authorisation token for "+xid, xid==null? null : xid.getService());
		}
		private static final long serialVersionUID = 1L;		
	}
	
	public static class Expired extends AuthException {
		public Expired(String service) {
			super("The "+service+" authorisation token for this user has expired. Please refresh your authorisation.", service);
		}
		public Expired(XId xid) {
			super("The authorisation token for "+(xid == null? "this user" : xid)+" has expired. Please refresh your authorisation.", xid==null? null : xid.getService());
		}
		
		public Expired(XId xid, String displayName) {
			// Give user name as "Jane Smith (xid@service)" OR "Jane Smith" OR "this user (xid@service)" OR "this user" depending on information available
			super("The authorisation token for " +
					(displayName != null? displayName : "this user") +
					(xid != null? " (" + xid + ")" : "") +
					" has expired. Please refresh your authorisation.", xid == null? null : xid.getService());
		}
		private static final long serialVersionUID = 1L;		
	}
}
