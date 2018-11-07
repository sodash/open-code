package com.winterwell.youagain.client;

import com.winterwell.utils.io.Option;

public class YouAgainClientConfig {
	
	@Option
	public String endpoint = "https://youagain.good-loop.com/youagain.json";

	@Override
	public String toString() {
		return "YouAgainClientConfig[endpoint=" + endpoint + "]";
	}
	
	
}