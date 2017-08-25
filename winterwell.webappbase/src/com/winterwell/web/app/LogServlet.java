package com.winterwell.web.app;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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
import com.winterwell.web.app.WebRequest;
import com.winterwell.web.app.WebRequest.KResponseType;
import com.winterwell.web.fields.AField;
import com.winterwell.web.fields.Form;
import com.winterwell.web.fields.IntField;
import com.winterwell.web.fields.SafeString;
import com.winterwell.web.fields.SelectField;

import com.winterwell.depot.Depot;
import com.winterwell.depot.Desc;


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
			Level level = Level.SEVERE;
			Log.report(AJAX_TAG_PREFIX+tag, msg, level);
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

//	@Override
//	protected void display(PageBuilder page, RequestState state) throws Exception 
//	{
//		// admin's only
//		if (state.getUser()==null) throw new NotLoggedInException();
//		if ( ! Security.canSuDo(state.getUser())) {
//			throw new SecurityException();
//		}
//	
//		displayLogLevel(page, state);
//		
//		page.append("<div class='form'>");
//		Form form = new Form(getPath());
//		form.setMethod("get");
//		form.startTable();
//		form.addRow("Number of lines", NUM_LINES);
//		form.addRow("Filter (case insensitive)", FILTER);
//
//		// Filter by date
//		DateFilter df = new DateFilter(state);
//		df.appendHtmlTo(page);
//		
//		form.appendHtmlTo(page.sb());
//		page.append("</div>\n");
//		
//		Integer n = state.get(NUM_LINES);		
//		if (n==null) n = 100;
//		// by date?
//		Time start = df.getStart();
//		Time end = df.getEnd();
//		if (start!=null && end != null) {
//			n = Math.max(n, 100000);
//		}		
//		String slug = state.getSlug();
//		
//		// What file?
//		File logFile;
//		if (slug != null && slug.equals("jetty")) {
//			logFile = new File(Statics.getWebAppDir(), "jetty.log");
//		} else if (state.get(Fields.DESC)!=null) {
//			Desc desc = state.get(Fields.DESC);
//			logFile = Depot.getDefault().getLocalPath(desc);
//			n = 100000; // loadsa data
//		} else {			
//			logFile = new File(Statics.getWebAppDir(), "log.txt");
//		}
//		
//		
//		page.append("<style> .SEVERE { color:rgb(200,0,0); } "
//				+ ".WARNING { color:rgb(100,0,0); } "
//				+ ".INFO { color:rgb(0,0,100); } "
//				+ ".FINE { color:rgb(0,100,0); }</style>\n");
//		// TODO read off the top 100 lines - use a TableWidget to give pages?
//		// TODO search - i.e. find lines that match a string
//		// FIXME: this relies upon GNU tail. Other unices differ.
//		Proc tail = new Proc("tail -n"+n+" "+logFile.getAbsolutePath());
//		tail.run();
//		tail.waitFor(10000);
//		String out = tail.getOutput();
//		// anti hack device (plus protects the layout)
//		out = WebUtils.stripTags(out);
//		String[] lines = out.split("\n");
//		// out = out.replace("\n", "<br/>\n");
//		String filter = state.get(FILTER);	
//		Period period = start!=null || end!=null? df.getPeriod() : null;
//		lines = filterEntries(filter, period, lines, 0, 1);
//		out = coloriseEntries(lines);
//		page.append(out);
//	}

	/**
	 * @param filter
	 * @param end TODO
	 * @param start TODO
	 * @param lines
	 * @return
	 */
	private String[] filterEntries(String filter, Period period, String[] lines, int before, int after) {
		if (filter==null && period==null) return lines;		
		boolean[] in = new boolean[lines.length];
		for (int i=0; i<lines.length; i++) {
			String line = lines[i];
			if (filter!=null && ! StrUtils.containsIgnoreCase(line, filter)) {
				continue;
			}
			if (period != null) {
				int ci = line.indexOf(']');
				if (ci==-1) continue;
				String date = line.substring(1,ci);
				Time time = new Time(date);
				if ( ! period.contains(time)) {
					continue;
				}
			}
			in[i] = true;
			for(int a=1; a <= after; a++) {
				if (i+a == lines.length) break;
				in[i+a] = true;
			}
			for(int b=1; b <= before; b++) {
				int ib = i - b;
				if (ib<0) break;
				in[ib] = true;
			}
		}		
		// ok - which ones?
		ArrayList<String> lines2 = new ArrayList();
		for (int i=0; i<lines.length; i++) {
			if (in[i]) lines2.add(lines[i]);
		}
		return lines2.toArray(StrUtils.ARRAY);
	}

	public static Pattern severityPattern() {
		return Pattern.compile("\\]\\s*([A-Z]+)");
	}
	
//	public static LogFile fastLog = new LogFile(new File(CreoleMain.WEBAPP_DIR, "fast.log"))
//							// keep 8 weeks of 1 week log files ??revise this??
//							.setLogRotation(new Dt(1, TUnit.WEEK), 8);
//	static {
//		Log.removeListener(fastLog);
//	}
	
//	/**
//	 * Log msg to fast.log file.  
//	 * @param req
//	 * @param resp
//	 * @throws IOException 
//	 */
//	public static void fastLog(WebRequest state) throws IOException {
//		HttpServletRequest req = state.getRequest();
//		HttpServletResponse resp = state.getResponse();
//		String msg = ErrorServlet.MSG.getValue(req);
//		if (Utils.isBlank(msg)) {
//			msg = state.toString();
//		}
//		// Guard against giant objects getting put into log, which is almost
//		// certainly a careless error
//		if (msg.length() > Log.MAX_LENGTH) {
//			msg = msg.substring(0, 500)
//					+ "... (message is too long for Log!)";
//			WebUtils2.sendError(400, msg, resp);
//			return;
//		}
//		String tag = TAG.getValue(req);
//		if (Utils.isBlank(tag)) {
//			WebUtils2.sendError(400, "no tag", resp);
//			return;
//		}
//		// chop #tag down to tag (including embedded #, as in tr_#myapp)
//		tag = tag.replace("#", "");
//		// Note: LogFile will force the report onto one line by converting [\r\n] to " "
//		// Add in referer and IP
//		// Tab-separating elements on this line is useless, as Report.toString() will immediately convert \t to space.
//		String msgPlus = msg+" ENDMSG "+state.getReferer()+" "+state.getRemoteAddr();
//		Report rep = new Report(AJAX_TAG_PREFIX+tag, null, msgPlus, Level.INFO);
//		fastLog.listen2(rep.toStringShort(), rep.getTime());
//
//		// json or jsonp?
//		if (state.getResponseType()==KResponseType.json || state.getResponseType()==KResponseType.jsonp) {
//			JsonResponse ok = new JsonResponse();
//			WebUtils2.sendJson(ok, state);
//		}
//		WebUtils2.sendText("OK", resp);
//	}

}
