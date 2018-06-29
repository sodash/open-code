package com.winterwell.youagain.client;

import java.util.Map;

import javax.annotation.Generated;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.winterwell.utils.Utils;
import com.winterwell.web.data.XId;

/**
 * TODO merge with DBAuth and make both JWT token based
 * 
 * A token identifying and authorising.
 * @author daniel
 */
public class AuthToken {

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((app == null) ? 0 : app.hashCode());
		result = prime * result + getXId().hashCode();
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AuthToken other = (AuthToken) obj;
		if (app == null) {
			if (other.app != null)
				return false;
		} else if (!app.equals(other.app))
			return false;
		if (xid == null) {
			if (other.xid != null)
				return false;
		} else if (!xid.equals(other.xid))
			return false;
		return true;
	}
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

	public XId getXId() {
		if (xid==null) {
			// get from token
			DecodedJWT decd = new JWTDecoder(app).decryptJWT(getToken());
			String subj = decd.getSubject();
			xid = new XId(subj, false);
		}
		return xid;
	}
	
}
