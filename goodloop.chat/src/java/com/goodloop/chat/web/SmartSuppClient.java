package com.goodloop.chat.web;

import java.io.File;
import java.util.Arrays;
import java.util.Map;

import com.winterwell.utils.Dep;
import com.winterwell.utils.Printer;
import com.winterwell.utils.Utils;
import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.utils.io.ConfigFactory;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.web.WebUtils2;
import com.winterwell.web.FakeBrowser;
import com.winterwell.web.app.Logins;

public class SmartSuppClient {

	private static final String LOGTAG = null;
	String chatId;
	String agentId;
	private String accessToken;
	
	public void setChatId(String chatId) {
		this.chatId = chatId;
	}
	public void setAgentId(String agentId) {
		this.agentId = agentId;
	}

	String getAccessToken() {
		if (accessToken == null) {
			ChatConfig c = Dep.get(ChatConfig.class);
			accessToken =  c.smartsupp_accessToken;
		}
		return accessToken;
	}
	private FakeBrowser fb() {
		FakeBrowser fb = new FakeBrowser();
		fb.setRequestHeader("Authorization", "Bearer "+getAccessToken());
		fb.setRequestHeader("accept", "application/json");
		return fb;
	}
	
	public Map sendReply(String text) {
		Log.d(LOGTAG, "sendReply "+text+" "+this);
		FakeBrowser fb = fb();
		Map reply = new ArrayMap(
		  "agent_id", agentId,
		  "content", new ArrayMap(
		    "type", "text",
		    "text", text
		    )		    
		);
		String jb = WebUtils2.generateJSON(reply);
		String json = fb.postJsonBody("https://api.smartsupp.com/v2/conversations/"+chatId+"/messages", jb);
		Log.d(LOGTAG, json);
		return WebUtils2.parseJSON(json);
	}

}
