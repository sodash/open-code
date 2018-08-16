package com.winterwell.web.app;

import java.util.List;
import java.util.Map;

import javax.mail.internet.InternetAddress;

import com.winterwell.es.client.ESHttpClient;
import com.winterwell.es.client.admin.StatsRequest;
import com.winterwell.es.client.admin.StatsResponse;
import com.winterwell.utils.Dep;
import com.winterwell.utils.containers.Containers;
import com.winterwell.utils.io.SqlUtils;
import com.winterwell.utils.web.WebUtils2;
import com.winterwell.web.ajax.JsonResponse;
import com.winterwell.web.email.SimpleMessage;
import com.winterwell.web.fields.AField;
import com.winterwell.web.fields.SField;

public class TestmeServlet implements IServlet {

	@Override
	public void process(WebRequest state) throws Exception {
		String service = state.getSlugBits(0);
		if (service==null) {
			// alive!
			WebUtils2.sendJson(new JsonResponse(state, "OK"), state);
			return;
		}
		switch(service) {		
		
		case "email":
			AField<String> TO = new SField("to").setRequired(true);
			String to = TO.getValue(state.getRequest());
			Emailer emailer = Dep.get(Emailer.class);
			SimpleMessage msg = new SimpleMessage(emailer.getFrom(), new InternetAddress(to), 
					"Test Send Email", "Hello :)");
			boolean ok = emailer.send(msg);
			WebUtils2.sendJson(new JsonResponse(state, "Now check your email for 'Test Send Email' from "+emailer.getFrom()), state);
			return;

		case "sql":
			Iterable<Object[]> rs = SqlUtils.executeQuery("select version()", null, 2);
			List<Object> list = Containers.asList(rs);
			WebUtils2.sendJson(new JsonResponse(state, list), state);
			return;
		
		case "es":
			ESHttpClient esc = Dep.get(ESHttpClient.class);
			StatsRequest sr = esc.admin().indices().listIndices();
			StatsResponse sresp = sr.get();
			WebUtils2.sendJson(new JsonResponse(state, sresp.getJsonMap()), state);
			return;
		
		case "thread":
			Map<Thread, StackTraceElement[]> threads = Thread.getAllStackTraces();
			WebUtils2.sendJson(new JsonResponse(state, threads), state);
			return;
		}		
	}

}
