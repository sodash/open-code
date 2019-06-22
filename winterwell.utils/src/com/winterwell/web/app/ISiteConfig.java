package com.winterwell.web.app;

/**
 * Used by webappbase. DataLogConfig implements this (hence why its here)
 * @author daniel
 *
 */
public interface ISiteConfig {

	int getPort();

	default String getAppAuthPassword() {return null;}
	default String getAppAuthJWT() {return null;}
	
//	/** TODO
//	 * @return
//	 */
//	default String getServerType() {
//		return null;
//	}
	
	
}
