package com.winterwell.web.app;

/**
 * Used by webappbase. DataLogConfig implements this (hence why its here, to avoid a dependency cycle)
 * @author daniel
 *
 */
public interface ISiteConfig {

	int getPort();

	default String getAppAuthPassword() {return null;}


	/**
	 * Allows the server to verify itself with You-Again.
	 * 
	 * NB: appAuthJwt can be set in a .properties file. 
	 * Or this can return null, and AppUtils#initAppAuth() will call YouAgainClient.loadLocal()
	 */
	default String getAppAuthJWT() {
		return null;
	}
	
	/**
	 * Override to allow setting local / test / production from a .properties file.
	 * 
	 * ??Callers should use BuildHacks#getServerType() instead, which checks this plus
	 * other info. (hm - a config that shouldnt be used directly - that's inelegant)
	 * @return KServerType
	 */
	default Object getServerType2() {
		return null;
	}
}
