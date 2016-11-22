package com.winterwell.web;

/**
 * Exception for when something hasn't been setup properly. E.g. sending email
 * without an email login.
 * <p>
 * Note that {@link ConfigException}s are ALWAYS root exceptions (i.e. they do not have a cause Exception).
 * This is so that Utils.getRootCause() will return the ConfigException. 
 * 
 * @see AuthException which extends (and largely replaces) this.
 * @testedby {@link ConfigExceptionTest}
 * @author daniel
 */
public class ConfigException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	String service;

	public ConfigException(String string) {
		super(string);
	}

	public ConfigException(String msg, String service) {
		this(msg, service, null);
	}
	
	public ConfigException(String msg, String service, Throwable cause) {
		super(service+": "+msg, cause);
		this.service = service;
	}

}
