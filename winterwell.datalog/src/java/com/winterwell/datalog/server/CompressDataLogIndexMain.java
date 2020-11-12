package com.winterwell.datalog.server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.winterwell.datalog.DataLogConfig;
import com.winterwell.datalog.Dataspace;
import com.winterwell.es.ESType;
import com.winterwell.es.client.BulkRequestBuilder;
import com.winterwell.es.client.BulkResponse;
import com.winterwell.es.client.ESConfig;
import com.winterwell.es.client.ESHttpClient;
import com.winterwell.es.client.IESResponse;
import com.winterwell.es.client.IndexRequestBuilder;
import com.winterwell.es.client.KRefresh;
import com.winterwell.es.client.TransformRequestBuilder;
import com.winterwell.es.client.admin.CreateIndexRequest;
import com.winterwell.es.client.admin.PutMappingRequestBuilder;
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
import com.winterwell.utils.time.TUnit;
import com.winterwell.utils.time.Time;
import com.winterwell.web.FakeBrowser;
import com.winterwell.web.ajax.JSend;
import com.winterwell.web.app.AMain;

/**
 * 
 * Should we use RollUps??
 * Or Transforms??
 * https://www.elastic.co/guide/en/elasticsearch/reference/7.9/data-rollup-transform.html
 * 
 * 
 * 
 * Use aggregations to compress the index for a month
 * @author Daniel
 *
 */
public class CompressDataLogIndexMain extends AMain<DataLogConfig> {
	
	public final static String ALIAS = "datalog.transformed.all";
	Map<Integer, String> months = new HashMap<Integer, String>();
	
	public static void main(String[] args) {
		CompressDataLogIndexMain amain = new CompressDataLogIndexMain();
		amain.doMain(args);
	}
	
	@Override
	protected void doMain2() {
		months.put(1, "jan");
		months.put(2, "feb");
		months.put(3, "mar");
		months.put(4, "apr");
		months.put(5, "may");
		months.put(6, "jun");
		months.put(7, "jul");
		months.put(8, "aug");
		months.put(9, "sep");
		months.put(10, "oct");
		months.put(11, "nov");
		months.put(12, "dec");
		
		int m = new Time().minus(TUnit.MONTH).minus(TUnit.MONTH).getMonth();
		String month = months.get(m);
		String year = Integer.toString(new Time().getYear());
		String index = "datalog.transformed_" + month + year.substring(2,4);
		String source = "datalog.gl_" + month + year.substring(2,4);
		
		// aggregate data
		ESHttpClient esc = CreateIndexWithPropertiesMapping(index, ALIAS);
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
		trb.setBody(source, index, terms, "24h");
		trb.setDebug(true);
		IESResponse response = trb.get();
		Printer.out(response);
		
		//after creating transform job, start it 
		TransformRequestBuilder trb2 = esc.prepareTransformStart("transform_job"); 
		trb2.setDebug(true);
		IESResponse response2 = trb2.get();
		Printer.out(response2);
		
		//stop the transform job after 5 seconds
		Utils.sleep(5000);
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
	
	private FakeBrowser fb() {
		FakeBrowser fb = new FakeBrowser();
		fb.setDebug(true);
		return fb;
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
