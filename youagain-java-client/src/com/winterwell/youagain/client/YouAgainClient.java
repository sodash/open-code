package com.winterwell.youagain.client;

import java.util.Map;
import java.util.Set;

import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.utils.containers.Properties;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.web.WebUtils2;
import com.winterwell.web.FakeBrowser;
import com.winterwell.web.app.WebRequest;
import com.winterwell.web.data.XId;
import com.winterwell.web.fields.SField;

public class YouAgainClient {

	static final String ENDPOINT = "https://youagain.winterwell.com/youagain.json";
	
	static SField JWT = new SField("jwt");
	
	String app;
	
	public YouAgainClient(String app) {
		this.app = app;
	}
	
	public Properties login(WebRequest state) {
		Map<String, String> cookies = WebUtils2.getCookies(state.getRequest());
		Set<String> ckeys = cookies.keySet();
		Map<String, Object> params = state.getParameterMap();
		Set<String> pkeys = params.keySet();
		String jwt = state.get(JWT);
		String as = state.get("as");
		if (as==null) return null;
		XId uxid = new XId(as,false);
		// FIXME Now verify it
		Map vuser = verify(jwt);
		Properties user = new Properties(new ArrayMap("xid", uxid));
		state.setUser(uxid, user);
		return user;
	}

	private Map verify(String jwt) {
		if (jwt==null) return null;
		try {
			FakeBrowser fb = new FakeBrowser();
			Object response = fb.getPage(ENDPOINT, new ArrayMap("app", app, "action", "verify", "jwt", jwt));
			System.out.println(response);
			return null;
		} catch(Throwable ex) {
			Log.e("youagain.verify", ex);
			return null;
		}
	}

}
