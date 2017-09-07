package com.winterwell.youagain.client;

import com.winterwell.web.data.XId;

/**
 * A token identifying and authorising.
 * @author daniel
 */
public class AuthToken {

	public AuthToken(String token) {
		this.token = token;
	}
	/**
	 * A token which can be verified with the YouAgain server.
	 */
	String token;
	/**
	 * Who is this for?
	 */
	XId xid;
	
}
