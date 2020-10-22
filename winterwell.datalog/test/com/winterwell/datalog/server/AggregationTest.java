package com.winterwell.datalog.server;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import static org.junit.Assert.*;
import org.junit.Test; 
import org.eclipse.jetty.util.ajax.JSON;

import com.winterwell.gson.JsonParser;
import com.winterwell.gson.JsonElement;
import com.winterwell.gson.JsonObject;
import com.winterwell.gson.FlexiGson;
import com.winterwell.gson.JsonArray;
import com.winterwell.datalog.Dataspace;
import com.winterwell.es.UtilsForESTests;
import com.winterwell.es.client.BulkRequestBuilder;
import com.winterwell.es.client.BulkResponse;
import com.winterwell.es.client.ESConfig;
import com.winterwell.es.client.ESHttpClient;
import com.winterwell.es.client.IndexRequestBuilder;
import com.winterwell.es.client.KRefresh;
import com.winterwell.utils.Dep;
import com.winterwell.utils.Printer;
import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.utils.web.WebUtils;
import com.winterwell.utils.web.WebUtils2;
import com.winterwell.web.FakeBrowser;
import com.winterwell.web.ajax.JSend;
import com.winterwell.web.ajax.JThing;

public class AggregationTest {
	
	private static DataLogServer server;
	private static String ENDPOINT;
	private static final CharSequence DATASPACE = new Dataspace("gl"); //query from the local ES index
	public final static String INDEX = "datalog.compressed_oct20"; //bulk into newly created local ES index
	
	public void initDataTest() {
		if (ENDPOINT!=null) return;
		// spin up a server
		server = new DataLogServer();
		server.doMain(new String[0]);
		ENDPOINT = "http://localhost:"+server.getConfig().getPort();
	}
	
	@Test
	public void testAggregationByDomain() {
		initDataTest();
		FakeBrowser fb = fb();
		// create breakdown that does aggregation based on time interval of domains
		String json = fb.getPage(ENDPOINT+"/data", new ArrayMap(
				"name","test-1",
				"dataspace", DATASPACE,
				"breakdown", "domain/time"
				));
		JSend resp = JSend.parse(json);
		String data = resp.getData().string();
		Printer.out(data);
		assert ! data.contains("no0");
	}
	
	@Test
	// this will create a new index named datalog.compressed_oct20 
	public void testBulkNewIndexData() {
		initDataTest();
		FakeBrowser fb = fb();
		// create breakdown that does aggregation based on time interval of domains
		String json = fb.getPage(ENDPOINT+"/data", new ArrayMap(
				"name","test-1",
				"dataspace", DATASPACE,
				"breakdown", "domain/time"
				));
		JSend resp = JSend.parse(json);
		String data = resp.getData().string();
		Printer.out(data);
		
		// prepare to bulk aggregated data
		Dep.setIfAbsent(FlexiGson.class, new FlexiGson());
		Dep.setIfAbsent(ESConfig.class, new ESConfig());
		ESConfig esconfig = Dep.get(ESConfig.class);
		if ( ! Dep.has(ESHttpClient.class)) Dep.setSupplier(ESHttpClient.class, false, ESHttpClient::new);
		ESHttpClient esc = Dep.get(ESHttpClient.class);
		BulkRequestBuilder bulk = esc.prepareBulk();
		
		// parse aggregated data 
		JsonElement jelement = new JsonParser().parse(data);
	    JsonObject  jobject = jelement.getAsJsonObject();
	    jobject = jobject.getAsJsonObject("by_domain_time");
	    JsonArray jarray = jobject.getAsJsonArray("buckets");
	    JsonArray child_jarray;
	    // each j is a different domain
	    int i = 0;
	    for (JsonElement j : jarray) {
	    	jobject = j.getAsJsonObject();
	    	String domain_name = jobject.get("key").getAsString();
	    	jobject = jobject.getAsJsonObject("by_time");
	    	child_jarray = jobject.getAsJsonArray("buckets");
	    	for (JsonElement child : child_jarray) {
	    		String time = child.getAsJsonObject().get("key_as_string").getAsString();
	    		double doc_count = child.getAsJsonObject().get("doc_count").getAsDouble();
	    		IndexRequestBuilder pi = esc.prepareIndex(INDEX, "compressed_"+i);
	    		i = i + 1; //increment document id
	    		//create new document - with only three fields domain, time and count for now
				pi.setBodyMap(new ArrayMap("domain", domain_name, "time", time, "count", doc_count));
				bulk.add(pi);
	    	}
	    }
	    bulk.setRefresh(KRefresh.WAIT_FOR);
		bulk.setDebug(true);
		BulkResponse br = bulk.get();
		assert ! br.hasErrors() : br.getError();
		
		// sanity check if data is written to local ES
		Map<String, Object> got = esc.get(INDEX, "simple", "compressed_20");
		System.out.println(got);
	}
	/* 
	//troublesome to use
	private JSend parseJSON(String data, String keyname) {
		Map jobj = (Map) JSON.parse(data);
		System.out.println(jobj.keySet());
		Object _data = jobj.get(keyname); //need to check if keyname is correct
		JThing thing = new JThing();
		JSend jsend = new JSend();
		thing.setJsonObject(_data);
		JSend resp = jsend.setData(thing);
		return resp;
	} */
	
	private FakeBrowser fb() {
		FakeBrowser fb = new FakeBrowser();
		fb.setDebug(true);
		return fb;
	}

}
