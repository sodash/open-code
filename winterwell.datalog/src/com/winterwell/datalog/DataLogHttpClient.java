package com.winterwell.datalog;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.jetty.util.ajax.JSON;

import com.winterwell.datalog.server.DataServlet;
import com.winterwell.utils.FailureException;
import com.winterwell.utils.Printer;
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
	
	public DataLogHttpClient(String namespace) {
		this.namespace = namespace;
	}
	
	public List<DataLogEvent> getEvents(String q, int maxResults) {
		// Call DataServlet
		FakeBrowser fb = new FakeBrowser();
		// TODO auth!
//		fb.setAuthenticationByJWT(token);
		
		String json = fb.getPage(ENDPOINT, new ArrayMap("q", q, "size", maxResults));
		
		Map jobj = (Map) JSON.parse(json);
		
		List<Map> egs = Containers.asList((Object)SimpleJson.get(jobj, "cargo", "examples"));
		List<DataLogEvent> des = new ArrayList();
		// Convert into DataLogEvents
		for (Map eg : egs) {
			DataLogEvent de = DataLogEvent.fromESHit(eg);
			des.add(de);
		}
		
		Boolean success = (Boolean) jobj.get("success");
		if (success == false) {
			Log.e(new FailureException(Printer.toString(jobj.get("errors")))); // TODO throw
		}
		
		return des;
	}

}
