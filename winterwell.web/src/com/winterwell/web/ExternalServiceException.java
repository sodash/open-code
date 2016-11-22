package com.winterwell.web;



/**
 * @deprecated Overlaps with WebEx -- use that for preference.
 * 
 * Exception for when a request to an external service (e.g. GMail or Twitter)
 * fails.
 * 
 * @author Daniel
 */
public class ExternalServiceException extends RuntimeException {

	/**
	 * For security problems.
	 * 
	 * @author daniel
	 */
	public static class Security extends ExternalServiceException {
		private static final long serialVersionUID = 1L;
	}
	
	public static class Networking extends ExternalServiceException {
		private static final long serialVersionUID = 1L;
		public Networking(String string, Throwable cause) {
			super(string,cause);
		}

	}

	private static final long serialVersionUID = 42L;

	public ExternalServiceException() {
		this("");
	}

	public ExternalServiceException(Throwable e) {
		super(e);
	}

	public ExternalServiceException(String msg) {
		super(msg);
	}

	public ExternalServiceException(String msg, Exception e) {
		super(msg, e);
	}
	
	public ExternalServiceException(String msg, Throwable e) {
		super(msg, e);
	}

	

}
