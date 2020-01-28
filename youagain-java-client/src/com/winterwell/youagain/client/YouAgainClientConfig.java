package com.winterwell.youagain.client;

import java.io.File;

import com.winterwell.utils.io.Option;

public class YouAgainClientConfig {
	
	@Option
	public File localTokenStore = new File(".token");
	
	@Option
	public String endpoint = "https://youagain.good-loop.com/youagain.json";

	@Override
	public String toString() {
		return "YouAgainClientConfig[endpoint=" + endpoint + "]";
	}
	
	
}