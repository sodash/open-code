package com.goodloop.chat.web;

import com.winterwell.utils.io.Option;
import com.winterwell.web.app.BasicSiteConfig;

public class ChatConfig extends BasicSiteConfig {

	public ChatConfig() {
		port = 7878;
	}
	
	@Option
	public String smartsupp_key;
	
	@Option
	public String smartsupp_accessToken;
}
