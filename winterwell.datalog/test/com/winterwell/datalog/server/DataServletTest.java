package com.winterwell.datalog.server;

import static org.junit.Assert.*;

import java.util.Map;

import org.junit.AfterClass;
import org.junit.Test;

import com.winterwell.datalog.DataLogEvent;
import com.winterwell.datalog.DataLogRemoteStorage;
import com.winterwell.datalog.Dataspace;
import com.winterwell.utils.Printer;
import com.winterwell.utils.Utils;
import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.utils.io.SysOutCollectorStream;
import com.winterwell.web.FakeBrowser;
import com.winterwell.web.ajax.JSend;

public class DataServletTest {

	private static final CharSequence DATASPACE = new Dataspace("testspace");
	private static DataLogServer server;
	private String ENDPOINT;

	public void initDataTest() {
		if (ENDPOINT!=null) return;
		// spin up a server
		server = new DataLogServer();
		server.doMain(new String[0]);
		ENDPOINT = "http://localhost:"+server.getConfig().getPort();

		// poke some data in
		// ...pixel tracking data
		new FakeBrowser().getPage(ENDPOINT+"/pxl?host=example.com&domain=test.example.com");		
		// ...event data
		DataLogEvent event = new DataLogEvent(DATASPACE, null, 1, new String[]{"unittest"}, new ArrayMap(
			"user", "DataServletTest@bot",
			"stage", "init",
			"host", "localhost"
		));
		DataLogRemoteStorage.saveToRemoteServer(ENDPOINT, event);
		// pause for ES to save
		Utils.sleep(1000);
	}
	
	
	@Test
	public void testBreakdowns() {
		initDataTest();
		FakeBrowser fb = fb();		
		String json = fb.getPage(ENDPOINT+"/data", new ArrayMap(
				"name","test-1",
				"dataspace", DATASPACE,
				"breakdown", "evt/time,evt,user"
				));
		JSend resp = JSend.parse(json);
		Map data = resp.getDataMap();
		Printer.out(data);
	}
	
	@AfterClass
	public static void close() {
		server.stop();
	}

	private FakeBrowser fb() {
		FakeBrowser fb = new FakeBrowser();
		fb.setDebug(true);
		return fb;
	}

}
