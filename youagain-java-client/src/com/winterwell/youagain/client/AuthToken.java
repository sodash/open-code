package com.winterwell.youagain.client;

import java.util.Map;

import javax.annotation.Generated;

import com.winterwell.utils.Utils;
import com.winterwell.web.data.XId;

/**
 * TODO merge with DBAuth and make both JWT token based
 * 
 * A token identifying and authorising.
 * @author daniel
 */
public class AuthToken {

	public String app;
	
	/**
	 * @return a JWT token
	 */
	public String getToken() {
		if (token==null) {			
			try {
				// this shouldn't happen, but I guess its OK to make a fresh token
				token = new JWTEncoder(app).encryptJWT(xid);
			} catch (Exception e) {
				throw Utils.runtime(e);
			}
		}
		return token;
	}
	
	public AuthToken(String token) {
		this.token = token;
	}
	public AuthToken(Map jsonObj) {
		this.token = (String) jsonObj.get("jwt");
		this.xid = XId.xid(jsonObj.get("xid"));
	}
	/**
	 * A token which can be verified with the YouAgain server.
	 */
	String token;
	/**
	 * Who is this for?
	 */
	public XId xid;
	
	@Override
	public String toString() {
		return "AuthToken["+xid+"]";
	}
	
}
