package com.winterwell.datalog.client;

import java.util.Map;

import com.winterwell.datalog.DataLog;
import com.winterwell.datalog.DataLogEvent;
import com.winterwell.datalog.IDataLog;
import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.utils.log.Log;
import com.winterwell.web.FakeBrowser;

/**
 * TODO A remote server client for DataLog
 * So the adserver can log stuff into lg.
 * @author daniel
 *
 */
public class DataLogClient 
//implements IDataLog
{

	private String endpoint;

	public void count(DataLogEvent event) {
		try {
			FakeBrowser fb = new FakeBrowser();
			// FIXME!!!
			Map<String, String> vars = new ArrayMap();
			fb.getPage(endpoint, vars);
		} catch(Exception ex) {
			Log.w("datalog", ex);
		}
	}
}
