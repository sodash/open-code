package com.winterwell.datalog.server;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.util.ajax.JSON;

import com.winterwell.datalog.DataLog;
import com.winterwell.datalog.DataLogEvent;
import com.winterwell.datalog.DataLogRemoteStorage;
import com.winterwell.utils.Printer;
import com.winterwell.utils.StrUtils;
import com.winterwell.utils.Utils;
import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.utils.containers.Containers;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.time.TUnit;
import com.winterwell.utils.time.Time;
import com.winterwell.utils.web.WebUtils2;
import com.winterwell.web.FakeBrowser;
import com.winterwell.web.app.AppUtils;
import com.winterwell.web.app.BrowserType;
import com.winterwell.web.app.FileServlet;
import com.winterwell.web.app.KServerType;
import com.winterwell.web.app.WebRequest;
import com.winterwell.web.app.WebRequest.KResponseType;
import com.winterwell.web.fields.BoolField;
import com.winterwell.web.fields.DoubleField;
import com.winterwell.web.fields.JsonField;
import com.winterwell.web.fields.SField;

import ua_parser.Client;
import ua_parser.Parser;


/**
 * Fast Ajax logging of stats.
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
 * @testedby {@link LgServletTest}
 */
public class LgServlet {

	static final SField TAG = new SField("t");
	public static final SField DATASPACE = new SField("d");

	public LgServlet() {		
	}
		
	
	static JsonField PARAMS = new JsonField("p");
	
	static final List<String> NOTP = Arrays.asList(TAG.getName(), DATASPACE.getName(), "via", "track");
	/**
	 * group-by ID for merging several events into one.
	 */
	public static final SField GBY = new SField("gby");
	
	/**
	 * Log msg to fast.log file.  
	 * @param req
	 * @param resp
	 * @throws IOException 
	 */
	public static void fastLog(WebRequest state) throws IOException {
		HttpServletRequest req = state.getRequest();
		HttpServletResponse resp = state.getResponse();
		String u = state.getRequestUrl();
		Map<String, Object> ps = state.getParameterMap();
		String ds = state.getRequired(DATASPACE);
		// TODO security check the dataspace?
		String tag = state.getRequired(TAG);
		double count = state.get(new DoubleField("count"), 1.0);
		// NB: dont IP/user track simple events, which are server-side
		boolean stdTrackerParams = ! DataLogEvent.simple.equals(tag) && state.get(new BoolField("track"), true);
		// Read the "extra" event parameters
		Map params = (Map) state.get(PARAMS);		
		if (params==null) {
			// params from the url?
			// e.g. 
			// https://lg.good-loop.com/lg?d=gl&t=install&idfa={idfa}&adid={adid}&android_id={android_id}&gps_adid={gps_adid}
			// &fire_adid={fire_adid}&win_udid={win_udid}&ua={user_agent}&ip={ip_address}&country={country}
			// &time={created_at}&app_id={app_id}&app_name={app_name}&store={store}&tracker_name={tracker_name}&tracker={tracker}
			// &bid={dcp_bid}
			// or use p.param for unambiguity
			// TODO which would we prefer??
			Map<String, String> smap = state.getMap();			
			params = new HashMap();
			for(Map.Entry<String, String> kv : smap.entrySet()) {
				String v = kv.getValue();
				if (v==null || v.isEmpty()) continue;
				String k = kv.getKey();
				if (NOTP.contains(k)) continue;
				if (k.startsWith("p.")) k = k.substring(2);				
				params.put(k, v);
			}
		}
		
		String gby = state.get(GBY);
		// log it!
		DataLogEvent logged = doLog(state, ds, gby, tag, count, params, stdTrackerParams);
		
		// Reply
		// .gif?
		if (state.getResponseType()==KResponseType.image) {
			FileServlet.serveFile(TrackingPixelServlet.PIXEL, state);
			return;
		}
		if (DataLogServer.settings.CORS) {
			WebUtils2.CORS(state, false);
		}
		WebUtils2.sendText(logged!=null? "OK" : "not logged", resp);
	}

	static List<Map> userTypeForIPorXId;
	static Time userTypeForIPorXIdFetched;
	
	/**
	 * 
	 * @param state
	 * @param dataspace
	 * @param tag
	 * @param count
	 * @param params can be null
	 * @param stdTrackerParams
	 * @return
	 */
	public static DataLogEvent doLog(WebRequest state, String dataspace, String gby, String tag, double count, 
			Map params, boolean stdTrackerParams) 
	{
		assert dataspace != null;		
		assert tag != null : state;
		String trckId = TrackingPixelServlet.getCreateCookieTrackerId(state);
		// special vars
		if (stdTrackerParams) {			
			params = doLog2_addStdTrackerParams(state, params, trckId);
		}
		
		// HACK remove Hetzner from the ip param 
		// TODO make this a config setting?? Or even better, the servers report their IP
		Object ip = params.get("ip");
		if (ip instanceof String) ip = ((String) ip).split(",\\s*");
		List ips = Containers.list(ip);
		if (ips.contains("5.9.23.51")) {
			ips = Containers.filter(ips, a -> ! "5.9.23.51".equals(a));
			if (ips.size() == 1) {
				params.put("ip", ips.get(0));
			} else {
				params.put("ip", ips);
			}
		}
		
		// screen out our IPs?
		if ( ! accept(dataspace, tag, params)) {
			return null;
		}
		
		// Add ip/user type
		String userType = getInvalidType(ips);
		Log.d(userType);
		if (userType!=null) {
			params.put("invalid", userType);
		}
		
		// write to log file
		doLogToFile(dataspace, tag, count, params, trckId, state);
				
		// write to Stat / ES
		// ...which dataspaces?
		// Multiple dataspaces: Dan A reports a significant cost to per-user dataspaces
		// -- he estimated one server per 4k ES indexes. c.f. #5403
		// Info should be stored to named dataspace + user + publisher + advertiser
		// TODO upgrade DatalogEvent to have several dataspaces??
//		ArraySet<String> dataspaces = new ArraySet(
//			dataspace, params.get("user") // publisher, advertiser			
//		);
//		for(String ds : dataspaces) {
//			if (ds==null) continue;
		DataLogEvent event = new DataLogEvent(dataspace, gby, count, new String[] { tag}, params);
//		event.time = state.get(time); FIXME
		DataLog.count(event);
//		}
		return event;
	}
	
	
	/**
	 * Is it a bot? works with Portal which holds the data
	 * @param ips
	 * @return
	 */
	private static String getInvalidType(List ips) {
		if (userTypeForIPorXId==null || userTypeForIPorXIdFetched==null || userTypeForIPorXIdFetched.isBefore(new Time().minus(10, TUnit.MINUTE))) {
			//Needs to be set first -- will get caught in a loop otherwise as userTypeForIPorXId is still null
			userTypeForIPorXIdFetched = new Time();			
			FakeBrowser fb = new FakeBrowser();
			fb.setRequestMethod("GET");

			try {
				//Right now, just set to point at local. TODO read in correct endpoint from state
				String json= fb.getPage("https://portal.good-loop.com/botip/_list.json");
				Map response = (Map) JSON.parse(json);
				Map esres = (Map) response.get("cargo");
				List<Map> hits = Containers.asList(esres.get("hits"));
				
				userTypeForIPorXId = hits;
			}
			catch(Exception ex) {
				ex.printStackTrace();
			}
		}
		//At this point, can safely assume that we have a valid list of IPs
		for (Object userIP : ips) {
				for(Map botIP : userTypeForIPorXId) {
					String badIP = (String) botIP.get("ip");
					if (userIP.equals(badIP)) return (String) botIP.get("type");
				}
		}
		return null;
	}


	static ua_parser.Parser parser;
	

	private static Map doLog2_addStdTrackerParams(WebRequest state, Map params, String trckId) {
		// TODO allow the caller to explicitly set some of these if they want to
		if (params==null) params = new ArrayMap();
		// Replace $user with tracking-id, and $
		params.putIfAbsent("user", trckId);			
		// ip: $ip
		params.putIfAbsent("ip", state.getRemoteAddr());
		// Browser info
		String ua = state.getUserAgent();			
		params.putIfAbsent("ua", ua);
			
		BrowserType bt = new BrowserType(ua);
		boolean mobile = bt.isMobile();		
		params.putIfAbsent("mbl", mobile);		
		// browser
		String browser;
		try {
			ua_parser.Parser _parser = uaParser();
			Client uac = _parser.parse(ua);
			browser = uac.userAgent.family+"_"+uac.userAgent.major;
		} catch(Throwable ex) {
			Log.d("lg", ex);
			browser = bt.getBrowserMake()+"_"+bt.getVersion();
		}
		params.putIfAbsent("browser", browser);
		// OS
		String os = bt.getOS();
		params.putIfAbsent("os", os);
		
		// what page?
		String ref = state.getReferer();
		if (ref==null) ref = state.get("site"); // DfP hack
		// remove some gumpf (UTM codes)
		String cref = WebUtils2.cleanUp(ref);
		if (cref != null) {
			params.putIfAbsent("url", cref);
			// domain (e.g. sodash.com) & host (e.g. www.sodash.com)				
			params.putIfAbsent("domain", WebUtils2.getDomain(cref)); 
			// host is the one to use!
			params.putIfAbsent("host", WebUtils2.getHost(cref)); // matches publisher in adverts
		}
		return params;
	}

	
	static Parser uaParser() throws IOException {
		if (parser==null) {
			parser = new Parser();
		}
		return parser;
	}


	/**
	 * HACK screen off our IPs and test sites
	 * 
	 * TODO instead do this by User, and have a no-log parameter in the advert
	 * 
	 * @param dataspace2
	 * @param tag2
	 * @param params2
	 * @return
	 */
	private static boolean accept(String dataspace, String tag, Map params) {
		KServerType stype = AppUtils.getServerType(null);
		// only screen our IPs out of production
		if (stype != KServerType.PRODUCTION) 
		{
			return true;
		}
		if ( ! "gl".equals(dataspace)) return true;
		Object ip = params.get("ip");
		List<String> ips = Containers.list(ip);		
		if ( ! Collections.disjoint(OUR_IPS, ips)) {
			Log.d("lg", "skip ip "+ip+" event: "+tag+params);
			return false;
		}
		if ("good-loop.com".equals(params.get("host"))) {
			String url = (String) params.get("url");
			// do track the marketing site, esp live demo, but otherwise no GL sites 
			if (url!=null) {
				if (url.contains("live-demo")) return true;
				if (url.contains("//www.good-loop.com")) return true;
				if (url.contains("//good-loop.com")) return true;
				if (url.contains("//as.good-loop.com")) return true;
			}
			Log.d("lg", "skip url "+url+" event: "+tag+params);
			return false;
		}
		return true;
	}

	static List<String> OUR_IPS = Arrays.asList("62.30.12.102", "62.6.190.196", "82.37.169.72");
	
	private static void doLogToFile(String dataspace, String tag, double count, Map params, String trckId, WebRequest state) {
		String msg = params == null? "" : Printer.toString(params, ", ", ": ");
		if (count != 1) msg += "\tcount:"+count;
		msg += "\ttracker:"+trckId+"\tref:"+state.getReferer()+"\tip:"+state.getRemoteAddr();
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
		
		// error or warning?
		if (tag.contains("error")) {
			// Reduced to "warning" so we don't spam LogStash alert emails.
			Log.w(tag, msgPlus); 
		} else if (tag.contains("warning")) {
			Log.w(tag, msgPlus);
		} else {
			// normal case
			Log.i(tag, msgPlus);
		}
	}

}
