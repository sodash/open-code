package com.winterwell.youagain.client;

import com.winterwell.web.WebEx;

/**
 * Thrown by servlets that require a logged in user.
 * @author daniel
 */
public final class NoAuthException extends WebEx.E401 {
	
	public NoAuthException() {
		super(null);
	}
	
	public NoAuthException(Object state) {
		super(null, ""+state);
	}

	private static final long serialVersionUID = 1L;
}
