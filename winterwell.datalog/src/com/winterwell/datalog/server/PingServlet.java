package com.winterwell.datalog.server;

import com.winterwell.datalog.DataLog;
import com.winterwell.datalog.IDataLog;
import com.winterwell.datalog.Rate;
import com.winterwell.es.client.ESHttpClient;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.time.TUnit;
import com.winterwell.utils.time.Time;
import com.winterwell.utils.web.WebUtils;
import com.winterwell.utils.web.WebUtils2;
import com.winterwell.web.app.WebRequest;

/**
 * Just confirm the server is alive and well.
 * @author daniel
 *
 */
public class PingServlet {

	private WebRequest state;

	public PingServlet(WebRequest request) {
		this.state = request;
	}

	public void doGet() {
		Double memUsed = DataLog.getTotal(new Time().minus(TUnit.HOUR), new Time(), IDataLog.STAT_MEM_USED).get();
		if (memUsed==0) {
			Log.e("ping", "Connection to database may be down! mem-used: 0");
//			return;
		}
		String output = "Hello from Java :)";
		WebUtils2.sendText(output, state.getResponse());
	}

}
