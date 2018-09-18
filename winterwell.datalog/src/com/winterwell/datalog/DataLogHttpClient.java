package com.winterwell.datalog;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.jetty.util.ajax.JSON;

import com.winterwell.datalog.server.DataLogFields;
import com.winterwell.nlp.query.SearchQuery;
import com.winterwell.utils.FailureException;
import com.winterwell.utils.MathUtils;
import com.winterwell.utils.Mutable.Ref;
import com.winterwell.utils.Printer;
import com.winterwell.utils.TodoException;
import com.winterwell.utils.Utils;
import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.utils.containers.Containers;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.time.Period;
import com.winterwell.utils.time.Time;
import com.winterwell.utils.web.SimpleJson;
import com.winterwell.utils.web.WebUtils2;
import com.winterwell.web.FakeBrowser;
import com.winterwell.web.ajax.JSend;
import com.winterwell.youagain.client.AuthToken;

/**
 * Get data via the DataLog web service (i.e. call DataServlet)
 * @author daniel
 * @testedby {@link DataLogHttpClientTest}
 */
public class DataLogHttpClient {

	Dataspace dataspace;
	
	String ENDPOINT = "https://lg.good-loop.com/data";
	
	private List<AuthToken> auth;

	private Map<String,Double> overview;

	private List<Map> examples;

	private Time start;

	private Time end;
	
	@Override
	public String toString() {
		return "DataLogHttpClient [namespace=" + dataspace + ", ENDPOINT=" + ENDPOINT + "]";
	}

	public DataLogHttpClient(Dataspace namespace) {
		this(null, namespace);
	}
	
	public Object save(DataLogEvent event) {
		String server = WebUtils2.getHost(ENDPOINT);
		// HACK: preserve (local testing) http protocol
		if (ENDPOINT.startsWith("http://")) server = "http://"+server;
		return DataLogRemoteStorage.saveToRemoteServer(server, event);
	}
	/**
	 * 
	 * @param endpoint Can be null (uses the default {@link #ENDPOINT})
	 * @param namespace
	 */
	public DataLogHttpClient(String endpoint, Dataspace namespace) {
		if (endpoint!=null) {			
			assert endpoint.contains("://") && endpoint.contains("/data") : endpoint;
			ENDPOINT = endpoint;
		}
		this.dataspace = namespace;
		Utils.check4null(namespace);
		assert ! namespace.toString().contains(".") : "server / namespace mixup? "+namespace;
	}
	
	public List<DataLogEvent> getEvents(SearchQuery q, int maxResults) {
		// Call DataServlet
		FakeBrowser fb = new FakeBrowser();
		fb.setDebug(true);
		// auth!
		if (auth!=null) {
			AuthToken.setAuth(fb, auth);
		}
		
		Map<String, String> vars = new ArrayMap(
				"dataspace", dataspace, 
				"q", q==null? null : q.getRaw(), 
				"size", maxResults,
				DataLogFields.START.name, start==null? null : start.toISOString(),
				DataLogFields.END.name, end==null? null : end.toISOString()
				);
		// call
		String json = fb.getPage(ENDPOINT, vars);
		
		Map jobj = (Map) JSON.parse(json);
		
		List<Map> egs = Containers.asList((Object)SimpleJson.get(jobj, "cargo", "examples"));
		List<DataLogEvent> des = new ArrayList();
		// Convert into DataLogEvents
		for (Map eg : egs) {
			DataLogEvent de = DataLogEvent.fromESHit(dataspace, (Map)eg.get("_source"));
			des.add(de);
		}
		
		Boolean success = (Boolean) jobj.get("success");
		if (success == false) {
			Log.e(new FailureException(Printer.toString(jobj.get("errors")))); // TODO throw
		}
		
		return des;
	}

	public DataLogHttpClient setAuth(List<AuthToken> auth) {
		this.auth = auth;
		return this;
	}
		

	public Map<String, Double> getBreakdown(SearchQuery q, Breakdown breakdown) {
		// Call DataServlet
		FakeBrowser fb = new FakeBrowser();
		fb.setDebug(true);
		
		// auth!
		if (auth!=null) {
			AuthToken.setAuth(fb, auth);
		}
		
		String b = breakdown.toString();		
		String json = fb.getPage(ENDPOINT, new ArrayMap(
				"dataspace", dataspace,				
				"q", q.getRaw(), 
				"breakdown", b,
				DataLogFields.START.name, start==null? null : start.toISOString(),
				DataLogFields.END.name, end==null? null : end.toISOString(),
				"size", 5)); // size is num examples
		
		JSend jobj = JSend.parse(json);
		if ( ! jobj.isSuccess()) {
			throw new FailureException(jobj.getMessage());
		}		
		
		List<Map> buckets = Containers.asList((Object)SimpleJson.get(jobj.getDataMap(), "by_"+breakdown.by, "buckets"));
		Map<String, Double> byX = new ArrayMap();
		// convert it		
		for (Map bucket : buckets) {
			String k = (String) bucket.get("key");
			Object ov = SimpleJson.get(bucket, breakdown.field, breakdown.op);
			double v = MathUtils.toNum(ov);
			byX.put(k, v);
		}
		
		// stash extra info in fields
		overview = SimpleJson.get(jobj.getDataMap(), breakdown.field);
		examples = Containers.asList((Object)SimpleJson.get(jobj.getDataMap(), "examples"));
		
		return byX;
	}

	/**
	 * stached from the previoud {@link #getBreakdown(SearchQuery, Breakdown)} call
	 * @return
	 */
	public List<DataLogEvent> getExamples() {
		if (examples==null) return null;
		List<DataLogEvent> des = new ArrayList();
		// Convert into DataLogEvents
		for (Map eg : examples) {
			DataLogEvent de = DataLogEvent.fromESHit(dataspace, (Map)eg.get("_source"));
			des.add(de);
		}
		return des;
	}

	public void setPeriod(Time start, Time end) {
		this.start = start; 
		this.end = end;
	}

}
