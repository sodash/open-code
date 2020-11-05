package com.winterwell.datalog.server;

import static org.junit.Assert.*;

import java.util.ArrayList;

import org.junit.Test;

import com.winterwell.es.client.BulkRequestBuilder;
import com.winterwell.es.client.BulkResponse;
import com.winterwell.es.client.ESConfig;
import com.winterwell.es.client.ESHttpClient;
import com.winterwell.es.client.ESHttpRequest;
import com.winterwell.es.client.IESResponse;
import com.winterwell.es.client.TransformRequestBuilder;
import com.winterwell.gson.FlexiGson;
import com.winterwell.utils.Dep;
import com.winterwell.utils.Printer;

public class RollupAndTransformTest {

	@Test
	public void testRollup() {
		fail("Not yet implemented");
	}
	
	@Test
	public void testTransform() {
		Dep.setIfAbsent(FlexiGson.class, new FlexiGson());
		Dep.setIfAbsent(ESConfig.class, new ESConfig());
		if ( ! Dep.has(ESHttpClient.class)) Dep.setSupplier(ESHttpClient.class, false, ESHttpClient::new);
		ESHttpClient esc = Dep.get(ESHttpClient.class);
		TransformRequestBuilder trb = esc.prepareTransform();
		
		// specify some terms that we want to keep
		ArrayList<String> terms = new ArrayList<String>();
		terms.add("domain");
		terms.add("os");
		
		// specify source and destination
		trb.setBody("datalog.gl_sep20", "datalog.transformed", terms, "24h");
		trb.setDebug(true);
		IESResponse response = trb.get();
		Printer.out(response);
		//assert ! response.hasErrors() : response.getError();
		
	}

}
