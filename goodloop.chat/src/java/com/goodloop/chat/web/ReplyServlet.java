package com.goodloop.chat.web;

import java.util.Map;

import com.winterwell.utils.Printer;
import com.winterwell.utils.Utils;
import com.winterwell.utils.containers.ArraySet;
import com.winterwell.utils.web.WebUtils2;
import com.winterwell.web.ajax.JSend;
import com.winterwell.web.ajax.KAjaxStatus;
import com.winterwell.web.app.IServlet;
import com.winterwell.web.app.WebRequest;

public class ReplyServlet implements IServlet {

	@Override
	public void process(WebRequest state) throws Exception {
		String chatId = state.get("chatId");
		String text = state.get("text");
		String type = state.get("type"); // text (or in future ux)
		String visitorId = state.get("oxid");
//		String pb = state.getPostBody();
		String agentId = "596772";
		
		SmartSuppClient ssc = new SmartSuppClient();
		ssc.setChatId(chatId);
		ssc.setAgentId(agentId);
		// TODO!
//		ssc.sendReply("Hm... What about "+
//				Utils.getRandomMember(new ArraySet<>("anacondas beetles crabs dragons elephants frogs goats hippos iguanas jaguars".split(" ")))
//			+"?");
		
		JSend jsend = new JSend();
		jsend.setStatus(KAjaxStatus.success);
		jsend.send(state);
	}

}
