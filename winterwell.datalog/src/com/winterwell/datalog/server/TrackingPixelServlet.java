package com.winterwell.datalog.server;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.winterwell.datalog.DataLog;
import com.winterwell.utils.web.WebUtils2;

import com.winterwell.utils.Utils;
import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.time.TUnit;
import com.winterwell.web.app.FileServlet;
import com.winterwell.web.app.WebRequest;
import com.winterwell.web.fields.AField;
import com.winterwell.web.fields.SField;
import com.winterwell.web.data.XId;

/**
 * See {@link Tripwire} E.g. use: <img src='http://myworkspace.soda.sh/trk?via=areseller'>
 * 
 * Referer url: explicitly set via ref=, or the referring page from the http header
 * 
 * @author daniel
 *
 */
public class TrackingPixelServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		WebRequest wr = new WebRequest(this, req, resp);
		processIncoming(wr);			
	}
	
	


	/**
	 * @param state
	 * @return can be null for do-not-track
	 */
	public static String getCreateCookieTrackerId(WebRequest state) {
		if (state==null) return null;
		String uid = WebUtils2.getCookie(state.getRequest(), "trkid");		
		if (uid!=null) return uid;
		boolean dnt = state.isDoNotTrack();
		// TODO if this user has opted-in, we can ignore DNT 
//		if (dnt) return null; FIXME
		uid = Utils.getRandomString(20)+"@trk";
		WebUtils2.addCookie(state.getResponse(), "trkid", uid, TUnit.YEAR.dt, DataLogServer.settings.COOKIE_DOMAIN);
		return uid;
	}
	
	static File PIXEL = new File("web/img/tracking-pixel.gif");


	private void processIncoming(WebRequest state) {	
		// Who are they? Track them across pages
		String uid = getCreateCookieTrackerId(state);
		String ref = Utils.or(state.get("ref"), state.getReferer());
		// transfer link properties to cookie? -- like the affiliate
		String via = processViaAffiliateTracking(state, ref);
		// serve the resource, so we can release the request
		try {
			FileServlet.serveFile(PIXEL, state);
		} catch (IOException e) {
			Log.w("img0", e);
		}
		String tag = "pixel";
		String dataspace = "good-loop"; // ??
		// log some stuff
		Map params = new ArrayMap(
				"user", "$user", 
				"useragent", "$useragent",
				"ip", "$ip",
				"url", "$url");
		// Count it
		LgServlet.doLog(state, dataspace, tag, via, params);

		
		// log it
		Log.d("track", state.getReferer()+" via: "+via+" uid: "+uid);		

//		// TODO Tag the email / tweet
//		XId xid = state.get(Fields.XID);
//		if (xid==null) {
//			return;
//		}		
//		TempTag tag = new Tripwire().getTrackerTag(xid, grp);
//		QueryFilter<DBText> qf = new QueryFilter(DBText.class);
//		qf.noStdFilters();
//		qf.addTag(tag);
//		qf.setSecurity(false);
//		IText text = qf.getUniqueResult();
//		if (text!=null) {
//			Tags.tag2(text, new TempTag(Tripwire.OPENED_TAG, tag.getGroupSlug()), KManual.BACKGROUND, false, null, null);
//			// was it part of a campaign mailshot?
//			XId parent = text.get(MailshotServlet.PARENT);
//			if (parent!=null) {
//				Stat.count(1, statTag(grp, parent));
//			}
//		}
	}


//	public static String[] statTag(String grp, XId parentTemplateText) {
//		 return new String[]{grp+"_"+Tripwire.OPENED_TAG, String.valueOf(parentTemplateText)};
//	}

	
	public static final SField VIA = new SField("via");
	
	/** TODO also track affiliate events server-side 
	 * @return via or null*/
	private String processViaAffiliateTracking(WebRequest state, String ref) {
		if (ref==null) return null;				
		String viav = VIA.getValue(ref);
		
//		// ??Does this work, or is it too sensitive to the ordering?? 
//		// intro: first-visit-only affiliate tracking. 
//		// If this is the first visit to the domain, then the introducer gets via (affiliate) credit.
//		// But if they reached this page as part of their journey, then there's no credit.
//		// Use-case: A reseller has their own landing pages on your site.
//		String domain = WebUtils2.getDomain(ref);				
//		if (domain!=null) {			
//			String intro = INTRO.getValue(ref);
//			String intro_d = "intro_"+domain;
//			String oldIntro = WebUtils2.getCookie(state.getRequest(), intro_d);
//			if (oldIntro==null) {
//				WebUtils2.addCookie(state.getResponse(), intro_d, Utils.or(intro, "organic"), TUnit.MONTH.dt, ".soda.sh");
//				// upgrade the intro to a via
//				if (viav==null) viav = intro;	
//			}			
//		}
		
		// via affiliate tracking		
		if (viav==null) {
//			Log.d("track", ref+" no via");
			return null;
		}
//		Log.d("track", ref+" via "+viav);
		// 1st affiliate wins -- 2nd affiliate does not override
		String oldVia = WebUtils2.getCookie(state.getRequest(), VIA.getName());
		if (oldVia==null) {
			WebUtils2.addCookie(state.getResponse(), VIA.getName(), viav, TUnit.MONTH.dt, ".soda.sh");
		} else if ( ! oldVia.equals(viav)) {
			Log.d("track", ref+" new-via "+viav+" skipped due to old-via "+oldVia);
		}
		// but do put all the affiliates into a chain
		String via2 = WebUtils2.getCookie(state.getRequest(), "via2");
		if (Utils.isBlank(via2)) {
			via2= viav; 
		} else if (via2.endsWith(viav)) {
			// no op (avoid repeats)
			return oldVia;
		} else {
			via2 += ","+viav; 
		}
		WebUtils2.addCookie(state.getResponse(), "via2", via2, TUnit.YEAR.dt, ".soda.sh");
		return oldVia==null? viav : oldVia;
	}
	
}
