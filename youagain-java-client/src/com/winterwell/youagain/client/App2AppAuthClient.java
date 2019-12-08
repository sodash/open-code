package com.winterwell.youagain.client;

import com.winterwell.utils.Dep;
import com.winterwell.utils.ReflectionUtils;
import com.winterwell.utils.log.Log;


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

	
	private String LOGTAG = "A2A";

	/**
	 * 
	 * @param appAuthName
	 * @param appAuthPassword
	 * @return "This is MyApp, signed YA"
	 * 
	 * TODO also support self-signed, "This is MyApp, signed MyApp"
	 */
	public AuthToken getIdentityTokenFromYA(String appAuthName, String appAuthPassword) {
		YouAgainClient yac = Dep.get(YouAgainClient.class);
		if (appAuthName==null || appAuthPassword==null) {
			Log.w(LOGTAG, "missing appAuthName / appAuthPassword "+ReflectionUtils.getSomeStack(8));
			return null;
		}
		AuthToken auth = yac.login(appAuthName, appAuthPassword);
		return auth;
	}
	
	public AuthToken registerIdentityTokenWithYA(String appAuthName, String appAuthPassword) {
		YouAgainClient yac = Dep.get(YouAgainClient.class);
		if (appAuthName==null || appAuthPassword==null) {
			Log.w(LOGTAG, "missing appAuthName / appAuthPassword "+ReflectionUtils.getSomeStack(8));
			return null;
		}
		AuthToken auth = yac.register(appAuthName, appAuthPassword);
		return auth;
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
	
}
