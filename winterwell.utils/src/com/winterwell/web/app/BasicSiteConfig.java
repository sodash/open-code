package com.winterwell.web.app;

import com.winterwell.utils.io.Option;

/**
 * 
 * @author daniel
 *
 */
public class BasicSiteConfig implements ISiteConfig {

	@Option
	public int port = 8180; // Best to change this

	@Option public String appAuthPassword;
	@Option String appAuthJWT;
	

	@Override
	public String getAppAuthPassword() {
		return appAuthPassword;
	}

	@Override
	public String getAppAuthJWT() {
		return appAuthJWT;
	}


	@Override
	public int getPort() {
		return port;
	}

}
