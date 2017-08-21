package com.winterwell.datalog;

import java.util.List;

import com.winterwell.depot.IInit;
import com.winterwell.gson.Gson;
import com.winterwell.utils.Dep;
import com.winterwell.utils.containers.ListMap;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.threads.Actor;
import com.winterwell.web.FakeBrowser;
import com.winterwell.web.app.AppUtils;
import com.winterwell.web.app.KServerType;

/**
 * TODO should we use an Actor model here for high-throughput low-latency?
 * @author daniel
 *
 */
public class CallbackManager extends Actor<DataLogEvent> implements IInit {

	private static final String LOGTAG = "CallbackManager";
	
	ListMap<String,Callback> callbacksForDataspace = new ListMap();
	
	public CallbackManager() {
	
	}
	
	@Override
	public void init() {
		// a call to adserver
		KServerType mtype = AppUtils.getServerType(null);
		String protocol = mtype==KServerType.LOCAL? "http" : "https"; 
		String url = protocol+"://"+(mtype==KServerType.PRODUCTION? "" : mtype.toString().toLowerCase())
				+"as.good-loop.com/lgwebhook";
		// FIXME minview is where money gets spent!		
		Callback minview = new Callback("gl", "minview", url);
		callbacksForDataspace.add("gl", minview);
	}

	protected void receive(DataLogEvent msg, Actor sender) throws Exception {		
		List<Callback> cbs = callbacksForDataspace.get(msg.dataspace);
		if (cbs==null) return;
		for (Callback callback : cbs) {
			if (callback.evt!=null && ! callback.evt.equals(msg.eventType)) {
				continue;
			}
			try {
				Log.d(LOGTAG, callback.url+" for "+msg);
				FakeBrowser fb = new FakeBrowser();
				fb.setDebug(true);
				String json = Gson.toJSON(msg);
				String ok = fb.post(callback.url, json);
			} catch(Exception ex) {
				// retry once
				if (sender != this) {
					send(msg, this);
					return;
				}
				throw ex;
			}
		}
	}
	
}
