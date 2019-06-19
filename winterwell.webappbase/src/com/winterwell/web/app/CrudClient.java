package com.winterwell.web.app;

import java.util.Map;

import com.winterwell.data.AThing;
import com.winterwell.gson.Gson;
import com.winterwell.utils.Dep;
import com.winterwell.utils.Utils;
import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.utils.web.WebUtils;
import com.winterwell.web.FakeBrowser;
import com.winterwell.web.ajax.JSend;
import com.winterwell.web.ajax.JThing;

/**
 * Status: WIP 
 * A java client for working with data managed by a {@link CrudServlet}
 * @author daniel
 *
 * @param <T>
 */
public class CrudClient<T> {

	private Class<T> type;
	private String endpoint;
	
	/**
	 * Without this, expect an auth error!
	 */
	private String jwt;

	public void setJwt(String jwt) {
		this.jwt = jwt;
	}
	
	public CrudClient(Class<T> type, String endpoint) {
		this.type = type;
		this.endpoint = endpoint;
		Utils.check4null(type, endpoint);
	}

	public JSend publish(T item) {
		FakeBrowser fb = new FakeBrowser();
		fb.setDebug(true);

		// You really should set auth!
		if (jwt != null) {
			fb.setAuthenticationByJWT(jwt);
		}
		
		Gson gson = gson();
		Map json = gson.toJsonObject(item);
		Map<String, String> vars = new ArrayMap(
			WebRequest.ACTION_PARAMETER, CrudServlet.ACTION_PUBLISH,
			AppUtils.ITEM.getName(), json
		);
		String url = endpoint;
		// ID?
		String id = getId(item);
		if (id != null) {
			url += "/"+WebUtils.urlEncode(id);
		}
		
		String response = fb.post(url, vars);
		JSend jsend = new JSend();
		jsend.setCode(fb.getStatus());
		try {
			Object data = gson.fromJson(response);
			jsend.setData(new JThing(data));
		} catch(Throwable ex) {
			// oh well
			jsend.setMessage(response);
		}
		return jsend;
	}

	protected String getId(T item) {
		if (item instanceof AThing) {
			return ((AThing) item).getId();
		}
		return null;
	}

	private Gson gson() {
		Gson gson = Dep.getWithDefault(Gson.class, new Gson());
		return gson;
	}
	
}
