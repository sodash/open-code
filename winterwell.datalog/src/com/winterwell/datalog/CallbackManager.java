package com.winterwell.datalog;

import java.util.List;

import com.winterwell.depot.IInit;
import com.winterwell.gson.Gson;
import com.winterwell.utils.containers.Containers;
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
		if (DataLog.getImplementation().getConfig().noCallbacks) {
			return;
		}
		// HACK remove this hard-coded callback, and make it a dynamic setup
		// a call to adserver
		KServerType mtype = AppUtils.getServerType(null);
		StringBuilder url = AppUtils.getServerUrl(mtype, "as.good-loop.com");
		url.append("/lgwebhook");
		// minview is where money gets spent. donation is when a user picks a charity.		
		for(String evt : new String[] {"minview","click","donation"}) {
			Callback cb = new Callback("gl", evt, url.toString());
			callbacksForDataspace.add("gl", cb);
		}
	}

	@Override
	protected void consume(DataLogEvent msg, Actor sender) throws Exception {
		assert msg != null;
		if (DataLog.getImplementation().getConfig().noCallbacks) {
			Log.d(LOGTAG, "config: no callbacks");
			return;
		}
		List<Callback> cbs = callbacksForDataspace.get(msg.dataspace);
		Log.d(LOGTAG, "callbacks: "+cbs+" for "+msg);
		if (cbs==null) return;
		for (Callback callback : cbs) {
			// does the event match the callback?
			if ( ! matches(msg, callback)) {
				continue;
			}
			try {
				consume2_doCallback(msg, callback);
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

	void consume2_doCallback(DataLogEvent msg, Callback callback) {
		String json = Gson.toJSON(msg);
		Log.d(LOGTAG, callback.url+" for "+msg+" Posting "+json);
		FakeBrowser fb = new FakeBrowser();
		fb.setUserAgent(FakeBrowser.HONEST_USER_AGENT);
		fb.setDebug(true);		
		String ok = fb.post(callback.url, json);
	}

	private boolean matches(DataLogEvent msg, Callback callback) {
		if (callback.evt==null) return true; // match all
		// NPE paranoia
		if (msg.getEventType()==null) return false;
		if (Containers.contains(callback.evt, msg.getEventType())) {
			return true;
		}
		return false;
	}
	
}
