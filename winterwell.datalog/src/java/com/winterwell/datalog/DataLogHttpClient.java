package com.winterwell.datalog;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.jetty.util.ajax.JSON;

import com.winterwell.datalog.server.DataLogFields;
import com.winterwell.nlp.query.SearchQuery;
import com.winterwell.utils.Dep;
import com.winterwell.utils.FailureException;
import com.winterwell.utils.MathUtils;
import com.winterwell.utils.Printer;
import com.winterwell.utils.Utils;
import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.utils.containers.Containers;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.time.Time;
import com.winterwell.utils.time.TimeUtils;
import com.winterwell.utils.web.SimpleJson;
import com.winterwell.utils.web.WebUtils2;
import com.winterwell.web.FakeBrowser;
import com.winterwell.web.ajax.JSend;
import com.winterwell.youagain.client.AuthToken;

/**
 * Get data via the DataLog web service (i.e. call DataServlet)
 * @author daniel
 * @testedby  DataLogHttpClientTest}
 */
public class DataLogHttpClient {

	public final Dataspace dataspace;
	
	String ENDPOINT = Dep.get(DataLogConfig.class).getDataEndpoint;
	
	private List<AuthToken> auth;

	/**
	 * TODO this is a limited hack which only supports one top-level summary  
	 */
	private transient Map<String,Double> overview;
	/** TODO this is normally present but how does it overlap with overview? */
	private transient Map<String,Double> all;

	private List<Map> examples;

	private Time start;

	private Time end;
	
	private boolean debug = true;

	/**
	 * @deprecated for debug - the last url fetched
	 */
	private transient String lastCall;
	
	@Override
	public String toString() {
		return "DataLogHttpClient [namespace=" + dataspace + ", ENDPOINT=" + ENDPOINT + "]";
	}

	public DataLogHttpClient(Dataspace namespace) {
		this(null, namespace);
	}
	
	/**
	 * Save to remote lg server
	 * @param event
	 * @return
	 */
	public Object save(DataLogEvent event) {
		String server = WebUtils2.getHost(ENDPOINT);
		// HACK: preserve (local testing) http protocol
		if (ENDPOINT.startsWith("http://")) server = "http://"+server;
		return DataLogRemoteStorage.saveToRemoteServer(server, event);
	}
	/**null
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
		fb.setDebug(debug);
		// auth!
		if (auth!=null) {
			AuthToken.setAuth(fb, auth);
		}
		
		Map<String, String> vars = new ArrayMap(
				"dataspace", dataspace, 
				"q", q==null? null : q.getRaw(), 
				"size", maxResults,
				DataLogFields.START.name, startParam(),
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

	private String startParam() {
		// /data sedaults to start=1 month ago
		return start==null? TimeUtils.WELL_OLD.toISOString() : start.toISOString();
	}	

	public DataLogHttpClient setAuth(List<AuthToken> auth) {
		this.auth = auth;
		return this;
	}
	
	/** As a bit of security (cos examples carry more data than aggregate stats), we default to 0 */
	int numExamples = 0;
	
	public void setNumExamples(int numExamples) {
		this.numExamples = numExamples;
	}

	/**
	 * 
	 * TODO refactor for greater flex
	 * 
	 * Side effects: set overview (if breakdown requested it) and examples
	 * @param q
	 * @param breakdown
	 * @return {key e.g. "oxfam": value e.g. 100}
	 */
	public Map<String, Double> getBreakdown(SearchQuery q, Breakdown breakdown) {
		// Call DataServlet		
		String b = breakdown.toString();		
		ArrayMap vars = new ArrayMap(
				"dataspace", dataspace,				
				"q", q.getRaw(), 
				"breakdown", b,
				DataLogFields.START.name, startParam(),
				DataLogFields.END.name, end==null? null : end.toISOString(),
				"size", numExamples);
		
		// Call!
		JSend jobj = get2_httpCall(vars);		
		
		// e.g. by_cid buckets		
		List<Map> buckets = new ArrayList();
		Map jobjMap = jobj.getDataMap();
		for(String byi : breakdown.by) {
			List byi_buckets = Containers.asList((Object)SimpleJson.get(jobjMap, "by_"+byi, "buckets"));
			if (byi_buckets != null) buckets.addAll(byi_buckets);
		}		
		Map<String, Double> byX = new ArrayMap();
		// convert it		
		for (Map bucket : buckets) {
			String k = (String) bucket.get("key");
			Object ov = SimpleJson.get(bucket, breakdown.field, breakdown.op);
			double v = MathUtils.toNum(ov);
			byX.put(k, v);
		}		
		// stash extra info in fields
		overview = SimpleJson.get(jobjMap, breakdown.field); // present if breakdown included by=""
		all = SimpleJson.get(jobjMap, "all");
		examples = Containers.asList((Object)SimpleJson.get(jobjMap, "examples"));
		
		return byX;
	}

	/**
	 * Call DataLog!
	 * NB: Can be over-ridden to implement a cache
	 * @param vars
	 * @return
	 */
	protected JSend get2_httpCall(ArrayMap vars) {		
		FakeBrowser fb = new FakeBrowser();
		fb.setDebug(debug);		
		// auth!
		if (auth!=null) {
			AuthToken.setAuth(fb, auth);
		}		
		String json = fb.getPage(ENDPOINT, vars);		
		lastCall = fb.getLocation();
		
		JSend jobj = JSend.parse(json);
		if ( ! jobj.isSuccess()) {
			throw new FailureException(jobj.getMessage());
		}
		return jobj;
	}

	/**
	 * stached from the previous {@link #getBreakdown(SearchQuery, Breakdown)} call
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

	/**
	 * 
	 * @param start If null, defaults to well-old (i.e. all)
	 * @param end If null, defaults to now
	 */
	public void setPeriod(Time start, Time end) {
		this.start = start; 
		this.end = end;
	}

	/**
	 * Stashed from the previous {@link #getBreakdown(SearchQuery, Breakdown)}
	 * @return
	 */
	public Map<String, Double> getOverview() {
		return overview;
	}
	
	/**
	 * As {@link #getOverview()}
	 * @return
	 */
	public Map<String, Double> getAll() {
		return all;
	}

	public String getLastCall() {
		return lastCall;
	}
	
	// Set whether the client's FakeBrowsers should run in debug mode
	public void setDebug(boolean debug) {
		this.debug = debug;
	}

}
