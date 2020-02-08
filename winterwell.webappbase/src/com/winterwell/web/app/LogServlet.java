package com.winterwell.web.app;

import java.util.regex.Pattern;

import com.winterwell.utils.Dep;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.web.WebUtils2;
import com.winterwell.web.fields.AField;
import com.winterwell.web.fields.IntField;
import com.winterwell.web.fields.SafeString;
import com.winterwell.youagain.client.YouAgainClient;


/**
 * Provide Ajax logging.
 * 
 * Note: This does not allow viewing logs over API -- as that would be a security risk (logs can easily contain sensitive data).
 * 
 * <h3>AJAX Logging</h3>
 * 
 * Endpoint: /log <br>
 * Parameters: <br>
 *  - tag Optional. Will have ajax prepended, so we can distinguish ajax-logged events (which could be bogus!) 
 * from internal ones. E.g. "foo" gets written as "#ajax.foo" <br>
 *  - msg
 * 
 * @see AServlet
 * @author daniel
 *
 */
public class LogServlet implements IServlet {

	public LogServlet() {
	}

	public static final SafeString MSG = new SafeString("msg");
	
	@Override
	public void process(WebRequest state) throws Exception {
		// try to login, so the log messages have a user-id
		try {
			YouAgainClient yac = Dep.get(YouAgainClient.class);
			yac.getAuthTokens(state);
		} catch(Throwable ex) {
			Log.e(AJAX_TAG_PREFIX+"login", ex);
		}
		// Provide Ajax logging 
		// NB: This opens us to a very subtle attack where you poke stuff into our logs
		String msg = state.get(MSG);
		if (msg!=null) {
			String tag = state.get(TAG, "log");
			Log.e(AJAX_TAG_PREFIX+tag, msg+" ua: "+state.getUserAgent());
			WebUtils2.sendText("OK", state.getResponse());
		}		
	}

	public static final AField<String> TAG = new SafeString("tag");

	/**
	 * log. prepended to all log requests made by ajax.
	 */
	public static final String AJAX_TAG_PREFIX = "ajax.";
	
}
