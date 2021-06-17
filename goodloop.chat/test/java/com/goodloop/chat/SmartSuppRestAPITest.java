package com.goodloop.chat;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.eclipse.jetty.util.ajax.JSON;
import org.junit.Test;

import com.goodloop.chat.web.ChatConfig;
import com.winterwell.gson.Gson;
import com.winterwell.utils.Printer;
import com.winterwell.utils.Utils;
import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.utils.containers.Containers;
import com.winterwell.utils.io.ConfigFactory;
import com.winterwell.utils.web.WebUtils2;
import com.winterwell.web.FakeBrowser;
import com.winterwell.web.app.Logins;

public class SmartSuppRestAPITest {

	
	@Test
	public void testGetConversationList() {
		FakeBrowser fb = fb();
		
		ArrayMap req = new ArrayMap("size", 100);
		String jreq = WebUtils2.generateJSON(req);
		String resp = fb.postJsonBody("https://api.smartsupp.com/v2/conversations/search", jreq);
		Printer.out(resp);
		Map jobj = WebUtils2.parseJSON(resp);
		List<Map> items = Containers.asList(jobj.get("items"));
		for (Map map : items) {
			System.out.println(map.get("id"));
		}
//	{
//		  "timezone": "UTC",
//		  "size": 50,
//		  "query": [
//		    {
//		      "field": "status",
//		      "value": "open"
//		    }
//		  ],
//		  "sort": [
//		    {
//		      "createdAt": "asc"
//		    }
//		  ],
//		  "after": [
//		    1585454846490
//		  ]
//		}
	}
	
	
	@Test
	public void testGetTranscript() {
		FakeBrowser fb = fb();
		String chatId = "con987nau2GGo";
		String json = fb.getPage("https://api.smartsupp.com/v2/conversations/"+chatId+"/messages?size=50&sort=desc");
		Printer.out(json);
	}
	

	@Test
	public void testReply() {
		FakeBrowser fb = fb();
		String chatId = "con987nau2GGo";
		String agentId = "596772";
		Map reply = new ArrayMap(
		  "agent_id", agentId,
		  "content", new ArrayMap(
		    "type", "text",
		    "text", "Hm... Try "+Utils.getRandomMember(Arrays.asList("apple banana carrot dandelion eggplant".split(" ")))
		    )
		);
		String jb = WebUtils2.generateJSON(reply);
		String json = fb.postJsonBody("https://api.smartsupp.com/v2/conversations/"+chatId+"/messages", jb);
		Printer.out(json);
	}		

	private FakeBrowser fb() {
		ChatConfig c = ConfigFactory.get().setAppName("chat").getConfig(ChatConfig.class);		
		FakeBrowser fb = new FakeBrowser();
		File p = Logins.getLoginFile("chat", "chat.properties");
		fb.setRequestHeader("Authorization", "Bearer "+c.smartsupp_accessToken);
		fb.setRequestHeader("accept", "application/json");
		return fb;
	}
}
