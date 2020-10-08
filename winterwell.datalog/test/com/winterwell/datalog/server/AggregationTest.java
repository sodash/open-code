package com.winterwell.datalog.server;

import java.util.List;
import java.util.Map;
import static org.junit.Assert.*;
import org.junit.Test;

import com.winterwell.datalog.DataLog;
import com.winterwell.datalog.DataLogEvent;
import com.winterwell.datalog.DataLogImpl;
import com.winterwell.datalog.DataLogRemoteStorage;
import com.winterwell.datalog.DataLogSecurity;
import com.winterwell.datalog.Dataspace;
import com.winterwell.datalog.ESDataLogSearchBuilder;
import com.winterwell.datalog.ESStorage;
import com.winterwell.es.client.ESHttpClient;
import com.winterwell.es.client.SearchRequestBuilder;
import com.winterwell.es.client.SearchResponse;
import com.winterwell.nlp.query.SearchQuery;
import com.winterwell.nlp.query.SearchQuery.SearchFormatException;
import com.winterwell.utils.Dep;
import com.winterwell.utils.Printer;
import com.winterwell.utils.Utils;
import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.utils.io.CSVSpec;
import com.winterwell.utils.io.CSVWriter;
import com.winterwell.utils.io.FileUtils;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.threads.ICallable;
import com.winterwell.utils.time.Dt;
import com.winterwell.utils.time.TUnit;
import com.winterwell.utils.time.Time;
import com.winterwell.utils.web.WebUtils;
import com.winterwell.utils.web.WebUtils2;
import com.winterwell.web.FakeBrowser;
import com.winterwell.web.WebEx;
import com.winterwell.web.ajax.JSend;
import com.winterwell.web.ajax.JsonResponse;
import com.winterwell.web.app.IServlet;
import com.winterwell.web.app.Json2Csv;
import com.winterwell.web.app.WebRequest;
import com.winterwell.web.fields.DtField;
import com.winterwell.web.fields.IntField;
import com.winterwell.web.fields.ListField;
import com.winterwell.web.fields.SField;
import com.winterwell.youagain.client.AuthToken;
import com.winterwell.youagain.client.YouAgainClient;

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
	
	private FakeBrowser fb() {
		FakeBrowser fb = new FakeBrowser();
		fb.setDebug(true);
		return fb;
	}

}
