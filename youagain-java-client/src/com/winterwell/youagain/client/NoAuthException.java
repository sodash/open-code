package com.winterwell.youagain.client;

/**
 * Thrown by servlets that require a logged in user.
 * @author daniel
 */
public final class NoAuthException extends SecurityException {
	
	public NoAuthException() {
		super();
	}
	
	public NoAuthException(Object state) {
		super(""+state);
	}

	private static final long serialVersionUID = 1L;
}
