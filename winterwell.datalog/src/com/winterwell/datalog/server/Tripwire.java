package com.winterwell.datalog.server;

import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import com.winterwell.utils.TodoException;
import com.winterwell.utils.Utils;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.time.TUnit;
import com.winterwell.utils.web.WebUtils;
import com.winterwell.utils.web.WebUtils2;
import com.winterwell.web.app.WebRequest;
import com.winterwell.web.fields.SField;

import com.winterwell.datalog.Stat;
import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.web.data.XId;

/**
 * Handle tripwires - pages which call the server when they're hit.
 * @author daniel
 *
 */
public class Tripwire {
	
	/**
	 * Associate an ip address (taken from the current request) with a user.
	 * @param user can be null which clears the association
	 */
	public void associateIP(RequestState rState, DBPerson user) {
		HttpServletRequest req = rState.getRequest();
		String remoteAddr = rState.getRemoteAddr();
		// TODO set ip to user ref
//		if (user==null) ips.remove(remoteAddr);
//		TODO else ips.put(remoteAddr, user.getId());		
	}

	public void createPageHitEvent(RequestState rState) {
		// this is the page with the tripwire
		HttpServletRequest req = rState.getRequest();
		String referringPage = req.getHeader("referer");
		Stat.count(1, "tripwire_page", referringPage);		
	}
	
	/**
	 * 
	 * @param state
	 * @param objectHit NB: this is often a {@link DBShortUrl} from the {@link XServlet} url shortener.
	 */
	public void createObjectHitEvent(RequestState state, IHasXId objectHit) {
		if (objectHit instanceof DBShortUrl) {
			// this is the url they're heading to			
			String referringPage =  
					((DBShortUrl)objectHit).getUrl();
			String shortCode = ((DBShortUrl) objectHit).getCode();
			Stat.count(1, "tripwire_click", referringPage, shortCode);
		} else {
			Stat.count(1, "tripwire_obj", objectHit.getXId().toString());
		}
	}
		
//	private void createEvent2(RequestState rState, IHasXId object, String referringPage) {		
//		// TODO distinguish the quality of info
//		DBPerson person = guessUser(rState);		
//		
//		// event
//		new DBEvent(new EventDescription(person, PAGE_HIT_TRIGGER, 
//					object, referringPage));
//	}


	/**
	 * Try to guess who a user is by unsecure methods (e.g. uxid cookie)
	 * @param state
	 * @return
	 */
	public DBPerson guessUser(RequestState state) {
		// logged in
		DBPerson user = DBPerson.getUser();
		if (user!=null) return user;
		
		// cookie
		HttpServletRequest req = state.getRequest();
		String cookie = WebUtils2.getCookie(req, Fields.USER_XID_COOKIE);
		// ...or parameter?
		if (cookie==null) {
			cookie = req.getParameter("USER_XID_COOKIE");
		}
		if (cookie!=null) {
			try {
				XId xid = new XId(cookie, false);
				user = DBPerson.unsecureFind(xid);
				if (user!=null) return user;
			} catch (Exception e) {
				// Not important
				Log.report("tripwire", "Strange user id cookie value: "+cookie, Level.INFO);
			}
		}
		
//		// IP
//		String remoteAddr = state.getRemoteAddr();
//		Long id = ips.get(remoteAddr);
//		user = PersistUtils.find(DBPerson.class, id);
//		return user;
		
		return null;
	}

	


	/**
	 * @see #setTripwire(boolean)
	 */
	public final boolean isTripwired() {
		throw new TodoException(); // TODO
	}
	

	/**
	 * TODO if true, views should trigger an event via the Tripwire plugin.
	 */
	public void setTripwire(DBObject object, boolean tripwire) {
		throw new TodoException(); // TODO
	}
	
	/**
	 * TODO convert all links to be shortened click-trackers
	 * @param text
	 */
	public void addTrackerLinks(DBText text) {
		
	}
	
	/**
	 * If an email, add a tracking pixel to the (html) contents. Does nothing otherwise.
	 * @param text
	 */
	public void addTrackerPixel(DBText text) {
		// Only for email (for now??)
		if ( ! Email.isEmailService(text.getService())) {
			return;
		}
		String msg = text.getContents();
		// What if it's not html??
		String mime = text.getMimeType();
		// A local link to a tracking pixel
		// See Img0Servlet and MasterHttpServlet
		String grp = text.getGroupSlug();
		if (grp==null) {
			grp = DBGroup.get().getSlug();
		}
		// The xid will change when we send :(
		// So tag it.
		XId xid = text.getXId();
		TempTag tag = getTrackerTag(xid, grp);
		Tags.tag2(text, tag, KManual.BACKGROUND, false, null, null);
		String url = "https://"+grp+".soda.sh/trk?xid="+WebUtils.urlEncode(xid.toString());
		msg += "<img width='1' height='1' src='"+WebUtils2.attributeEncode(url)+"'/>";
		text.setContents(msg);
		// NB: /trk is handled by Img0Servlet
	}
	
	/**
	 * REMOVE a tracking pixel from the (html) contents. Use-case: for putting mail into Sent, so you don't
	 * trigger your own tracker.
	 * @param text
	 */
	public static void removeTrackerPixel(DBText text) {
		// Only for email (for now??)
		if ( ! Email.isEmailService(text.getService())) {
			return;
		}
		String msg = text.getContents();
		// What if it's not html??
		String mime = text.getMimeType();
		// A local link to a tracking pixel
		// See Img0Servlet and MasterHttpServlet
		String grp = text.getGroupSlug();
		if (grp==null) {
			grp = DBGroup.get().getSlug();
		}
		String msg2 = removeTrackerPixel2(msg, grp);
		if (msg2.equals(msg)) {
			return;
		}
		Log.d("email", "Removing tracker pixel from "+text.getXId()+" "+text.getName());
		text.setContents(msg2);
	}

	/**
	 * 
	 * @param msg
	 * @param grp Can be null
	 * @return
	 */
	public static String removeTrackerPixel2(String msg, String grp) {
		if (grp==null) grp = "\\w+";
		Pattern img = Pattern.compile("<img[^>]+?src='https://"+grp+"\\.soda\\.sh/trk\\?xid=[^>]+?>");
		Matcher m = img.matcher(msg);
		String msg2 = m.replaceAll("");
		return msg2;
	}

	/**
	 * 
	 * @param state
	 * @return can be null
	 */
	public static String getVia(RequestState state) {
		if (state==null) return null;
		String uid = WebUtils2.getCookie(state.getRequest(), Img0Servlet.VIA.getName());
		if (uid!=null) return uid;
		uid = state.get(Img0Servlet.VIA);
		if (uid!=null) return uid;
		String ref = state.get("ref");
		if (ref==null) return null;		
		String viav = Img0Servlet.VIA.getValue(ref);
		return viav;
	}

}
