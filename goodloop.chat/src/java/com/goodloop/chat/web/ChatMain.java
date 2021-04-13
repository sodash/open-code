package com.goodloop.chat.web;

import java.util.Map;

import com.goodloop.chat.data.Chat;
import com.winterwell.data.KStatus;
import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.web.app.AMain;
import com.winterwell.web.app.AppUtils;
import com.winterwell.web.app.ISiteConfig;
import com.winterwell.web.app.JettyLauncher;
import com.winterwell.web.app.MasterServlet;

public class ChatMain extends AMain {

	public ChatMain() {
		super("chat",ChatConfig.class);
	}
	
	public static void main(String[] args) {
		ChatMain cm = new ChatMain();
		cm.doMain(args);
	}
	
	@Override
	protected void init2(ISiteConfig config) {
		super.init2(config);
		init3_youAgain();
		init3_emailer();
		init3_gson();
		init3_ES();
		
		Class[] dbclasses = new Class[] {Chat.class};
		AppUtils.initESIndices(KStatus.main(), dbclasses);
		Map<Class, Map> mappingFromClass = new ArrayMap();
		AppUtils.initESMappings(KStatus.main(), dbclasses, mappingFromClass);
	}
	
	@Override
	protected void addJettyServlets(JettyLauncher jl) {
		super.addJettyServlets(jl);
		MasterServlet ms = jl.addMasterServlet();
		ms.addServlet("chat", ChatServlet.class);
	}
}
