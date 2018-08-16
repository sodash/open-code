package com.winterwell.datalog;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.jetty.util.ajax.JSON;

import com.winterwell.utils.FailureException;
import com.winterwell.utils.Printer;
import com.winterwell.utils.Utils;
import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.utils.containers.Containers;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.web.SimpleJson;
import com.winterwell.web.FakeBrowser;

/**
 * Get data via the DataLog web service (i.e. call DataServlet)
 * @author daniel
 * @testedby {@link DataLogHttpClientTest}
 */
public class DataLogHttpClient {

	String namespace;
	
	String ENDPOINT = "https://lg.good-loop.com/data";
	
	@Override
	public String toString() {
		return "DataLogHttpClient [namespace=" + namespace + ", ENDPOINT=" + ENDPOINT + "]";
	}

	/**
	 * 
	 * @param endpoint Can be null (uses the default {@link #ENDPOINT})
	 * @param namespace
	 */
	public DataLogHttpClient(String endpoint, String namespace) {
		if (endpoint!=null) {			
			assert endpoint.contains("://") && endpoint.contains("/data") : endpoint;
			ENDPOINT = endpoint;
		}
		this.namespace = namespace;
		Utils.check4null(namespace);
		assert ! namespace.contains(".") : "server / namespace mixup? "+namespace;
	}
	
	public List<DataLogEvent> getEvents(String q, int maxResults) {
		// Call DataServlet
		FakeBrowser fb = new FakeBrowser();
		// TODO auth!
//		fb.setAuthenticationByJWT(token);
		
		String json = fb.getPage(ENDPOINT, new ArrayMap("dataspace", namespace, "q", q, "size", maxResults));
		
		Map jobj = (Map) JSON.parse(json);
		
		List<Map> egs = Containers.asList((Object)SimpleJson.get(jobj, "cargo", "examples"));
		List<DataLogEvent> des = new ArrayList();
		// Convert into DataLogEvents
		for (Map eg : egs) {
			DataLogEvent de = DataLogEvent.fromESHit(namespace, (Map)eg.get("_source"));
			des.add(de);
		}
		
		Boolean success = (Boolean) jobj.get("success");
		if (success == false) {
			Log.e(new FailureException(Printer.toString(jobj.get("errors")))); // TODO throw
		}
		
		return des;
	}

}
