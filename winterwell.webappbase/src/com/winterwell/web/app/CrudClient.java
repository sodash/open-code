package com.winterwell.web.app;

import java.util.Map;

import com.winterwell.data.AThing;
import com.winterwell.gson.Gson;
import com.winterwell.utils.Dep;
import com.winterwell.utils.Utils;
import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.utils.web.WebUtils;
import com.winterwell.web.FakeBrowser;
import com.winterwell.web.WebEx;
import com.winterwell.web.ajax.JSend;
import com.winterwell.web.ajax.JThing;

/**
 * Status: WIP 
 * A java client for working with data managed by a {@link CrudServlet}
 * @author daniel
 *
 * @param <T> the data-item managed
 */
public class CrudClient<T> {

	private Class<T> type;
	private String endpoint;
	private boolean debug;

	public void setDebug(boolean b) {
		debug = b;
	}	
	
	/**
	 * @deprecated Normally this is set from config.
	 * @param endpoint
	 */
	public void setEndpoint(String endpoint) {
		this.endpoint = endpoint;
	}
	
	
	/**
	 * Without this, expect an auth error!
	 * 
	 * We want a JWT that says:
	 * "I am app A" (identity) 
	 * and 
	 * "I, Bob, give app A permission to manage T" (permission)
	 */
	private String jwt;
	
	protected boolean authNeeded;

	/**
	 * Set authentication! Without this, expect an auth error.
	 * @param jwt
	 */
	public void setJwt(String jwt) {
		this.jwt = jwt;
	}
	
	public CrudClient(Class<T> type, String endpoint) {
		this.type = type;
		this.endpoint = endpoint;
		Utils.check4null(type, endpoint);
	}

	public JSend list() {
		FakeBrowser fb = fb();
		
		String response = fb.getPage(endpoint+"/"+CrudServlet.LIST_SLUG);
		
		JSend jsend = jsend(fb, response);
		return jsend;
	}
	

	public JThing<T> get(String id) throws WebEx.E404 {
		FakeBrowser fb = fb();
		String response = fb.getPage(endpoint+"/"+WebUtils.urlEncode(id));
		
		JSend jsend = jsend(fb, response);
		JThing<T> jt = jsend.getData();
		jt.setType(type);
		return jt;
	}
	
	public JSend publish(T item) {
		FakeBrowser fb = fb();
		
		Gson gson = gson();
		String sjson = gson.toJson(item);
		Map<String, String> vars = new ArrayMap(
			WebRequest.ACTION_PARAMETER, CrudServlet.ACTION_PUBLISH,
			AppUtils.ITEM.getName(), sjson
		);
		String url = endpoint;
		// ID?
		String id = getId(item);
		if (id != null) {
			String encId = WebUtils.urlEncode(id);
			url += "/"+encId;
		}
		
		String response = fb.post(url, vars);

		JSend jsend = jsend(fb, response);
		return jsend;
	}

	private JSend jsend(FakeBrowser fb, String response) {
		Gson gson = gson();
		Map data = gson.fromJson(response);
		JSend jsend = JSend.parse2_create(data);
				
		jsend.setCode(fb.getStatus());
		return jsend;
	}

	private FakeBrowser fb() {
		FakeBrowser fb = new FakeBrowser();
		fb.setDebug(debug);

		// You really should set auth!
		if (jwt != null) {
			fb.setAuthenticationByJWT(jwt);
		} else {
			if (authNeeded) throw new WebEx.E401("No authentication set for "+this+" - call setJwt()");
		}
		return fb;
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
