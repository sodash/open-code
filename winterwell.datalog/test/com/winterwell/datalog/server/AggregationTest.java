package com.winterwell.datalog.server;

import java.util.Map;

import org.junit.Test;

import com.winterwell.datalog.Dataspace;
import com.winterwell.es.ESType;
import com.winterwell.es.client.BulkRequest;
import com.winterwell.es.client.BulkResponse;
import com.winterwell.es.client.ESConfig;
import com.winterwell.es.client.ESHttpClient;
import com.winterwell.es.client.IESResponse;
import com.winterwell.es.client.IndexRequest;
import com.winterwell.es.client.KRefresh;
import com.winterwell.es.client.admin.CreateIndexRequest;
import com.winterwell.es.client.admin.PutMappingRequest;
import com.winterwell.es.fail.ESIndexAlreadyExistsException;
import com.winterwell.gson.FlexiGson;
import com.winterwell.gson.JsonArray;
import com.winterwell.gson.JsonElement;
import com.winterwell.gson.JsonObject;
import com.winterwell.gson.JsonParser;
import com.winterwell.utils.Dep;
import com.winterwell.utils.Printer;
import com.winterwell.utils.Utils;
import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.utils.containers.Pair2;
import com.winterwell.web.FakeBrowser;
import com.winterwell.web.ajax.JSend;

public class AggregationTest {
	
	private static DataLogServer server;
	private static String ENDPOINT;
	private static final CharSequence DATASPACE = new Dataspace("gl"); //query from the local ES index
	public final static String INDEX = "datalog.compressed_oct20"; //bulk into new local ES index
	
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
		// create breakdown that does aggregation based specified fields
		String json = fb.getPage(ENDPOINT+"/data", new ArrayMap(
				"name","test-1",
				"dataspace", DATASPACE,
				"breakdown", "evt/campaign/domain/pub/vert/browser/os/time"
				));
		JSend resp = JSend.parse(json);
		String data = resp.getData().string();
		Printer.out(data);
		
		// prepare to bulk aggregated data
		// check to see if index already exists, if not, create one
		ESHttpClient esc = CreateIndexWithPropertiesMapping(INDEX);
		BulkRequest bulk = esc.prepareBulk();
		
		// parse aggregated data 
		JsonElement jelement = new JsonParser().parse(data);
	    JsonObject  jobject = jelement.getAsJsonObject();
	    jobject = jobject.getAsJsonObject("by_evt_campaign_domain_pub_vert_browser_os_time");
	    JsonArray jarray1 = jobject.getAsJsonArray("buckets");
	    // each j1 is a different evt
	    int i = 0; // document id
	    for (JsonElement j1 : jarray1) {
	    	Pair2<String,JsonArray> evt = parseAggregations(j1, "by_campaign_domain_pub_vert_browser_os_time");
	    	String evt_name = evt.getFirst();
	    	JsonArray jarray2 = evt.getSecond();
	    	for (JsonElement j2 : jarray2) { // each j2 is a campaign
	    		Pair2<String,JsonArray> campaign = parseAggregations(j2, "by_domain_pub_vert_browser_os_time");
		    	String campaign_name = campaign.getFirst();
		    	JsonArray jarray3 = campaign.getSecond();
		    	for (JsonElement j3 : jarray3) { // each j3 is a domain
		    		Pair2<String,JsonArray> domain = parseAggregations(j3, "by_pub_vert_browser_os_time");
			    	String domain_name = domain.getFirst();
			    	JsonArray jarray4 = domain.getSecond();
			    	for (JsonElement j4 : jarray4) { // each j4 is a pub
			    		Pair2<String,JsonArray> pub = parseAggregations(j4, "by_vert_browser_os_time");
				    	String pub_name = pub.getFirst();
				    	JsonArray jarray5 = pub.getSecond();
				    	for (JsonElement j5 : jarray5) { // each j5 is a vert
				    		Pair2<String,JsonArray> vert = parseAggregations(j5, "by_browser_os_time");
					    	String vert_name = vert.getFirst();
					    	JsonArray jarray6 = vert.getSecond();
					    	for (JsonElement j6 : jarray6) { // each j6 is a browser
					    		Pair2<String,JsonArray> browser = parseAggregations(j6, "by_os_time");
						    	String browser_name = browser.getFirst();
						    	JsonArray jarray7 = browser.getSecond();
						    	for (JsonElement j7 : jarray7) { // each j7 is an os
						    		Pair2<String,JsonArray> os = parseAggregations(j7, "by_time");
							    	String os_name = os.getFirst();
							    	JsonArray jarray8 = os.getSecond();
							    	for (JsonElement j8 : jarray8) { // each j8 is a time interval of one day
							    		String time = j8.getAsJsonObject().get("key_as_string").getAsString();
							    		double doc_count = j8.getAsJsonObject().get("doc_count").getAsDouble();
							    		IndexRequest pi = esc.prepareIndex(INDEX, "compressed_"+i);
							    		i = i + 1; //increment document id
							    		pi.setBodyMap(new ArrayMap(
							    				"evt", evt_name,
							    				"campaign", campaign_name,
							    				"domain", domain_name,
							    				"pub", pub_name,
							    				"vert", vert_name,
							    				"browser", browser_name,
							    				"os", os_name,
							    				"time", time,
							    				"count", doc_count));
										bulk.add(pi);
							    	}
						    	}
					    	}
				    	}
			    	}
		    	}
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
	
	// take index name as input argument
	private ESHttpClient CreateIndexWithPropertiesMapping(String idx) {
		Dep.setIfAbsent(FlexiGson.class, new FlexiGson());
		Dep.setIfAbsent(ESConfig.class, new ESConfig());
		if ( ! Dep.has(ESHttpClient.class)) Dep.setSupplier(ESHttpClient.class, false, ESHttpClient::new);
		ESHttpClient esc = Dep.get(ESHttpClient.class);
		try {
			CreateIndexRequest cir = esc.admin().indices().prepareCreate(idx).setAlias("datalog.compressed.all");
			cir.get().check();
			Utils.sleep(100);
			// set properties mapping
			PutMappingRequest pm = esc.admin().indices().preparePutMapping(idx);
			ESType mytype = new ESType()
					.property("domain", ESType.keyword)
					.property("browser", ESType.keyword)
					.property("campaign", ESType.keyword)
					.property("evt", ESType.keyword)
					.property("os", ESType.keyword)
					.property("pub", ESType.keyword)
					.property("vert", ESType.keyword)
					.property("time", new ESType().date())
					.property("count", new ESType(double.class));
			pm.setMapping(mytype);
			pm.setDebug(true);
			IESResponse resp = pm.get().check();
			Printer.out(resp.getJson());
		} catch (ESIndexAlreadyExistsException ex) {
			Printer.out("Index already exists, proceeding...");
		}
		return esc;
	}
	
	private Pair2<String, JsonArray> parseAggregations(JsonElement j, String term) {
		JsonObject jobject = j.getAsJsonObject();
    	String name = jobject.get("key").getAsString();
    	jobject = jobject.getAsJsonObject(term);
    	JsonArray jarray = jobject.getAsJsonArray("buckets");
    	return new Pair2<String, JsonArray>(name, jarray);
	}
	
	private FakeBrowser fb() {
		FakeBrowser fb = new FakeBrowser();
		fb.setDebug(true);
		return fb;
	}

}
