package com.winterwell.datalog.server;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.winterwell.utils.Printer;
import com.winterwell.utils.Proc;
import com.winterwell.web.ajax.JsonResponse;

import com.winterwell.utils.StrUtils;
import com.winterwell.utils.Utils;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.log.LogFile;
import com.winterwell.utils.log.Report;
import com.winterwell.utils.time.Dt;
import com.winterwell.utils.time.Period;
import com.winterwell.utils.time.TUnit;
import com.winterwell.utils.time.Time;
import com.winterwell.utils.web.WebUtils;
import com.winterwell.utils.web.WebUtils2;
import com.winterwell.web.app.FileServlet;
import com.winterwell.web.app.WebRequest;
import com.winterwell.web.app.WebRequest.KResponseType;
import com.winterwell.web.fields.AField;
import com.winterwell.web.fields.Form;
import com.winterwell.web.fields.IntField;
import com.winterwell.web.fields.JsonField;
import com.winterwell.web.fields.SField;
import com.winterwell.web.fields.SafeString;
import com.winterwell.web.fields.SelectField;
import com.winterwell.datalog.DataLog;
import com.winterwell.datalog.DataLogEvent;
import com.winterwell.depot.Depot;
import com.winterwell.depot.Desc;


/**
 * Fast Ajax logging.
 * 
 * Endpoint: /lg <br>
 * Parameters: <br>
 *  - tag Optional. Will have log prepended, so we can distinguish ajax-logged events (which could be bogus!) 
 * from internal ones. E.g. "foo" gets written as "#log.foo" <br>
 *  - msg
 * 
 * @see AServlet
 * <p>
 * TODO filter by time
 * @author daniel
 *
 */
public class LgServlet {

	static final SField TAG = new SField("t");
	public static final SField DATASPACE = new SField("d");

	public LgServlet() {		
	}
		
	
	static JsonField PARAMS = new JsonField("p");
	
	/**
	 * Log msg to fast.log file.  
	 * @param req
	 * @param resp
	 * @throws IOException 
	 */
	public static void fastLog(WebRequest state) throws IOException {
		HttpServletRequest req = state.getRequest();
		HttpServletResponse resp = state.getResponse();
		String ds = state.getRequired(DATASPACE);
		// TODO security check the dataspace?
		String tag = state.getRequired(TAG);
		String via = req.getParameter("via");
		Map params = (Map) state.get(PARAMS);
		
		doLog(state, ds, tag, via, params);
		
		// Reply
		// .gif?
		if (state.getResponseType()==KResponseType.image) {
			FileServlet.serveFile(TrackingPixelServlet.PIXEL, state);
			return;
		}
		WebUtils2.CORS(state, false);
		WebUtils2.sendText("OK", resp);
	}

	static void doLog(WebRequest state, String dataspace, String tag, String via, Map params) {
		assert dataspace != null;
		String trckId = TrackingPixelServlet.getCreateCookieTrackerId(state);
		// special vars
		if (params!=null) {
			// Replace $user with tracking-id, and $
			if ("$user".equals(params.get("user"))) {
				params.put("user", trckId);
			}
			// ip: $ip
			if ("$ip".equals(params.get("ip"))) {
				params.put("ip", state.getRemoteAddr());
			}
			if ("$useragent".equals(params.get("useragent"))) {
				params.put("useragent", state.getUserAgent());
			}
			// url: $url
			if ("$url".equals(params.get("url"))) {
				// remove some gumpf (UTM codes)
				String cref = WebUtils2.cleanUp(state.getReferer());
				params.put("url", cref);
			}
		}
		// write to log file
		doLogToFile(dataspace, tag, params, trckId, via, state);
				
		// write to Stat / ES
		DataLogEvent event = new DataLogEvent(dataspace, 1, tag, params);
		DataLog.count(event);
	}

	
	private static void doLogToFile(String dataspace, String tag, Map params, String trckId, String via, WebRequest state) {
		String msg = params == null? "" : Printer.toString(params, ", ", ": ");
		msg += "\ttracker:"+trckId+"\tref:"+state.getReferer()+"\tip:"+state.getRemoteAddr();
		if (via!=null) msg += " via:"+via;
		// Guard against giant objects getting put into log, which is almost
		// certainly a careless error
		if (msg.length() > Log.MAX_LENGTH) {
			msg = StrUtils.ellipsize(msg, Log.MAX_LENGTH);
//			error = StrUtils.ellipsize(msg, 140)+" is too long for Log!";
		}
		// chop #tag down to tag (including embedded #, as in tr_#myapp)
		tag = tag.replace("#", "");
		tag = dataspace+"."+tag;
		// Note: LogFile will force the report onto one line by converting [\r\n] to " "
		// Add in referer and IP
		// Tab-separating elements on this line is useless, as Report.toString() will immediately convert \t to space.
		String msgPlus = msg+" ENDMSG "+state.getReferer()+" "+state.getRemoteAddr();
//		Report rep = new Report(tag, null, msgPlus, Level.INFO);
		Log.i(tag, msgPlus);
//		DataLogServer.logFile.listen2(rep.toStringShort(), rep.getTime());
	}

}
