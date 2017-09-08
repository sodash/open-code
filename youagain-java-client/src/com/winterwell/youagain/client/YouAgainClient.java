package com.winterwell.youagain.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jetty.util.ajax.JSON;

import com.winterwell.utils.Utils;
import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.utils.containers.Containers;
import com.winterwell.utils.containers.Properties;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.web.SimpleJson;
import com.winterwell.utils.web.WebUtils2;
import com.winterwell.web.FakeBrowser;
import com.winterwell.web.app.WebRequest;
import com.winterwell.web.data.XId;
import com.winterwell.web.fields.ListField;
import com.winterwell.web.fields.SField;

public class YouAgainClient {

	static final String ENDPOINT = "https://youagain.winterwell.com/youagain.json";
	
	static ListField<String> JWT = new ListField("jwt");
	
	final String app;
	
	public YouAgainClient(String app) {
		assert ! Utils.isBlank(app);
		this.app = app;
	}	
	
	/**
	 * This will also call state.setUser()
	 * @param state
	 * @return null if not logged in at all, otherwise list of AuthTokens
	 */
	public List<AuthToken> getAuthTokens(WebRequest state) {
		List<String> jwt = getAllJWTTokens(state);
		// verify the tokens
		List<AuthToken> tokens = verify(jwt);
		String as = state.get("as");
		if (as!=null) {
			// TODO must have an auth token or be su
			XId uxid = new XId(as,false);
			Properties user = new Properties(new ArrayMap("xid", uxid));
			// set the user
			state.setUser(uxid, user);
		}
		if (jwt.isEmpty()) return null;
		return tokens;
	}
	
	List<AuthToken> verify(List<String> jwt) {
		if (jwt==null) return null;
		try {
			FakeBrowser fb = new FakeBrowser();
			Object response = fb.getPage(ENDPOINT, new ArrayMap(
					"app", app, 
					"action", "verify", 
					"jwt", jwt));
			System.out.println(response);
			return null;
		} catch(Throwable ex) {
			Log.e("youagain.verify", ex);
			return null;
		}
	}

	private List<String> getAllJWTTokens(WebRequest state) {
		Map<String, String> cookies = WebUtils2.getCookies(state.getRequest());
		Set<String> ckeys = cookies.keySet();
		Map<String, Object> params = state.getParameterMap();
		Set<String> pkeys = params.keySet();
		List<String> jwt = state.get(JWT);
		return Utils.or(jwt, new ArrayList());
	}

	public AuthToken login(String usernameUsuallyAnEmail, String password) {
		Utils.check4null(usernameUsuallyAnEmail, password);
		FakeBrowser fb = new FakeBrowser();
		Object response = fb.getPage(ENDPOINT, new ArrayMap(
				"app", app, 
				"action", "login",
				"person", usernameUsuallyAnEmail,
				"password", password));
		System.out.println(response);
		return null;
	}

	public AuthToken register(String usernameUsuallyAnEmail, String password) {
		Utils.check4null(usernameUsuallyAnEmail, password);
		FakeBrowser fb = new FakeBrowser();
		String response = fb.getPage(ENDPOINT, new ArrayMap(
				"app", app, 
				"action", "signup",
				"person", usernameUsuallyAnEmail,
				"password", password));
		Map jobj = (Map) JSON.parse(response);
		Map user = SimpleJson.get(jobj, "cargo", "user");
		return new AuthToken("TODO");
	}

}
