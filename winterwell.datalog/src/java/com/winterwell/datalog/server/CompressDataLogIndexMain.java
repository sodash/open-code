package com.winterwell.datalog.server;

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
	
	
	public static void main(String[] args) {
		CompressDataLogIndexMain amain = new CompressDataLogIndexMain();
		amain.doMain(args);
	}
	
	@Override
	protected void doMain2() {
		// aggregate data
		
		// save data
		
	}
	
	private FakeBrowser fb() {
		FakeBrowser fb = new FakeBrowser();
		fb.setDebug(true);
		return fb;
	}

}
