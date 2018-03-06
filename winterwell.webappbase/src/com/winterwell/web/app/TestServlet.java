package com.winterwell.web.app;

import javax.mail.internet.InternetAddress;

import com.winterwell.utils.Dep;
import com.winterwell.utils.web.WebUtils2;
import com.winterwell.web.ajax.JsonResponse;
import com.winterwell.web.email.SimpleMessage;

public class TestServlet implements IServlet {

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
			String to = state.get("to");
			Emailer emailer = Dep.get(Emailer.class);
			SimpleMessage msg = new SimpleMessage(emailer.getFrom(), new InternetAddress(to), 
					"Test Send Email", "Hello :)");
			boolean ok = emailer.send(msg);
			WebUtils2.sendJson(new JsonResponse(state, "Now check your email for 'Test Send Email' from "+emailer.getFrom()), state);
			return;
		}		
	}

}
