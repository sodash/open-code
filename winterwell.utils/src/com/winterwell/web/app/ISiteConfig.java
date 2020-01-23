package com.winterwell.web.app;

/**
 * Used by webappbase. DataLogConfig implements this (hence why its here)
 * @author daniel
 *
 */
public interface ISiteConfig {

	int getPort();

	default String getAppAuthPassword() {return null;}


	/**
	 * Allows the server to verify itself with You-Again.
	 * 
	 * ??How to get this
	 */
	default String getAppAuthJWT() {return null;}	
	
}
