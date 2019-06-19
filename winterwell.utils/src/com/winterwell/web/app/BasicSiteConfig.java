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

	@Override
	public int getPort() {
		return port;
	}

}
