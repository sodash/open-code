package com.winterwell.web.app;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.winterwell.utils.Environment;
import com.winterwell.utils.IBuildStrings;
import com.winterwell.utils.IProperties;
import com.winterwell.utils.Key;
import com.winterwell.utils.Printer;
import com.winterwell.utils.ReflectionUtils;
import com.winterwell.utils.StrUtils;
import com.winterwell.utils.Utils;
import com.winterwell.utils.WrappedException;
import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.utils.containers.Containers;
import com.winterwell.utils.io.ConfigBuilder;
import com.winterwell.utils.io.FileUtils;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.time.Dt;
import com.winterwell.utils.time.StopWatch;
import com.winterwell.utils.web.WebUtils;
import com.winterwell.utils.web.WebUtils2;
import com.winterwell.web.WebInputException;
import com.winterwell.web.ajax.AjaxMsg;
import com.winterwell.web.data.XId;
import com.winterwell.web.fields.AField;
import com.winterwell.web.fields.Checkbox;
import com.winterwell.web.fields.FileUploadField;
import com.winterwell.web.fields.MissingFieldException;
import com.winterwell.web.fields.SField;
import com.winterwell.web.test.TestHttpSession;

/**
 * The state of this request. Has properties and some special fields.
 * 
 * @testedby {@link WebRequestTest}
 * @author daniel
 * 
 */
public class WebRequest implements IProperties, Closeable {


	public void removeCookie(String cookieName, String cookieDomain) {
		String path = "/";;
		// is null null valid??
		if (freshCookies!=null) freshCookies.remove(cookieName);
		WebUtils2.removeCookie(cookieName, getResponse(), cookieDomain, path);
	}
	
	/**
	 * Uses path=/ Call {@link WebUtils2#addCookie(HttpServletResponse, String, Object, Dt, String)} for more options.
	 * 
	 * @param cookieDomain .mysite.com means cookies will work across mysite.com, abc.mysite.com and www.mysite.com 
	 * @return 
	 * @see WebUtils2#addCookie(HttpServletResponse, String, Object, Dt, String)
	 */
	public Cookie setCookie(String name, String value, Dt timeTolive, String cookieDomain) {
		if (freshCookies==null) freshCookies = new ArrayMap();
		freshCookies.put(name, value);
		return WebUtils2.addCookie(getResponse(), name, value, timeTolive, cookieDomain);
	}

	/**
	 * The request url, without any path info.
	 * @return e.g. http://bbc.co.uk
	 */
	public String getRequestProtocolHost() {
		StringBuffer rurl = request.getRequestURL();
		int i = rurl.indexOf("/", 9);
		return i==-1? rurl.toString() : rurl.substring(0, i);
	}

	Map<String,String> freshCookies;
	
	/**
	 * 
	 * @param name
	 * @return cookie value -- a fresh cookie from {@link #setCookie(String, String, Dt, String)}
	 * or (normal) from the request.
	 */
	public String getCookie(String name) {
		if (freshCookies!=null) {
			String v = freshCookies.get(name);
			if (v!=null) return v;
		}
		return WebUtils2.getCookie(getRequest(), name);
	}

	/**
	 * Without the subdomain, e.g. "sodash.com" from http://foo.sodash.com/bar
	 */
	public String getDomain() {
		String url = getRequestUrl();
		assert(url.contains("://")) : url+" "+this;
		return WebUtils.getDomain(url);
	}
	
	/**
	 * aka the host part of the url, e.g. foo.sodash.com from http://foo.sodash.com/bar
	 */
	public String getFullDomain() {
		String url = getRequestUrl();
		assert(url.contains("://")) : url+" "+this;
		return WebUtils.getHost(url);
	}
	
	/**
	 * Get the IP address of the remote connection. This method (unlike {@link HttpServletRequest#getRemoteAddr()})
	 * is proxy aware and retrieve the <b>assumed true</b> remote IP from proxy headers, if present.
	 * Do not trust this for security purposes!
	 * 
	 * Ref: https://en.wikipedia.org/wiki/X-Forwarded-For 
	 * @return the IP address as a String
	 */
	public String getRemoteAddr() {
		String fip = request.getHeader("X-Forwarded-For"); // The standard for remote proxies
		if (fip == null) {
			fip = request.getHeader("X-Real-IP"); // The local nginx proxy can be setup to provide this 
			// see https://rtcamp.com/tutorials/nginx/forwarding-visitors-real-ip/
		}
		if (fip == null) fip = request.getRemoteAddr();
		return fip;
	}
	
	/**
	 * @return the referring page (if the header is given). Or null.
	 * Never blank. 
	 */
	public String getReferer() {
		HttpServletRequest req = getRequest();
		String referringPage = req.getHeader("referer");
		if (Utils.isBlank(referringPage)) return null;
		return referringPage;
	}

	/**
	 * What type of response to send back. E.g. a full web page or a JSON
	 * snippet.
	 * 
	 * HACK: This basically special-cases some special mime-types.
	 */
	public enum KResponseType {
		// /** A JavaScript library - loaded via a script tag to get round
		// * the cross-site scripting block. This will typically finish by
		// invoking
		// * a user-specified callback function */
		// js,
		/**
		 * Html for the core part of the page - probably an AJAX widget loading
		 * its contents
		 */
		body(WebUtils.MIME_TYPE_HTML),
		/** a comma-separated table, suitable for importing into a spreadsheet */
		csv("text/csv"),
		/**
		 * A browser asking for a web page. This is the default, and may be
		 * assumed if null.
		 */
		html("text/html"),
		/** An AJAX or script request which should be replied to in javascript. */
		js(WebUtils.MIME_TYPE_JAVASCRIPT),
		/** An AJAX or 3rd party request, which should be replied to in JSON. */
		json(WebUtils.MIME_TYPE_JSON),
		/** A cross-domain AJAX or 3rd party request, which should be replied to in JSON-P. */
		jsonp(WebUtils.MIME_TYPE_JSONP),
		/** a pdf file */
		pdf("application/pdf"),
		/** images (gif, png, jpeg*/
		image("image"),
		/** an RSS feed */
		rss(WebUtils.MIME_TYPE_RSS),
		/**
		 * An AJAX or 3rd party request, which should be replied to using plain
		 * text.
		 */
		txt("text/plain"),
		/**
		 * xhtml -- who uses it? annoying people :( <br>
		 * WARNING: This is currently converted to html!
		 * */
		xhtml("application/xhtml+xml"),
		/** An Excel spreadsheet (pre-97 format) */
		xls("application/vnd.ms-excel"),
		/** An Excel spreadsheet (modern format) */
		xlsx(
				"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
		/** An AJAX or 3rd party request, which should be replied to in XML. */
		xml(WebUtils.MIME_TYPE_XML),
		
		ical("text/calendar"),
		/** a url */
		link("text/url");

		public final String mimeType;

		private KResponseType(String mimeType) {
			this.mimeType = mimeType;
		}
		
		/**
		 * String values of the response type values.
		 * Use-case like valueOf, but without an exception if it isnt on the list.
		 */
		public static final List<String> strings = Containers.apply(Arrays.asList(KResponseType.values()), StrUtils.STR);		
	}

	public static final String ACTION_PARAMETER = "action";

	private static final Checkbox DEBUG = new Checkbox("debug");

	/**
	 * An HTML formatted message for the user (in an APageServlet).
	 */
	public static final Key<List<AjaxMsg>> KEY_MESSAGES = new Key(
			"AServlet.messages");

	// private final static Map defaultProperties = new HashMap<Key, Object>();
	public static final Key<WebRequest> KEY_REQUEST_STATE = new Key<WebRequest>(
			"rState");

	/**
	 * Indicates: if there is an error (eg a missing form field), please
	 * redirect to this url.
	 */
	public static final AField<String> REDIRECT_ON_ERROR = new AField<String>(
			"errlink", "hidden");

	/**
	 * Indicates: please redirect after whatever process currently underway is
	 * complete. Converted into a redirect by individual servlets as
	 * appropriate.
	 */
	public static final AField<String> REDIRECT_REQUEST = new AField<String>(
			"link", "hidden");

	/**
	 * For use with {@link #REDIRECT_REQUEST}. Indicates
	 * "send me back to the current page".
	 */
	public static final String REDIRECT_REQUEST_BACK = "back";

	/**
	 * A dangerous global flag which converts the xhtml KResponseType to html.
	 */
	static boolean TREAT_XHTML_AS_HTML = true;

	private static final Key<IProperties> USER = new Key<IProperties>("user");

	static final String _LOOP_CHECK = REDIRECT_REQUEST + "="
			+ REDIRECT_REQUEST_BACK;
	
	private String action;
	
	public void setAction(String action){
		this.action = action;
	}
	/**
	 * Normally false. Set by using the parameter debug=true in the url. If
	 * true, this is a (insecure) request for extra debug info.
	 */
	public final boolean debug;

	@Deprecated // Is this used??
	private final IProperties fieldProps;

	private IBuildStrings page;

	protected final Map<Key, Object> properties = new HashMap<Key, Object>();

	public Map<Key, Object> getOverriddenProperties() {
		return properties;
	}
	
	private String redirect;

	protected final HttpServletRequest request;

	final HttpServletResponse response;

	/**
	 * never null (defaults to html)
	 */
	private final KResponseType rType;

	/**
	 * The servlet handling this request
	 */
	protected final Object servlet;

	private final StopWatch stopWatch = new StopWatch();

	/**
	 * Note: This is set by {@link #doAuthenticate()} which is not called until
	 * after the constructor.
	 */
	protected IProperties user;

	private transient HttpSession tempSession;

	public WebRequest(HttpServletRequest request, HttpServletResponse response) {
		this(null, request, response);
	}
	
	/**
	 * Create a new WebRequest. Takes care of:
	 * 
	 * - Sets its own fields of course - Sets properties for AField to pick up
	 * on
	 * 
	 * Does not authenticate the user - call {@link #doAuthenticate()} to do so!
	 * This is because that can throw an exception.
	 * 
	 * @param servlet Can be null
	 * @param user Can be null
	 * @param request Cannot be null
	 * @param response Can be null -- but may cause problems
	 */
	public WebRequest(Object servlet, HttpServletRequest request,
			HttpServletResponse response) {
		assert request != null;
		this.servlet = servlet;
		this.request = request;
		this.response = response;
		// are we debugging?
		debug = request == null || DEBUG.isSet(request);
		Environment.get().setDebug(debug);
		// clean up action a bit
		// Action FIXME this breaks for multipart-form encoding!!!
		String action0 = request.getParameter(ACTION_PARAMETER);
		if (action0 != null) {
			if (Utils.isBlank(action0)) {
				action0 = null;
			} else {
				action0 = action0.intern();
			}
		}
		this.action = action0;

		// Field props
		fieldProps = AField.setRequest(this); // This is for AField to pick up
												// on user input
		// response type (defaults to html)
		rType = extractResponseType();
		assert rType != null : request.getRequestURI();
		// Redirect
		redirect = REDIRECT_REQUEST.getValue(request);
		// Make the state available
		Environment.get().put(KEY_REQUEST_STATE, this);
	}
	
	/**
	 * @return The last WebRequest in the current thread. Which might be an old closed one. Or null.
	 */
	public static WebRequest getCurrent() {
		return Environment.get().get(KEY_REQUEST_STATE);
	}

	/**
	 * @param actn
	 * @return true if there is an action and actn is part of it
	 */
	public final boolean actionContains(String actn) {
		// TODO use a word-boundary regex instead
		return action != null && action.contains(actn);
	}

	/**
	 * Just a slightly prettier convenience for a common use.
	 * 
	 * @param action
	 *            Must not be null
	 * @return true if the action parameter is equals() to action
	 */
	public final boolean actionIs(String myAction) {
		assert myAction != null;
		return Utils.equals(action, myAction);
	}

	/**
	 * Add a message for display to the user in a page. Multiple messages are
	 * supported. The message is stored in session attributes, and
	 * cleared by the first servlet that chooses to {@link #popMessages()} and
	 * send them to the user.
	 * This means the message from one servlet might be displayed by another,
	 * e.g. allowing for redirects.
	 * 
	 * @param message Cannot be null or empty.
	 * @param props
	 * @see #KEY_MESSAGES
	 * @see #popMessages()
	 */
	public final void addMessage(AjaxMsg message) {
		assert message!=null;
		// log all messages
		Log.i("AjaxMsg", message.getText()+"\tid:"+message.getId()+"\tstate:"+this);		
		// Allow messages to travel across requests  within a session
		HttpSession session = getSession();
		List<AjaxMsg> msgs = WebUtils2.getAttribute(session, KEY_MESSAGES);
		if (msgs == null) {
			msgs = new ArrayList<AjaxMsg>(4);
		}
		msgs.add(message);
		WebUtils2.setAttribute(session, KEY_MESSAGES, msgs);		
	}		
	
	static private boolean stateless;
	
	public static void setStateless(boolean stateless) {
		WebRequest.stateless = stateless;
	}
	
	/**
	 * 
	 * @return session. never null. if stateless, then this is a disconnected "dummy" session.
	 */
	public HttpSession getSession() {
		if (stateless) {
			if (tempSession==null) {
				Log.w("web", "Using a transient 'session' which will not persist across requests.");
				// a disconnect dummy (to avoid initialising session management with a session cookie)
				tempSession = new TestHttpSession();
			}
			return tempSession;
		}
		
		HttpSession s = request.getSession();
		if (s!=null) {
			if (tempSession!=null) {
				Log.e("web", "Transient and proper session?! "+this);
			}
			return s;
		}
		// hack
		Log.w("web", "Using a transient 'session' which will not persist across requests.");
		tempSession = new TestHttpSession();
		return tempSession;
	}

	/**
	 * Close the request.input & response.output streams.
	 */
	@Override
	public void close() {
		close(request, response);
	}

	/**
	 * @see com.winterwell.utils.IProperties#containsKey(winterwell.utils.Key) This
	 *      checks both the local properties bag and the http request parameters.
	 *      It will return true for explicit null parameters -- eg the query foo=&bar=1 would contain both the foo and bar keys. 
	 */
	@Override
	public final <T> boolean containsKey(Key<T> key) {
		if (properties.containsKey(key))
			return true;
		String kn = key.getName();
		String p = request.getParameter(kn);
		return p != null;
	}

	/**
	 * Use the request url ending for preference, but falls back to the
	 * *request* content-type header.
	 * 
	 * @param request
	 * @return never null (defaults to html)
	 */
	private KResponseType extractResponseType() {
		String pi = request.getRequestURI();
		if (pi != null) {
			String type = FileUtils.getType(pi);
			if ( ! type.isEmpty()) {
				// a bit convoluted, but it avoids catching an exception as a regular thing				
				int i = KResponseType.strings.indexOf(type);
				// e.g. .json or .html
				if (i!=-1) return KResponseType.values()[i];
				// a known type?
				if (FileUtils.IMAGE_TYPES.contains(type)) {
					return KResponseType.image;
				}
			}
		}
		// fall back to content-type header
		String cType = request.getHeader("Accept");
		if (cType == null) {
			cType = request.getContentType();
		}

		return extractResponseType2(cType);
	}

	/**
	 * Response type from url ending or content-header
	 **/
	KResponseType extractResponseType2(String cType) {
		// html as default
		if (cType == null)
			return KResponseType.html;

		// prefer html, e.g. given
		// "application/xml,application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,image/png,*/*;q=0.5"
		if (cType.contains("text/html"))
			return KResponseType.html;

		for (KResponseType rt : KResponseType.values()) {
			if (cType.startsWith(rt.mimeType)) {
				if (rt == KResponseType.xhtml && TREAT_XHTML_AS_HTML)
					return KResponseType.html;
				return rt;
			}
		}

		// catch some variants
		if (cType.contains("json"))
			return KResponseType.json;
		if (cType.contains("javascript"))
			return KResponseType.js;
		if (cType.contains("html"))
			return KResponseType.html;

		// This causes problems with Scotrail who are requesting the wrong
		// thing:
		// namely
		// "image/gif, image/x-xbitmap, image/jpeg, image/pjpeg, application/x-ms-application, application/x-ms-xbap, application/vnd.ms-xpsdocument, application/xaml+xml, application/vnd.ms-powerpoint, application/vnd.ms-excel, application/msword"
		// if (cType.contains("xml")) return KResponseType.xml;

		// default to html
		return KResponseType.html;
	}

	/**
	 * Retrieve the value corresponding to the specified key, or null if unset.
	 * This method looks for the key in a sequence of locations:
	 * <ol>
	 * <li>The local property bag (converting from String if the key is an
	 * AField)
	 * <li>(<i>if</i> the key is also an AField) The request object. This does
	 * <i>not</i> throw a {@link MissingFieldException} if the value is unset
	 * but required.
	 * <li>(<i>if</i> the key is also an AField) The request cookies.
	 * <li>The default property bag
	 * </ol>
	 * 
	 * @param key
	 *            This should usually be an AField!
	 * @see com.winterwell.utils.IProperties#get(winterwell.utils.Key)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public <T> T get(Key<T> key) {
		assert key != null;
		// From the bag?
		Object v = properties.get(key);
		if (v != null) {
			// Do we need to convert it?
			if (v instanceof String && key instanceof AField) {
				try {
					return ((AField<T>) key).fromString((String) v);
				} catch (Exception e) {
					throw Utils.runtime(e);
				}
			}
			return (T) v;
		}

		// Failing that, retrieve it from the request
		if (key instanceof AField) {
			AField field = ((AField) key);
			try {
				// as a form parameter
				v = field.getValue(getRequest());
				if (v != null)
					return (T) v;
				// Fall back to cookie
				String cv = getCookie(key.getName());
				if (cv != null)
					return (T) field.fromString(cv);
			} catch (MissingFieldException e) {
				// ignore -- why?! ^Dan W June 2018
			} catch (Exception e) {
				throw Utils.runtime(e);
			}
		} else {
			// Did someone use a Key where they wanted an AField?
			// return raw String & log a warning
			assert request != null : this;
			String vs = getRequest().getParameter(key.getName());
			if (vs != null) {
				IllegalArgumentException ex = new IllegalArgumentException(key
						+ " should be an AField! But chill we handled it.");
				Log.report(ex);
				return (T) vs;
			}
		}
		// // Fall back to defaults
		// v = defaultProperties.get(key);
		return (T) v; // null
	}

	/**
	 * Convenience wrapper around {@link #get(Key)}. Returns defaultValue if no
	 * value has been set.
	 */
	public final <T> T get(Key<T> key, T defaultValue) {
		if (defaultValue == null) {
			Log.w("web", "null default for "+key);
		}
		T result = get(key);
		if (result == null)
			return defaultValue;
		return result;
	}
	
	/**
	 * Convenience for {@link #get(Key)} with SField
	 * @param fieldName
	 * @return
	 */
	public String get(String fieldName) {
		return get(new SField(fieldName));
	}
	

	/**
	 * Get the action field.
	 * 
	 * The following usage pattern is encouraged as it eliminates the need to
	 * null check getAction(): <code>
	 * state.actionIs(MY_ACTION_STRING)
	 * </code>
	 * 
	 * @return Can be null, never blank.
	 */
	public final String getAction() {
		return action;
	}

	/**
	 * Set values here to alter what {@link AField#getHtml()} displays. Note
	 * that setting null will *not* ensure a blank field.
	 */
	@Deprecated
	// returns this
	public IProperties getFieldProps() {
		return fieldProps;
	}

	/**
	 * Retrieve the keyset associated with the property bag. This
	 * <b>excludes</b> request fields ('cos we don't know how to convert them).
	 * 
	 * @see com.winterwell.utils.IProperties#getKeys()
	 * @see HttpServletRequest#getParameterNames()
	 * @see HttpServletRequest#getParameterMap()
	 * @Deprecated dangerous - provided for interface compatibility only
	 */
	@SuppressWarnings("unchecked")
	@Override
	@Deprecated
	// dangerous - provided for interface compatibility only
	public Collection getKeys() {
		return properties.keySet();
	}


	/**
	 * Returns a parameter map without the garbled empty stuff which can occur,
	 * and adding in explicitly set {@link #properties}.
	 * @return {parameter: String[] if from the request, or Object if poked} 
	 * @see #getMap() which will convert String[] into String 
	 */
	public final Map<String,Object> getParameterMap() {
		// How does this interact with .getPostBody()??
		Map params = getRequest().getParameterMap();
		
		HashMap modParams = new HashMap();
		modParams.putAll(params);
		ArrayList<String> killParams = new ArrayList();
		//		cleanup nothing =
		modParams.remove("");
		modParams.remove("_");
		modParams.remove("=");
		for(Object param: modParams.keySet()){
			if (param instanceof String && ((String) param).endsWith("%3D")){
				killParams.add((String) param);
			}
		}
		for (String killParam : killParams){
			modParams.remove(killParam);
		}
		//		add in properties 
		for(Map.Entry<Key,Object> e : properties.entrySet()) {
			Object v = e.getValue();
			modParams.put(e.getKey().getName(), v);
		}
		return modParams;
	}
	
	/**
	 * Returns a parameter map without the garbled empty stuff which can occur, and converting values into
	 * a single String value. If a parameter has several non-null String values, only the first is returned. 
	 * @return {parameter: String} This is a new map which does not share state with the request.
	 */
	public final Map<String,String> getMap() {
		Map<String, Object> map = getParameterMap();
		Map<String, String> map2 = new HashMap();
		for(String key : map.keySet()) {
			Object v = map.get(key);
			if (v==null) continue;
			if (v.getClass().isArray()) {
				List<Object> vl = Containers.asList(v);
				v = null;
				for (Object vi : vl) {
					if (vi!=null) {
						v = vi;
						break;
					}
				}
				if (v==null) continue;
			}
			map2.put(key, v.toString());
		}
		return map2;
	}
	
	/**
	 * Will be null until {@link #setPage(IBuildStrings)} is called!
	 * 
	 * @return
	 */
	public IBuildStrings getPage() {
		return page;
	}

	/**
	 * @return requested redirect, or null. This redirect is for when the
	 *         current operation successfully completes, and is not done unless
	 *         you call {@link #sendRedirect()}.
	 *         <p>
	 *         Can be set via {@link #setRedirect(String)} or by the request itself using 
	 *         {@link #REDIRECT_REQUEST} (e.g. put link=myurl into the request url)
	 */
	public final String getRedirect() {
		return redirect;
	}

	public final HttpServletRequest getRequest() {
		return request;
	}

	/**
	 * @return the servlet and slug, but not the query parameters, e.g.
	 *         "/view/myPage"
	 */
	public String getRequestPath() {
		String pi = request.getPathInfo();
		// possibly equivalent?? request.getRequestURI()
		// StringBuffer link = request.getRequestURL();
		return pi;
	}

	/**
	 * Convenience for {@link WebUtils2#getRequestURL(HttpServletRequest)}.
	 * 
	 * @return the full url - including protocol, host, port, path and get query
	 *         parameters. TODO ??This should not be url-decoded??
	 */
	public String getRequestUrl() {
		return WebUtils2.getRequestURL(getRequest());
	}

	/**
	 * Convenience for getting a field that must not be null
	 * 
	 * @param field
	 * @return field-value. never null
	 * @throws MissingFieldException
	 *             if unset
	 * @throws WebInputException
	 *             if the field value does not parse
	 */
	public <X> X getRequired(AField<X> field) throws MissingFieldException {
		X x = get(field);
		if (x == null)
			throw new MissingFieldException(field, "from "+this);
		return x;
	}

	public final HttpServletResponse getResponse() {
		return response;
	}

	public final KResponseType getResponseType() {
		return rType;
	}

	/**
	 * The servlet handling this request. Can be null
	 */
	public Object getServlet() {
		return servlet;
	}

	/**
	 * Over-ride to handle servlet paths, e.g. as done in Creole
	 * 
	 * @return
	 */
	protected String getServletPath() {
		return "";
	}
	
	@SuppressWarnings("unchecked")
	public <X> X getSessionAttribute(Key<X> key) {
		HttpSession session = getSession();
		Object v = session.getAttribute(key.getName());
		return (X) v;
	}
	
	public String getSubDomain() {
		String url = request.getRequestURI();
		String host = WebUtils2.getHost(url);
		String domain = WebUtils2.getDomain(url);
		if (host==null) { // caused by relative urls
			String u2 = getRequestUrl();
			host = WebUtils2.getHost(u2);	
			domain = WebUtils2.getDomain(u2);
		}
		if (host.length()==domain.length()) {
			return null;
		}
		String sub = host.substring(0, host.length() - domain.length() - 1);
		return sub;
	}

	/**
	 * WARNING: this removes .html .json etc -- but it can also remove .stuff you want!
	 * 
	 * @return the page slug, or null if none. The slug is url decoded. E.g. if
	 *         the url is http://foobar.com/servlet/red%20fish/sub?x=1, the slug
	 *         is "red fish/sub" The final file-type ending is removed, so
	 *         http://foobar.com/wibble.html would give the slug "wibble"
	 * 
	 * @see #getSlugBits()
	 */
	public final String getSlug() {
		// NB getPathInfo() already returns URL decoded path
		String pi = getRequestPath();
		if (pi == null)
			return null;
		assert pi.startsWith("/") : "no / to start slug?! "+pi+" "+this;
		boolean keepFileType = false;
		// eg. /profile/ has no slug
		if (pi.endsWith("/")) {
			pi = pi.substring(0, pi.length() - 1);
			keepFileType = true; // its not a file type!
		}
		String servletPath = getServletPath();
		if (pi == null || pi.length() <= servletPath.length())
			return null;
		assert pi.startsWith(servletPath) : pi + " vs " + servletPath;
		if (pi.charAt(servletPath.length()) == '.')
			// instead of a trailing /slug we've found a trailing .type
			return null;
		pi = pi.substring(servletPath.length() + 1); // remove initial servlet
														// path plus trailing /
		// remove file type
		if ( ! keepFileType) {
			pi = FileUtils.getBasenameCautious(pi);
		}
		return pi;
	}

	/**
	 * Convenience method for {@link #getSlug()} where the slug is split up by
	 * the path separator /. Never returns null - returns an empty array
	 * instead.
	 */
	public final String[] getSlugBits() {
		String slug = getSlug();
		if (slug == null)
			return StrUtils.ARRAY;
		String[] bits = slug.split("/"); // SHould we split on // as well?
		return bits;
	}
	
	/**
	 * Convenience for might-be-null/undefined
	 * @param i 0-indexed
	 * @return slug-bit i or null
	 */
	public final String getSlugBits(int i) {
		String[] bits = getSlugBits();
		if (bits.length <= i) return null;
		String bi = bits[i];
		// Treat undefined and null as badly handled nulls
		if (Utils.isBlank(bi) || "undefined".equals(bi) || "null".equals(bi)) {
			Log.w("web", "Interpreted-as-null "+i+"="+bi+" in url "+getRequestUrl());
			return null;
		}
		return bi;
	}

	public StopWatch getStopWatch() {
		return stopWatch;
	}

	/**
	 * Use with {@link #setUser(XId, IProperties)}
	 * @return
	 */
	public IProperties getUser() {
		if (user == null) {
			user = getSessionAttribute(USER);
		}
		return user;
	}
	
	

	/**
	 * @return true if the request is still being processed.
	 */
	public boolean isOpen() {
		return !response.isCommitted();
	}

	@Override
	public boolean isTrue(Key<Boolean> key) {
		Boolean v = get(key);
		return v != null && v;
	}

	/**
	 * Get & clear the session messages for the user as set by {@link #addMessage(String)}.  
	 * @return often empty, never null
	 */
	public List<AjaxMsg> popMessages() {
		HttpSession session = getSession();
		List<AjaxMsg> alerts = WebUtils2.getAttribute(session, KEY_MESSAGES);
		WebUtils2.setAttribute(session, KEY_MESSAGES, null);
		return alerts;
	}

	public boolean containsMessageWithId(String id) {
		HttpSession session = getSession();
		List<AjaxMsg> alerts = WebUtils2.getAttribute(session, KEY_MESSAGES);
		if (alerts == null) return false;
		for (AjaxMsg alert :alerts){
			if (alert.getId().equals(id)){
				return true;
			}
		}
		return false;
	}
	
	
	/**
	 * This must be explicitly called if working with multipart form encoding.
	 * It processes the form, storing the parameters as normal (ie. they can
	 * then be accessed using {@link #get(Key)} as for other forms).
	 * <p>
	 * If the request was not a multipart form, this will harmlessly no-op. 
	 * 
	 * @param fields
	 *            These will be used to convert incoming string values. All
	 *            incoming Files and Strings are passed on so it is not
	 *            necessary to pass in String or File valued fields. This map is
	 *            purely for converting non-String fields. Can be null
	 * @param rState
	 */
	public final void processMultipartIncoming(Map<String, AField> fields) {
		if (fields == null) {
			fields = Collections.EMPTY_MAP;
		}
		// Process all form Fields
		Map<String, Object> vars = FileUploadField
				.processFormFields(getRequest());
		// Store 'em here
		try {
			for (Entry<String, Object> entry : vars.entrySet()) {
				// Convert if we have a field
				AField key = fields.get(entry.getKey());
				Object v = entry.getValue();
				if (key == null) {
					Key k = new Key(entry.getKey());
					put(k, v);
					continue;
				}
				// convert the value
				v = key.fromString((String) v);
				put(key, v);
			}
		} catch (Exception e) {
			throw new WebInputException(e.getMessage());
		}		
		// redirect?
		// (as this is not picked up by the normal mechanism in the constructor)
		redirect = (String) properties.get(REDIRECT_REQUEST);
	}

	

    /**
     * Utility method that determines whether the request contains multipart
     * content.
     * 
     * @author Based on Apache Commons FileUpload 
     *
     * @param request The servlet request to be evaluated. Must be non-null.
     *
     * @return <code>true</code> if the request is multipart;
     *         <code>false</code> otherwise.
     */
    public final boolean isMultipartContent() {
        if ( ! isPOST() && ! isPUT()) {
            return false;
        }
        String contentType = request.getContentType();
        if (contentType == null) {
            return false;
        }
        if (contentType.toLowerCase().startsWith("multipart/")) {
            return true;
        }
        return false;
    }

	
	@SuppressWarnings("unchecked")
	@Override
	public <T> T put(Key<T> key, T value) {
		// action? Also set the action field
		if ("action".equals(key.getName())) {
			setAction(value==null? null : value.toString());
		}
		
		if (value == null)
			return (T) properties.remove(key);
		else
			return (T) properties.put(key, value);
	}

	/**
	 * Send a page of html. Closes the response object. Convenience method for
	 * {@link WebUtils2#sendHtml(String, HttpServletResponse)}.
	 * <p>
	 * You must call {@link #setPage(IBuildStrings)} first to use this.
	 */
	public void sendPage() {
		assert page != null : "Use setPage() first";
		String html = page.toString();
		WebUtils2.sendHtml(html, getResponse());
	}

	/**
	 * Send a redirect *if* one was specified either via
	 * {@link Fields#REDIRECT_REQUEST2} or by {@link #setRedirect(String)}. A
	 * redirect location of "back" is interpreted as redirect-to-previous-page
	 * using the referer http header.
	 * 
	 * @return true if a redirect was sent, false otherwise
	 * @throws WrappedException of IOException
	 */
	public boolean sendRedirect() {
		if (redirect == null)
			return false;
		if (REDIRECT_REQUEST_BACK.equals(redirect)) {
			redirect = request.getHeader("referer");
			if (redirect==null) {
				Log.w("redirect", "#fail no go-back info for "+this);
				return false;
			}
			// protect against loops TODO fix & delete
			if (redirect.contains(_LOOP_CHECK)) {
				Log.report("Loopy redirect: " + redirect);
				return false;
			}
		} else {
			if (redirect.equals(getRequestUrl())) {
				Log.report("Loopy redirect: " + redirect);
				return false;
			}
		}
		assert ! response.isCommitted();
		
		try {
			// HACK FIXME delete - debug spew
			Log.d("redirect", redirect+" from "+WebRequest.getCurrent()+" at "+ReflectionUtils.getSomeStack(10));
						
			response.sendRedirect(redirect);

			assert !isOpen();
			return true;
		} catch (IOException e) {
			throw Utils.runtime(e);
		}		
	}

	public void setPage(IBuildStrings page) {
		this.page = page;
	}

	/**
	 * Set the desired redirect (does not cause a redirect until
	 * {@link #sendRedirect()} is called). This will automatically have been set
	 * from the request variable {@link Fields#REDIRECT_REQUEST2} if it was
	 * present, but it can be overridden.
	 * 
	 * @param location
	 *            Full url. Can be null. Can be {@link #REDIRECT_REQUEST_BACK}
	 */
	public void setRedirect(String location) {
		redirect = location;
	}

	/**
	 * 
	 * @param key
	 * @param value
	 *            If null, the key will be removed
	 */
	public <X> void setSessionAttribute(Key<X> key, X value) {
		HttpSession session = getSession();
		if (value == null) {
			session.removeAttribute(key.getName());
			return;
		}
		session.setAttribute(key.getName(), value);
	}

	/**
	 * 
	 * @param uxid Cannot be null if user is not null.
	 * @param user Can be null even if uxid is not null.
	 */
	public void setUser(XId uxid, IProperties user) {
		this.uxid = uxid;
		this.user = user;
		assert user==null || uxid != null : user;
		setSessionAttribute(USER, user);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("WebRequest:user=" + user + ":action=" + action + ":req={");
		// obfuscate passwords
		Map<String, Object> pmap = getParameterMap();
		pmap.entrySet().forEach(e -> {			
			Object v = e.getValue();
			if (ConfigBuilder.protectPasswords(e.getKey())) {
				v = "XXX";
			}
			sb.append(e.getKey()+"=");
			// cap big strings, so we can see more keys
			String sv = Printer.str(v);
			sb.append(StrUtils.ellipsize(sv, 50));
			sb.append(", ");
		});	
		if ( ! pmap.isEmpty()) StrUtils.pop(sb, 2);
		sb.append("}-");
		sb.append(getBrowserType());
		return sb.toString();
	}

	/**
	 * Interprets the user-agent header.
	 * @return
	 */
	public BrowserType getBrowserType() {		
		String ua = getUserAgent();
		return new BrowserType(ua);
	}

	/**
	 * e.g. Firefox on Windows, but more cryptic than that. 
	 * @return never null (in the rare case that we don't have this, return "unknown")
	 * @see #getBrowserType()
	 */
	public String getUserAgent() {
		String ua = request.getHeader("User-Agent");
		return ua==null? "unknown" : ua;
	}

	String postBody;
	
	/**
	 * 
	 * @return the post body, if there was one, or null.
	 */
	public String getPostBody() {
		if (postBody!=null) return postBody;
		try {
			String body = FileUtils.read(request.getInputStream());
			if (body!=null && ! body.isEmpty()) {
				postBody = body;
				return postBody;
			}
		} catch(IOException ex) {
			throw Utils.runtime(ex);
		}
		// getParameterMap -- which may already have been called -- will consume 
		// the post body! But we can recover it as the last blank-valued key.
		try {
			Map<String, String[]> map = request.getParameterMap();
			// Take the last blank-valued key!
			for(Map.Entry<String, String[]> me : map.entrySet()) {
				String[] v = me.getValue();
				if (v==null || v.length==0 || (v.length==1 && v[0].isEmpty())) {
					postBody = me.getKey().toString();
				}
			}
			return postBody;
		} catch (Exception e) {
			throw Utils.runtime(e);
		}
	}

	/**
	 * @return true if the do-not-track header "DNT" is on (= "1"). 
	 * I.e. true means don't track!
	 * Plus: also checks for a DNT cookie first, 
	 * as an easy way to implement yes/no-cookies on a per server basis.
	 * And will set the Tk header if appropriate.
	 */
	public boolean isDoNotTrack() {
		// cookie first, so site settings can override
		String dntc = getCookie("DNT"); // allow GL tracking everywhere?
//		if (dntc==null) dntc = getCookie("DNT-"+WebUtils.getDomain(getReferer()));
		if (dntc != null) {
			if ("0".equals(dntc.trim())) {
				// https://www.w3.org/TR/tracking-dnt/#tracking-status-value
				setHeader("Tk", "C");
				return false;
			}
			// site specific DNT
			return true;
		}
		String dnth = getRequest().getHeader("DNT");
		if (dnth==null) return false;
		if ("1".equals(dnth.trim())) {
			// DNT!
			return true;
		}
		// assume OK to track
		return false;
	}

	/**
	 * This is NOT auto-set. It relies on {@link #setUser(XId, IProperties)}.
	 * @return user id, or null
	 */
	public XId getUserId() {
		return uxid;
	}
	
	XId uxid;

	public void addMessage(String text) {
		addMessage(new AjaxMsg(text));
	}

	public static void close(HttpServletRequest req, HttpServletResponse resp) {
		try {
			FileUtils.close(req.getInputStream());		
		} catch(Exception ex) {
			// oh well
		}		
		try {
			FileUtils.close(resp.getOutputStream());		
		} catch(Exception ex) {
			// oh well
		}		
	}

	public boolean isPUT() {
		return is("PUT");
	}
	
	/**
	 * Is this a GET / PUT / POST / DELETE method?
	 * 
	 * HACK allows action=put etc as an alternative to a proper http PUT
	 * @param method
	 * @return
	 */
	private boolean is(String method) {
		return method.equalsIgnoreCase(getRequest().getMethod()) || actionIs(method);
	}

	public boolean isPOST() {
		return is("POST");
	}
	public boolean isGET() {
		return is("GET");
	}
	public boolean isDELETE() {
		return is("DELETE");
	}

    /**
    *
    * Sets a response header with the given name and value.
    * If the header had already been set, the new value overwrites the
    * previous one.  The <code>containsHeader</code> method can be
    * used to test for the presence of a header before setting its
    * value.
    */
	public void setHeader(String header, String value) {
//		?? encoding
		getResponse().setHeader(header, value);
	}
}
