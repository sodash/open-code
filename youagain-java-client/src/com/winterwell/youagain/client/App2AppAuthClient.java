package com.winterwell.youagain.client;

import java.util.regex.Pattern;

import com.winterwell.utils.Dep;
import com.winterwell.utils.ReflectionUtils;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.web.WebUtils2;
import com.winterwell.web.data.XId;


/**
 * 
 * Sketch code for app-to-app auth
 * NB: auth covers authentication and authorisation!
 * 
 * We want a JWT that says:
	 * "I am app A" (identity) 
	 * and 
	 * "I, Bob, give app A permission to manage T" (permission)
	 * 
	 * Notes or future complexity:
	 * 
	 * Permission could be a chain:
	 * "I Bob give app A1 permission to manage T"
	 * "I app A1 give app A2 permission to view T/2stuff"
	 * 
	 * Identity could carry witnesses, e.g.
	 * "I Alice certify that this is Bob"

 * @author daniel
 * @testedby {@link App2AppAuthClientTest}
 *
 */
public class App2AppAuthClient {

	private YouAgainClient yac;

	public App2AppAuthClient(YouAgainClient yac) {
		this.yac = yac;
	}

	private String LOGTAG = "A2A";

	/**
	 * 
	 * @param appAuthName e.g. "myapp.mydomain.com"
	 * @param appAuthPassword
	 * @return "This is MyApp (signed by YA to verify)"
	 * 
	 * TODO also support self-signed, "This is MyApp, signed MyApp"
	 */
	public AuthToken getIdentityTokenFromYA(String appAuthName, String appAuthPassword) {		
		if (appAuthName==null || appAuthPassword==null) {
			Log.w(LOGTAG, "missing appAuthName / appAuthPassword "+ReflectionUtils.getSomeStack(8));
			return null;
		}
		XId appXid = getAppXId(appAuthName);
		AuthToken auth = yac.login(appXid, appAuthPassword);
		return auth;
	}
	
	public AuthToken registerIdentityTokenWithYA(String appAuthName, String appAuthPassword) {
		if (appAuthName==null || appAuthPassword==null) {
			Log.w(LOGTAG, "missing appAuthName / appAuthPassword "+ReflectionUtils.getSomeStack(8));
			return null;
		}
		if ( ! appAuthName.contains(".")) {
			throw new IllegalArgumentException("Rejecting name: "+appAuthName+" Apps should use a valid domain name, managed by the app-owner. E.g. myapp.good-loop.com");
		}
		XId appXid = getAppXId(appAuthName);
		AuthToken auth = yac.register(appXid, appAuthPassword);
		return auth;
	}
	

	/**
	 * 
	 * @param appAuthName e.g. "my.good-loop.com"
	 * @return e.g. my.good-loop.com@app
	 */
	private XId getAppXId(String appAuthName) {
		if (appAuthName.endsWith("@app")) {
			Log.w(LOGTAG, "Already an XId (which is bad practice): "+appAuthName);
			return new XId(appAuthName);
		}
		appAuthName = canonical(appAuthName, true);
		return new XId(appAuthName, "app");
	}

	/**
	 * 
	 * @param appIdToken
	 * @param userIdAndPermissionsToken
	 * @return "This is MyApp, and TODO it has permission to do X for Bob"
	 */
	public AuthToken getPermissionsToken(AuthToken appIdToken, AuthToken userIdAndPermissionsToken) {
		// TODO	appIdToken.addClaim
		return appIdToken;
	}


	/**
	 * NB: the AppPlugin canonical code is here so it can be used by 3rd party apps 
	 * without a dependency on YA-server code.
	 * 
	 * @param appAuthName e.g. "my.good-loop.com"
	 * @return e.g. "my.good-loop.com"
	 */
	public static String canonical(String name, boolean strict) {
		String n = name.toLowerCase().trim();
		// domain name?
		if ( ! WebUtils2.URL_WEB_DOMAIN_REGEX.matcher(name).matches()) {
			if (strict) {
				throw new IllegalArgumentException("ya.app Deprecated app name: "+name+" Please use a domain name, e.g. myapp.mydomain.com");
			}
			Log.w("ya.app", "Deprecated app name: "+name+" Please use a domain name, e.g. myapp.mydomain.com");
		}
		// all good
		return n;
	}
	
	static Pattern APP_ID = Pattern.compile("[a-z0-9\\.\\-\\_]+");
	
}
