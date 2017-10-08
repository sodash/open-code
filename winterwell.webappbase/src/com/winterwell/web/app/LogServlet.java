package com.winterwell.web.app;

import java.util.ArrayList;
import java.util.regex.Pattern;

import com.winterwell.utils.StrUtils;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.time.Period;
import com.winterwell.utils.time.Time;
import com.winterwell.utils.web.WebUtils2;
import com.winterwell.web.fields.AField;
import com.winterwell.web.fields.IntField;
import com.winterwell.web.fields.SafeString;


/**
 * Provide views into the logs -- and Ajax logging.
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
 * <p>
 * TODO filter by time
 * @author daniel
 *
 */
public class LogServlet implements IServlet {

	public LogServlet() {
	}

	public static final SafeString MSG = new SafeString("msg");
	
	@Override
	public void process(WebRequest state) throws Exception {
		// Provide Ajax logging NB: This opens us to a very subtle attack where you poke stuff into our logs
		String msg = state.get(MSG);
		if (msg!=null) {
			String tag = state.get(TAG, "log");
			Log.e(AJAX_TAG_PREFIX+tag, msg+" ua: "+state.getUserAgent());
			WebUtils2.sendText("OK", state.getResponse());
		}		
	}

	private static final IntField NUM_LINES = new IntField("LogServlet.numLines");
	private static final SafeString FILTER = new SafeString("LogServlet.filter");
	public static final AField<String> TAG = new SafeString("tag");

	/**
	 * log. prepended to all log requests made by ajax.
	 */
	public static final String AJAX_TAG_PREFIX = "ajax.";



	public static Pattern severityPattern() {
		return Pattern.compile("\\]\\s*([A-Z]+)");
	}
	
}
