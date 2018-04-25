package com.winterwell.datalog.server;

import java.io.File;
import java.io.IOException;

import com.winterwell.datalog.DataLogConfig;
import com.winterwell.utils.Dep;
import com.winterwell.utils.Utils;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.time.TUnit;
import com.winterwell.web.app.FileServlet;
import com.winterwell.web.app.IServlet;
import com.winterwell.web.app.WebRequest;
import com.winterwell.web.fields.SField;

/**
 * See {@link Tripwire} E.g. use: <img src='http://myworkspace.soda.sh/trk?via=areseller'>
 * 
 * Referer url: explicitly set via ref=, or the referring page from the http header
 * 
 * @author daniel
 *
 */
public class TrackingPixelServlet implements IServlet {
	private static final long serialVersionUID = 1L;
	static final String DATALOG_EVENT_TYPE = "pxl";	
	


	/**
	 * Make/retrieve a tracker cookie
	 * TODO move into YouAgain Client
	 * @param state Can be null (returns null)
	 * @return A nonce@trk XId. Can be null for do-not-track. Repeated calls will return the same uid. 
	 */
	public static String getCreateCookieTrackerId(WebRequest state) {
		if (state==null) return null;
		String uid = state.getCookie("trkid");	
		if (uid!=null) return uid;
		// Do not track?
		boolean dnt = state.isDoNotTrack();
		if (dnt) {
			// If this user has opted-in, we can ignore DNT TODO a way for them to set that
			String tkc = state.getCookie("tkc"); // tracking consent?
			if (tkc==null || tkc.equals("0")) {
				return null;	
			}
			// https://www.w3.org/TR/tracking-dnt/#tracking-status-value
			state.getResponse().setHeader("Tk", "C");
		}
		uid = Utils.getRandomString(20)+"@trk";
		DataLogConfig dls = Dep.get(DataLogConfig.class);
		state.setCookie("trkid", uid, TUnit.YEAR.dt, dls.COOKIE_DOMAIN);
		return uid;
	}
	
	static File PIXEL = new File("web/img/tracking-pixel.gif");


	public void process(WebRequest state) {	
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
		String tag = DATALOG_EVENT_TYPE;
		
		// Default to dataspace "trk" but allow override
		String dataspace = state.get(LgServlet.DATASPACE);
		if (dataspace == null) {
			dataspace = "trk";
		}
		// Count it
		LgServlet.doLog(state, dataspace, tag, 1, via, null, true);

		
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
		String oldVia = state.getCookie(VIA.getName());
		if (oldVia==null) {
			state.setCookie(VIA.getName(), viav, TUnit.MONTH.dt, ".soda.sh");
		} else if ( ! oldVia.equals(viav)) {
			Log.d("track", ref+" new-via "+viav+" skipped due to old-via "+oldVia);
		}
		// but do put all the affiliates into a chain
		String via2 = state.getCookie("via2");
		if (Utils.isBlank(via2)) {
			via2= viav; 
		} else if (via2.endsWith(viav)) {
			// no op (avoid repeats)
			return oldVia;
		} else {
			via2 += ","+viav; 
		}
		state.setCookie("via2", via2, TUnit.YEAR.dt, ".soda.sh");
		return oldVia==null? viav : oldVia;
	}

	
}
