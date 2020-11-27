package com.winterwell.datalog.server;

import static org.junit.Assert.fail;

import java.util.ArrayList;

import org.junit.Test;

import com.winterwell.es.ESType;
import com.winterwell.es.client.ESConfig;
import com.winterwell.es.client.ESHttpClient;
import com.winterwell.es.client.IESResponse;
import com.winterwell.es.client.TransformRequestBuilder;
import com.winterwell.es.client.admin.CreateIndexRequest;
import com.winterwell.es.client.admin.PutMappingRequestBuilder;
import com.winterwell.es.fail.ESIndexAlreadyExistsException;
import com.winterwell.gson.FlexiGson;
import com.winterwell.utils.Dep;
import com.winterwell.utils.Printer;
import com.winterwell.utils.Utils;

public class RollupAndTransformTest {
	
	public final static String SOURCE = "datalog.gl_sep20"; 
	//public static String SOURCE = "scrubbed.datalog.gl_dec19_mapfix";
	public final static String INDEX = "datalog.transformed_sep20"; //bulk into new local ES index
	public final static String ALIAS = "datalog.transformed.all"; //alias to the new ES index

	@Test
	public void testRollup() {
		fail("Not yet implemented");
	}
	
	@Test
	public void testTransform() {
		ESHttpClient esc = CreateIndexWithPropertiesMapping(INDEX, ALIAS);
		TransformRequestBuilder trb = esc.prepareTransform("transform_job");
		
		// specify some terms that we want to keep
		ArrayList<String> terms = new ArrayList<String>();
		terms.add("domain");
		terms.add("pub");
		terms.add("vert");
		terms.add("browser");
		terms.add("campaign");
		terms.add("evt");
		terms.add("os");
		
		// create transform job
		// specify source and destination and time interval
		trb.setBody(SOURCE, INDEX, terms, "24h");
		trb.setDebug(true);
		IESResponse response = trb.get();
		Printer.out(response);
		
		//after creating transform job, start it 
		TransformRequestBuilder trb2 = esc.prepareTransformStart("transform_job"); 
		trb2.setDebug(true);
		IESResponse response2 = trb2.get();
		Printer.out(response2);
		
		//stop the transform job after 10 seconds
		Utils.sleep(10000);
		TransformRequestBuilder trb3 = esc.prepareTransformStop("transform_job"); 
		trb3.setDebug(true);
		IESResponse response3 = trb3.get();
		Printer.out(response3);
		
		//delete the transform job
		TransformRequestBuilder trb4 = esc.prepareTransformDelete("transform_job"); 
		trb4.setDebug(true);
		IESResponse response4 = trb4.get();
		Printer.out(response4);
	}
	
	private ESHttpClient CreateIndexWithPropertiesMapping(String idx, String alias) {
		Dep.setIfAbsent(FlexiGson.class, new FlexiGson());
		Dep.setIfAbsent(ESConfig.class, new ESConfig());
		if ( ! Dep.has(ESHttpClient.class)) Dep.setSupplier(ESHttpClient.class, false, ESHttpClient::new);
		ESHttpClient esc = Dep.get(ESHttpClient.class);
		try {
			CreateIndexRequest cir = esc.admin().indices().prepareCreate(idx).setAlias(alias);
			cir.get().check();
			Utils.sleep(100);
			// set properties mapping
			PutMappingRequestBuilder pm = esc.admin().indices().preparePutMapping(idx);
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

}
