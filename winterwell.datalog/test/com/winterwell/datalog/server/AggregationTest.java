package com.winterwell.datalog.server;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import static org.junit.Assert.*;
import org.junit.Test; 
import org.eclipse.jetty.util.ajax.JSON;

import com.winterwell.gson.JsonParser;
import com.winterwell.gson.JsonElement;
import com.winterwell.gson.JsonObject;
import com.winterwell.gson.JsonArray;
import com.winterwell.datalog.Dataspace;
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
	public void testCreateNewIndexData() {
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
		
		JsonElement jelement = new JsonParser().parse(data);
	    JsonObject  jobject = jelement.getAsJsonObject();
	    jobject = jobject.getAsJsonObject("by_domain_time");
	    JsonArray jarray = jobject.getAsJsonArray("buckets");
	    JsonArray child_jarray;
	    for (JsonElement j : jarray) {
	    	jobject = j.getAsJsonObject();
	    	String domain_name = jobject.get("key").getAsString();
	    	jobject = jobject.getAsJsonObject("by_time");
	    	child_jarray = jobject.getAsJsonArray("buckets");
	    	for (JsonElement child : child_jarray) {
	    		JsonObject compressed_json = new JsonObject();
	    		compressed_json.addProperty("domain", domain_name);
	    		compressed_json.addProperty("time", child.getAsJsonObject().get("key_as_string").getAsString());
	    		compressed_json.addProperty("count", child.getAsJsonObject().get("doc_count").getAsDouble());
	    		System.out.println("The domain is: " + domain_name);
	    		System.out.println("The time is: " + child.getAsJsonObject().get("key_as_string").getAsString());
	    		System.out.println("The count is: " + child.getAsJsonObject().get("doc_count").getAsDouble());
	    		System.out.println(compressed_json.toString());
	    		break;
	    	}
	    	break;
	    }
		
	    /*
		resp = parseJSON(data, "by_domain_time");
		data = resp.getData().string();
		Printer.out(data);
		resp = parseJSON(data, "buckets");
		data = resp.getData().string();
		// data is a list with multiple buckets, each bucket corresponds to a domain
		Printer.out(data);
		*/
	
	}
	
	private JSend parseJSON(String data, String keyname) {
		Map jobj = (Map) JSON.parse(data);
		System.out.println(jobj.keySet());
		Object _data = jobj.get(keyname); //need to check if keyname is correct
		JThing thing = new JThing();
		JSend jsend = new JSend();
		thing.setJsonObject(_data);
		JSend resp = jsend.setData(thing);
		return resp;
	}
	
	private FakeBrowser fb() {
		FakeBrowser fb = new FakeBrowser();
		fb.setDebug(true);
		return fb;
	}

}
