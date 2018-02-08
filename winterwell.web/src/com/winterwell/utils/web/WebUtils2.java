package com.winterwell.utils.web;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.Address;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.StringEscapeUtils;

import com.winterwell.json.JSONArray;
import com.winterwell.utils.Environment;
import com.winterwell.utils.FailureException;
import com.winterwell.utils.IProperties;
import com.winterwell.utils.IReplace;
import com.winterwell.utils.Key;
import com.winterwell.utils.Mutable;
import com.winterwell.utils.StrUtils;
import com.winterwell.utils.TodoException;
import com.winterwell.utils.Utils;
import com.winterwell.utils.WrappedException;
import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.utils.containers.Containers;
import com.winterwell.utils.containers.Pair;
import com.winterwell.utils.io.FileUtils;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.time.Dt;
import com.winterwell.web.FakeBrowser;
import com.winterwell.web.WebEx;
import com.winterwell.web.WebPage;
import com.winterwell.web.ajax.JsonResponse;
import com.winterwell.web.app.BrowserType;
import com.winterwell.web.app.WebRequest;
import com.winterwell.web.email.SimpleMessage;
import com.winterwell.web.fields.AField;
import com.winterwell.web.fields.Checkbox;
import com.winterwell.web.fields.MissingFieldException;
import com.winterwell.web.test.TestHttpServletRequest;

import eu.medsea.util.MimeUtil;
import sun.misc.BASE64Decoder;

/**
 * {@link WebUtils}2 with more dependencies and more Winterwell-specific bits.
 * 
 * @author daniel
 * 
 * @testedby {@link WebUtils2Test}
 */
public class WebUtils2 extends WebUtils {
	

	public static boolean isHtmlDoc(String html) {
		// TOTAL HACK (but fast)
		int fi = html.indexOf("<html");
		if (fi==-1) fi = html.indexOf("<HTML");
		if (fi==-1 || fi>500) return false;
		return true;
	}

	public static void display(WebPage html) {
		display(html.toString());
	}

	/**
	 * convert all emails to lowercase and remove the name part. E.g.
	 * "Bob &lt;Bob@FOO.COM&gt;" becomes "bob@foo.com"
	 * 
	 * @param address
	 * @return
	 */
	public static String canonicalEmail(Address address) {
		InternetAddress addr = (InternetAddress) address;
		return addr.getAddress().toLowerCase();
	}
	
	public static String cleanUp(String url) {
		if (url==null) return null;
		// de-bounce & lengthen urls? No, it's slow and can fail -- call #getLongUrl() if you want that.
		return removeGoogleTrackingParameters(url.trim());
	}
	
	/**
	 * 
	 * @param path
	 * @return url, without any Google UTM parameters
	 */
	static String removeGoogleTrackingParameters(String path) {
//		Campaign Source (utm_source) – Required parameter to identify the source of your traffic such as: search engine, newsletter, or other referral.
//		Campaign Medium (utm_medium) – Required parameter to identify the medium the link was used upon such as: email, CPC, or other method of sharing.
//		Campaign Term (utm_source) – Optional parameter suggested for paid search to identify keywords for your ad. You can skip this for Google AdWords if you have connected your AdWords and Analytics accounts and use the auto-tagging feature instead.
//		Campaign Content (utm_content) – Optional parameter for additional details for A/B testing and content-targeted ads.
//		Campaign Name (utm_campaign) – Required parameter to identify a specific product promotion or strategic campaign such as a spring sale or othe
		Pattern UTM_PARAMETERS = Pattern.compile("([&?])utm_(source|medium|content|campaign|term)=.*?(&|$)");
		while (true) {
			Matcher m = UTM_PARAMETERS.matcher(path);
			int len = path.length();
			path = m.replaceFirst("$1");
			if (path.length() == len) {
				break;
			}
		}
		// also Digital Analytix as used by the BBC
		// see http://www.about-digitalanalytics.com/comscore-digital-analytix-url-campaign-generator
		Pattern NS_PARAMETERS = Pattern.compile("([&?])ns_(source|mchannel|linkname|campaign|fee)=.*?(&|$)");
		while (true) {
			Matcher m = NS_PARAMETERS.matcher(path);
			int len = path.length();
			path = m.replaceFirst("$1");
			if (path.length() == len) {
				break;
			}
		}
		// clean up the url end
		if (path.endsWith("&")) {
			path = path.substring(0, path.length() - 1);
		}
		if (path.endsWith("?")) {
			path = path.substring(0, path.length() - 1);
		}
		return path;
	}
	
	public static String extractParameterFromUrl(String parameter, String url){
		Pattern p = Pattern.compile("(?<=" + parameter + "=).*?(?=&|$)");
		Matcher m = p.matcher(url);	
		if (m.find()){
			return m.group();
		}
		return null;
	}
	
	private static final Pattern AMP_HASH_CHAR = Pattern
			.compile("&amp;#(\\d+);");

	/**
	 * pattern matcher for an email address
	 * TODO replace with {@link SimpleMessage#EMAIL_REGEX}
	 * (but needs some care as they are slightly different at the moment)
	 */
	private static final Pattern pEmail = Pattern
			.compile("[a-zA-Z0-9_\\-\\+\\'][a-zA-Z0-9_\\-\\.\\+\\']*@[a-zA-Z0-9_\\-]+\\.[a-zA-Z0-9_\\-\\.]+");

	/**
	 * Match an ❬a❭ opening tag. Allows some invalid patterns, as spammers use these to get round exactly
	 * this kind of check.
	 */
	private static final Pattern pExternalLink = Pattern.compile(
			"<a[^>]+href=['\"]?(\\s*\\w+://[^'\" \t\r\n>]+)[^>]*>",
			Pattern.DOTALL);

	/**
	 * Convenience for creating cookies. Uses the "/" path
	 * 
	 * @param response
	 * @param name This will be url-encoded. 
	 * NB: The actual spec for cookie names is vague (c.f. http://stackoverflow.com/questions/1969232/allowed-characters-in-cookies#1969339)
	 * @param value
	 *            This will be converted using toString()
	 * @param timeTolive
	 * @param cookieDomain
	 *            e.g. ".soda.sh" Can be null for default behaviour
	 */
	public static void addCookie(HttpServletResponse response, String name,
			Object value, Dt timeTolive, String cookieDomain) {
		String cname = urlEncode(name);
		Cookie cookie = new Cookie(cname, value.toString());
		cookie.setMaxAge((int) (timeTolive.getMillisecs() / 1000));
		if (cookieDomain != null) {
			cookie.setDomain(cookieDomain);
		}
		// make the cookie available across the server
		cookie.setPath("/");
		// FIXME WHy is Jetty sometimes wrapping names in quotes? It seems to happen if the name uses a % encoding.		
		response.addCookie(cookie);
	}

	/**
	 * Convenience method: Add &lt;a href='uri'>text&lt;/a> to the page.
	 * 
	 * @param page
	 * @param uri
	 * @param text
	 */
	public static void appendLink(Appendable page, String uri, String text) {
		try {
			page.append("<a href='");
			page.append(uri);
			page.append("'>");
			page.append(text);
			page.append("</a>");
		} catch (IOException e) {
			throw new WrappedException(e);
		}
	}

	/**
	 * Akin to {@link FileUtils#changeType(File, String)}. Change the .html of a
	 * url.
	 * 
	 * @param url
	 * @param string
	 *            e.g. "html" or "txt"
	 * @return modified url
	 */
	public static StringBuilder changeType(HttpServletRequest request,
			String type) {
		StringBuilder link = new StringBuilder(request.getRequestURL());
		String qs = request.getQueryString();
		return changeType2(link, qs, type);
	}

	/**
	 * 
	 * @param url
	 * @param type E.g. "txt" (note: no leading dot)
	 *            Can be null for no-type. 
	 * @return
	 */
	public static String changeType(String url, String type) {
		StringBuilder link = new StringBuilder(url);
		int qm = url.indexOf("?");
		if (qm == -1) {
			qm = url.length();
		}
		int lastSlash = url.lastIndexOf("/", qm);
		int lastDot = url.lastIndexOf(".", qm);
		if (lastDot > lastSlash) {
			// chop out the current .type
			link.delete(lastDot, qm);
			qm = lastDot;
		}
		if (type == null)
			return link.toString();
		link.insert(qm, ".");
		link.insert(qm + 1, type);
		return link.toString();
	}

	static StringBuilder changeType2(StringBuilder link, String qs, String type) {
		int lastSlash = link.lastIndexOf("/");
		int lastDot = link.lastIndexOf(".");
		if (lastDot > lastSlash) {
			link.delete(lastDot, link.length());
		}
		link.append(".");
		link.append(type);
		if (qs == null)
			return link;
		link.append('?');
		link.append(qs);
		return link;
	}

	public static void checkForMissingFields(HttpServletRequest request,
			AField... fields) throws MissingFieldException {
		ArrayList<AField> missing = new ArrayList<AField>();
		for (AField field : fields) {
			if (Utils.isBlank(request.getParameter(field.getName()))) {
				missing.add(field);
			}
		}
		if (missing.size() != 0)
			throw new MissingFieldException(missing);
	}

	public static void checkForMissingFields(IProperties state,
			AField... fields) throws MissingFieldException {
		ArrayList<AField> missing = new ArrayList<AField>();
		for (AField field : fields) {
			if (state.get(field) == null) {
				missing.add(field);
			}
		}
		if (missing.size() != 0)
			throw new MissingFieldException(missing);
	}

	/**
	 * Suppose you don't trust your users to post links. There is the issue of
	 * spam. This method detects whether they're posting to external websites.
	 * Only picks up &lt;a&gt; links (i.e. ignores javascript based links).
	 * 
	 * @param txt
	 * @param allowedDomains
	 *            Matches against the whole url less protocol, so can include
	 *            path sub-dirs. E.g. www.winterwell.com/public
	 * @return
	 * @see #removeExternalLinks(String, String...)
	 */
	public static boolean containsExternalLinks(String txt,
			String... allowedDomains) {
		// remove protocol from allowedDomains
		for (int i = 0; i < allowedDomains.length; i++) {
			allowedDomains[i] = removeProtocolFromUrl(allowedDomains[i]);
		}
		// Match a tags with a protocol spec
		Matcher m = pExternalLink.matcher(txt);
		replaceLoopLabel: while (m.find()) {
			String link = m.group(1);
			// remove protocol from link
			link = removeProtocolFromUrl(link);
			// ignore good links
			for (String domain : allowedDomains) {
				if (link.startsWith(domain)) {
					continue replaceLoopLabel;
				}
			}
			//
			return true;
		}
		return false;
	}

	/**
	 * Convenience for using {@link FakeBrowser} to fetch one page
	 * 
	 * @param uri
	 * @return the web page at uri
	 */
	public static String fetchWebPage(String uri) {
		return new FakeBrowser().getPage(uri);
	}

	public static <X> X getAttribute(HttpSession session, Key<X> key) {
		Object x = session.getAttribute(key.getName());
		return (X) x;
	}

	/**
	 * 
	 * @param request
	 * @return name, password or null if it doesn;t work TODO test
	 */
	public static Pair<String> getBasicAuthentication(HttpServletRequest request) {
		String auth = request.getHeader("Authorization");
		if (auth == null || auth.length() < 7)
			return null;
		String basic = auth.substring(0, 6).toLowerCase();
		if (!basic.equals("basic "))
			return null;
		BASE64Decoder decoder = new sun.misc.BASE64Decoder();
		byte[] decoded;
		try {
			decoded = decoder.decodeBuffer(auth.substring(6));
		} catch (IOException e) {
			throw new WrappedException(e);
		}
		String ds = new String(decoded);
		int i = ds.indexOf(':');
		if (i == -1)
			return null;
		return new Pair<String>(ds.substring(0, i), ds.substring(i + 1));
	}

	/**
	 * Get a cookie sent as part of a request
	 * 
	 * @param req
	 * @param name
	 * @return cookie value for the given name, or null
	 */
	public static String getCookie(HttpServletRequest req, String name) {
		Cookie[] cookies = req.getCookies();
		if (cookies == null)
			return null;
		for (Cookie cookie : cookies) {
			if (cookie.getName().equals(name))
				return cookie.getValue();
		}
		return null;
	}

	/**
	 * Convenience for using {@link HttpServletRequest#getCookies()}
	 * 
	 * @param req
	 * @return map of cookies
	 */
	public static Map<String, String> getCookies(HttpServletRequest req) {
		Cookie[] cookies = req.getCookies();
		if (cookies == null)
			return Collections.emptyMap();
		Map<String, String> map = new HashMap();
		for (Cookie cookie : cookies) {
			String cname = urlDecode(cookie.getName());
			String cvalue = cookie.getValue();
			map.put(cname, cvalue);
		}
		return map;
	}

	/**
	 * Get the IP from which web requests from this machine come. NB The IP
	 * returned may be the IP of a NAT gateway or proxy and not this machine.
	 * 
	 * @deprecated Any use of this method is likely to be incorrect
	 * @return
	 */
	@Deprecated
	public static String getIPAddress() {
		return new FakeBrowser()
				.getPage("http://www.whatismyip.com/automation/n09230945.asp");
	}

	public static String getMimeType(File file) {
		return getMimeType(file.getPath());
	}

	
	/**
	 * @param fileName
	 * @return can (rarely) return null
	 */
	public static String getMimeType(String fileName) {
		try {
			String type = FileUtils.getType(fileName);
			String mimetype = (String) new ArrayMap(
					// Workaround for bug in MimeUtil re css
					"css", "text/css",
					// csv is text/csv c.f. http://tools.ietf.org/html/rfc4180
					"csv", "text/csv",
					// video? (NB: copy-pasta from JTwitter Twitter.java) 
					"mov", "video/quicktime",
					"avi",	"video/x-msvideo",
					"wmv",	"video/x-ms-wmv",
					"m4v",	"video/mp4",
					"mp4",	"video/mp4"
					).get(type);
			if (mimetype != null) return mimetype;
			// Ugly library, but hopefully effective
			return MimeUtil.getMimeType(fileName);
		} catch(Throwable ex) { // get mimetype can fail on a missing jar
			Log.e("FileServlet", ex);
			return null;
		}
	}

	/**
	 * Given an html or xml page, make a reasonable attempt to extract the plain
	 * text (but without parsing the page into a DOM, or handling any styling or
	 * javascript).
	 * <p>
	 * This will not try to sort the wheat from the chaff.
	 * It will compact whitespace, but not paragraph/double-line breaks.
	 * 
	 * This differs from {@link #stripTags(String)}, in that it will try to convert &lt;p&gt; into line-break
	 * 
	 * @param html
	 * @return plain text (trimmed)
	 * @see Creole's PageJuicer which does article contents extraction
	 */
	public static String getPlainText(String html) {		
		// convert paragraphs and line breaks
		html = PTAG.matcher(html).replaceAll("\n\n");
		html = BRTAG.matcher(html).replaceAll("\n");
		String plain = WebUtils.stripTags(html).trim();
		plain = htmlDecode(plain);
		plain = StrUtils.toCleanLinux(plain);
		// compact some whitespace (but keep paragraphs & line breaks)
		plain = plain.replaceAll("  +", " ");
		plain = plain.replaceAll("\n\n\n+", "\n\n");
		return plain.trim();
	}
	
	private static final Pattern PTAG = Pattern.compile("<(p|P)[^<>]*>");
	private static final Pattern BRTAG = Pattern.compile("<(br|BR)/?>");
	

	static final Pattern ENTITY_REGEX = Pattern.compile("&[#a-zA-Z0-9]+;");
	
	/**
	 * Unescape entities. E.g. "&amp;" becomes just "&"
	 * @testedby {@link WebUtils2Test#testHtmlDecode()}
	 */
	public static String htmlDecode(final String plain) {
		// Apache turned out to be a bottleneck!
		// String html = StringEscapeUtils.unescapeHtml4(v);		
		// So let's do it ourselves
		String p2 = StrUtils.replace(plain, ENTITY_REGEX, new HtmlDecoder(plain));
		return p2;
	}
	
	private static final int MAX_REDIRECTS = 5;

	private static final String ALLOW_CREDENTIALS_HEADER = "Access-Control-Allow-Credentials";


	/**
	 * @param url A full url, or just the query string
	 * @param param
	 * @return the value of parameter, or null if unset/blank.
	 * 
	 * ??How to send "please delete this setting" vs null??
	 */
	public static String getQueryParameter(String url, String param) {
		assert WebUtils.urlEncode(param).equals(param) : param;
		Pattern keyVal = Pattern.compile("([&?]|^)" + param + "=(.*?)(&|$)");
		Matcher m = keyVal.matcher(url);
		boolean found = m.find();
		if (!found)
			return null;
		String val = m.group(2);
		// convert "" to null for simplicity
		if (val.isEmpty()) return null;
		// Hack: Should we convert "null" and "undefined" to null? Yes, that's probably best.
		if (val.equals("undefined") || val.equals("null")) {
			return null;
		}
		val = WebUtils.urlDecode(val);
		return val;
	}

	/**
	 * 
	 * @param url
	 * @return
	 * @testedby {@link CGIUtilsTest#testGetQueryParameters()}
	 */
	public static Map<String, String> getQueryParameters(String url) {
		int i = url.indexOf('?');
		if (i == -1 || i == url.length() - 1)
			return Collections.EMPTY_MAP;
		Map<String, String> map = new ArrayMap();
		i = i + 1;
		while (i < url.length() - 1) {
			int ki = url.indexOf('=', i);
			if (ki == -1 || ki == url.length()) {
				break;
			}
			int vi = url.indexOf('&', ki);
			if (vi == -1) {
				vi = url.length();
			}
			if (vi == ki + 1) {
				i = vi + 1;
				continue;
			}
			String key = url.substring(i, ki);
			String val = url.substring(ki + 1, vi);
			map.put(key, WebUtils.urlDecode(val));
			i = vi + 1;
		}
		return map;
	}

	/**
	 * The url which referred our visitor to us, or null if unset.
	 * 
	 * @param request
	 * @return referring url
	 */
	public static String getReferrer(HttpServletRequest request) {
		return request.getHeader("Referer");
	}

	/**
	 * Get the full URL which the user entered (inc. protocol, domain, port,
	 * path & query).
	 * 
	 * @param request
	 * @see HttpServletRequest#getRequestURL() which is similar but doesn't
	 *      include the query string.
	 */
	public static String getRequestURL(HttpServletRequest request) {
		StringBuffer link = request.getRequestURL();
		String qs = request.getQueryString();
		if (qs == null)
			return link.toString();
		link.append('?');
		link.append(qs);
		return link.toString();
	}

	public static File getWebAppBaseDir(ServletConfig config) {
		ServletContext ctxt = config.getServletContext();
		String realPath = ctxt.getRealPath(""); // eg
												// /home/daniel/winterwell/code/creole/web
		File webDir = new File(realPath);
		if ( ! new File(webDir, "WEB-INF").exists() 
				&& ! new File(webDir, "static").exists()
				&& ! new File(webDir, "index.html").exists()) 
		{
			Log.w("web", webDir.getAbsolutePath()+" might not be valid: no WEB-INF or static sub-directories or index.html");
		}
		return webDir;
	}

	/**
	 * First try the personal section of an email, then fall back to
	 * {@link #guessNameFromEmail(String)}.
	 * 
	 * @param email
	 * @return e.g. "J.Smith" given "J.Smith <whatever@somewhere.com>" or
	 *         "John Smith" given "john.smith@whatever.com"
	 */
	public static String guessNameFromEmail(InternetAddress email) {
		String name = email.getPersonal();
		if (!Utils.isBlank(name))
			return name;
		return guessNameFromEmail(email.getAddress());
	}

	/**
	 * Extract a plausible name from an email address.
	 * 
	 * @return e.g. "John Smith" given "john.smith@whatever.com"
	 * @see #guessNameFromEmail(InternetAddress) which is a bit better
	 */
	public static String guessNameFromEmail(String email) {
		int i = email.indexOf('@');
		if (i == -1)
			return email;
		String namePart = email.substring(0, i);
		String[] bits = namePart.split("[_\\.\\+]");
		StringBuilder name = new StringBuilder();
		for (String bit : bits) {
			if (bit.length() == 0) {
				continue;
			}
			name.append(Character.toUpperCase(bit.charAt(0)));
			if (bit.length() > 1) {
				name.append(bit.substring(1));
			}
			name.append(' ');
		}
		if (name.length() > 0) {
			StrUtils.pop(name, 1);
		}
		return name.toString();
	}


	/**
	 * Convenience wrapper for {@link StringEscapeUtils#escapeHtml(String)}.
	 * Converts e.g. "bread & butter" to &quot;bread &amp; butter&quot; Note
	 * that any html tags in the string will be encoded for display as text!
	 * <p>
	 * This is a little over-zealous: it converts " and ' which browsers will
	 * handle fine. This does mean the returned Strings can be placed inside
	 * javascript calls, but it's almost certainly better to use
	 * {@link WebUtils#attributeEncode(String)} for that.
	 * 
	 * @param v
	 *            String to encode. Can be null (returns null)
	 * @see #htmlDecode(String)
	 * @testedby {@link CGIUtilsTest#testHtmlEncode()}
	 */
	public static String htmlEncode(String v) {
		if (v == null)
			return v;
		// Use Apache - but they ignore ' &apos; is correct for xml but not
		// html. So we use &#39;
		String html = StringEscapeUtils.escapeHtml4(v).replace("'", "&#39;");
		// also apache get &#; wrong - they encode the & to &amp;
		return AMP_HASH_CHAR.matcher(html).replaceAll("&#$1;");
	}

	/**
	 * Like {@link #htmlEncode(String)}, but it leaves urls as they are.
	 * 
	 * @param v
	 * @return
	 */
	public static String htmlEncodeWithUrlProtection(final String v) {
		// protect urls
		Matcher m = WebUtils.URL_REGEX.matcher(v);
		StringBuilder sb = new StringBuilder(v.length() + 16);
		int pos = 0;
		while (m.find()) {
			String bit = v.substring(pos, m.start());
			String encBit = htmlEncode(bit);
			sb.append(encBit);
			sb.append(m.group());
			pos = m.end();
		}
		// and the last bit
		if (pos != v.length()) {
			String bit = v.substring(pos);
			sb.append(htmlEncode(bit));
		}
		return sb.toString();
	}

	/**
	 * Add an anchor to a url
	 * 
	 * @param url
	 * @param anchor
	 *            This will be url encoded (i.e. the input should not be url
	 *            encoded)
	 * @return url#anchor
	 */
	public static String insertAnchor(String url, String anchor) {
		assert url.indexOf('#') == -1 : url;
		// this is correct even if the url contains a ?query portion
		return url + "#" + WebUtils.urlEncode(anchor);
	}

	/**
	 * @param request
	 * @return true if this is an HTTPS request (ie secure)
	 */
	public static boolean isHTTPS(HttpServletRequest request) {
		String protocol = request.getProtocol().toLowerCase();
		return protocol.startsWith("https");
	}

	
	/**
	 * Call the W3C validator to test this web page for compliance.
	 * 
	 * @param uri
	 * @return
	 */
	public static boolean isW3CValid(URI uri) {
		FakeBrowser browser = new FakeBrowser();
		String w3cUri = "http://validator.w3.org/check?uri="
				+ WebUtils.urlEncode(uri);
		String page = browser.getPage(w3cUri);
		if (page.contains("[Invalid]"))
			return false;
		if (page.contains("[Valid]"))
			return true;
		throw new TodoException();
	}
	

	/**
	 * 
	 * @param email
	 * @return true if this is a valid email address. Accepts addresses with or
	 *         without names, e.g. "Bob &lt;bob@bobserver.com&gt;" is OK
	 * @testedby {@link CGIUtilsTest#testIsValidEmail()}
	 */
	public static boolean isValidEmail(String email) {
		if (email == null)
			return false;
		try {
			InternetAddress ia = new InternetAddress(email, true);
			// InternetAddress allows foo@bar which we suspect is not actually
			// going to work
			String addr = ia.getAddress();
			return pEmail.matcher(addr).matches();
		} catch (AddressException e) {
			return false;
		}
		// return pEmail.matcher(email).find();
	}

	/**
	 * 
	 * @param txt
	 * @param allowedDomains
	 * @return txt where all &lt;a&gt; links to external sites have
	 *         rel='nofollow' set
	 */
	public static String nofollowExternalLinks(String txt, final String... allowedDomains) {
		// remove protocol from allowedDomains
		for (int i = 0; i < allowedDomains.length; i++) {
			allowedDomains[i] = removeProtocolFromUrl(allowedDomains[i]);
		}
		// Match a tags with a protocol spec
		String txt2 = StrUtils.replace(txt, pExternalLink, new IReplace() {			
			@Override
			public void appendReplacementTo(StringBuilder sb, Matcher match) {
				String link = match.group(1);
				// remove protocol from link
				link = removeProtocolFromUrl(link);
				// ignore good links
				for (String domain : allowedDomains) {
					if (link.startsWith(domain)) {
						sb.append(match.group());
						return;
					}
				}
				// nofollow it
				sb.append("<a rel='nofollow'");
				sb.append(match.group().substring(2));
			}
		});
		return txt2;
	}

	/**
	 * Extract the key/value pairs from a url-encoded string (e.g. a GET url)
	 * 
	 * @param response
	 * @param lowercaseKeys
	 *            If true, all keys will be lower-cased
	 * @return
	 */
	public static Map<String, String> parseUrlEncoded(String response,
			boolean lowercaseKeys) {
		int qi = response.indexOf('?');
		if (qi != -1) {
			if (qi == response.length() - 1)
				return Collections.emptyMap();
			response = response.substring(qi + 1);
		}
		String[] bits = response.split("&");
		Map<String, String> map = new HashMap<String, String>(bits.length);
		for (String bit : bits) {
			String[] kv = bit.split("=");
			if (kv.length == 0) {
				continue;
			}
			String k = kv[0];
			k = WebUtils.urlDecode(k);
			if (lowercaseKeys) {
				k = k.toLowerCase();
			}
			String v = kv.length == 1 ? null : WebUtils.urlDecode(kv[1]);
			map.put(k, v);
		}
		return map;
	}

	/**
	 * Set a "delete cookie" command in the response (which must be sent before
	 * anything will happen). Note: there is no way to find out the domain &
	 * path of a cookie - the browser won't tell us, but if we get it wrong, the
	 * delete will fail.
	 * 
	 * @param response
	 * @param cookieName
	 */
	public static void removeCookie(String cookieName,
			HttpServletResponse response, String domain, String path) {
		Cookie c1 = new Cookie(cookieName, "");
		if (domain != null) {
			c1.setDomain(domain);
		}
		if (path != null) {
			c1.setPath(path);
		}
		c1.setMaxAge(0);
		response.addCookie(c1);
	}

	/**
	 * Suppose you don't trust your users to post links. There is the issue of
	 * spam. One simple solution is to prune out any external links.
	 * 
	 * @param txt
	 * @param allowedDomains
	 *            E.g. "www.winterwell.com" Protocol (e.g. http, ftp) is not
	 *            needed and will be ignored.
	 * @return txt with external links replaced by non-functional links with an
	 *         anti-spam explanation saying "link removed to block spam"
	 * @see #containsExternalLinks(String, String...)
	 */
	public static String removeExternalLinks(String txt,
			String... allowedDomains) {
		// remove protocol from allowedDomains
		for (int i = 0; i < allowedDomains.length; i++) {
			allowedDomains[i] = removeProtocolFromUrl(allowedDomains[i]);
		}
		// Match a tags with a protocol spec
		Matcher m = pExternalLink.matcher(txt);
		StringBuilder sb = new StringBuilder(txt.length());
		int e = 0;
		replaceLoopLabel: while (m.find()) {
			String link = m.group(1);
			// remove protocol from link
			link = removeProtocolFromUrl(link);
			// ignore good links
			for (String domain : allowedDomains) {
				if (link.startsWith(domain)) {
					continue replaceLoopLabel;
				}
			}
			// chop it
			sb.append(txt.substring(e, m.start()));
			sb.append("<a title='link removed to block spam'>");
			e = m.end();
		}
		sb.append(txt.substring(e));
		return sb.toString();
	}

	/**
	 * Remove the "http://" bit of a url, if present
	 */
	private static String removeProtocolFromUrl(String url) {
		int pi = url.indexOf("://");
		if (pi != -1)
			return url.substring(pi + 3);
		return url;
	}

	/**
	 * TODO kill all java script elements in an html block. This could damage
	 * some normal text in the process!
	 * 
	 * @param txt
	 * @return
	 */
	public static String removeScript(String html) {
		Pattern scriptTag = Pattern
				.compile("<script", Pattern.CASE_INSENSITIVE);
		html = scriptTag.matcher(html).replaceAll("&lt;script");
		// this onEvent replacement is overly aggressive
		Pattern handlerTag = Pattern.compile("on(\\w+)=",
				Pattern.CASE_INSENSITIVE);
		html = handlerTag.matcher(html).replaceAll("on $1 = ");
		return html;
	}

	/**
	 * Send an html page as the response to an http request. Closes the response
	 * object.
	 * 
	 * @param html
	 * @param resp
	 */
	public static void sendHtml(String html, HttpServletResponse resp) {
		try {
			assert html != null && resp != null;
			resp.setContentType("text/html; charset=UTF-8");
			BufferedWriter out = FileUtils.getWriter(resp.getOutputStream());
			out.write(html);
			FileUtils.close(out);
		} catch (IOException e) {
			throw new WrappedException(e);
		}
	}

	/**
	 * @deprecated Use {@link #sendJson(JsonResponse, WebRequest)} instead, so it can sniff the browser :(
	 * 
	 * Sends a Json map. Then closes the output stream.
	 * <p>
	 * ?? move this into {@link JsonResponse}??
	 * 
	 * @param output
	 * @param response
	 * @throws IOException
	 * @testedby {@link CGIUtilsTest#testSendJson}
	 */
	public static void sendJson(JsonResponse output, HttpServletResponse response) throws IOException {
		// HACK
		WebRequest req = Environment.get().get(WebRequest.KEY_REQUEST_STATE);
		if (req==null) {
			req = new WebRequest(null, new TestHttpServletRequest(), response);
		} else {
			req= new WebRequest(req.getServlet(), req.getRequest(), response);
		}
		
		sendJson(output, req);
	}
	
	/**
	 * Sends a Json map. Then closes the output stream.
	 * <p>
	 * ?? move this into {@link JsonResponse}??
	 * 
	 * @param output
	 * @param response
	 * @throws IOException
	 * @testedby {@link CGIUtilsTest#testSendJson}
	 */
	public static void sendJson(JsonResponse output, WebRequest req) throws IOException {
		HttpServletResponse response = req.getResponse();
		// convert to JSON
		String json = output.toJSON();
		BufferedWriter out = null;
		try {
			// set mime and character encoding
			// HACK: Is it an old IE?
			BrowserType bt = req.getBrowserType();
			if (bt.getBrowserMake()==BrowserType.MAKE_IE && bt.getVersion() < 10) {
				response.setContentType(WebUtils.MIME_TYPE_TXT_UTF8);
			} else {
				response.setContentType(WebUtils.MIME_TYPE_JSON + "; charset=UTF-8");
			}
			// TODO use response.getWriter() instead? Does this affect encoding
			// issues at all?
			// PrintWriter pw = response.getWriter();
			out = FileUtils.getWriter(response.getOutputStream());
			if (output.callback != null) {
				out.append(output.callback + "(");
				out.append(json);
				out.append(");");
			} else {
				out.append(json);
			}
		} finally {
			FileUtils.close(out);
		}
	}

	/**
	 * Send a text return (closes the response).
	 * 
	 * @param output
	 *            The text to send
	 * @param response
	 * @throws IOException
	 *             (wrapped)
	 */
	public static final void sendText(String output,
			HttpServletResponse response) {
		BufferedWriter out = null;
		try {
			response.setContentType(WebUtils.MIME_TYPE_TXT_UTF8);
			// TODO use response.getWriter() instead? Does this affect encoding
			// issues at all?
			// PrintWriter pw = response.getWriter();
			out = FileUtils.getWriter(response.getOutputStream());
			out.append(output.toString());
		} catch (IOException e) {
			throw Utils.runtime(e);
		} finally {
			FileUtils.close(out);
		}
	}

	/**
	 * Send a text return (closes the response).
	 * Swallows all exceptions.
	 * 
	 * @param output
	 *            The text to send
	 * @param response
	 */
	public static final void sendError(int code, String output,
			HttpServletResponse response) {
		try {
			// Does this work??
			if (output!=null) {
				response.setContentType(WebUtils.MIME_TYPE_TXT_UTF8);
				// TODO use response.getWriter() instead? Does this affect encoding
				// issues at all?
				// PrintWriter pw = response.getWriter();
				BufferedWriter out = FileUtils.getWriter(response.getOutputStream());
				out.append(output.toString());
				response.setStatus(code);
				FileUtils.close(out); // NB: this close does not seem to be needed
				return;
			}
			response.sendError(code, output);
		} catch (Throwable e) {
			Log.e("web", e);
		}
	}

	public static <X> void setAttribute(HttpSession session, Key<X> key, X value) {
		session.setAttribute(key.getName(), value);
	}

	/**
	 * Convenience wrapper for creating InternetAddress objects
	 * without a try-catch.
	 * @param email
	 * @return email
	 * @throws wrapped
	 *             AddressException if the email is invalid
	 */
	public static InternetAddress internetAddress(String email) {
		try {
			InternetAddress e = new InternetAddress(email);
			// the constructor allows "bob" as valid? Not on our watch!
			if ( ! e.getAddress().contains("@"))
				throw new AddressException(email);
			return e;
		} catch (AddressException e) {
			throw Utils.runtime(e);
		}
	}

	/**
	 * @param _url Can be absolute or relative
	 * @return url with the protocol and host removed. So just the path & query parameters 
	 * E.g. from "http://sodash.com/stream" to "/stream".
	 * If there is no path & query, returns "/"
	 * @see #resolveUri(String, String)
	 */
	public static String relativeUrl(CharSequence _url) {
		String url = _url.toString();
		int i = url.indexOf("://", 0);
		if (i==-1) return url;
		int i2 = url.indexOf("/", i+3);
		if (i2==-1) return "/";
		return url.substring(i2);
	}

	
	/**
	 * Unravel long urls, e.g. from bitly to original
	 * @param url
	 * @param fastStopUrlLength e.g. 28 If a url is this long, don't unravel further. -1 for any length
	 * @return
	 * @throws IOException
	 */
	public static String getLongUrl(String url, int fastStopUrlLength) throws IOException, FailureException {
		return getLongUrl(url, fastStopUrlLength, null);
	}
	
	/**
	 * 
	 * @param startUrl
	 * @param fastStopUrlLength e.g. 28 If a url is this long, don't unravel further. -1 for any length
	 * @param error If not null, this will report if the chain ends with an error.
	 * @return url (as far down the chain as we got)
	 */
	public static String getLongUrl(final String startUrl, int fastStopUrlLength, Mutable.Ref<Exception> error) {
		String url = startUrl;
		String prev = startUrl;
		for(int i=0; i<MAX_REDIRECTS; i++) {
			if (fastStopUrlLength>0 && url.length() > fastStopUrlLength
					// HACK: youtube short links can be quite long
					&& ! url.startsWith("http://youtu.be")
					&& ! url.startsWith("https://youtu.be")) {
				return url;
			}
			try {
				HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
				connection.setFollowRedirects(false);
				connection.setDoOutput(false);
				connection.connect();
				int code = connection.getResponseCode();
				connection.disconnect();
				// A page! Then done
				if (code < 300) {
					URL u = connection.getURL();
					return u.toString();
				}
				// error
				if (code >= 400) {
					if (error!=null) error.value = WebEx.fromErrorCode(code, url);
					URL u = connection.getURL();
					return u.toString();
				}
				// It's a redirect -- get the location
				String locn = connection.getHeaderField("Location");
				if (locn==null || locn.isEmpty()) {
					if (error!=null) error.value = new FailureException("No location for redirect of "+url);
					return url;
				}
				// Follow the trail...
				prev = url;
				url = locn;				
				// ??Is this needed?? Remove jsessionid bollocks which creeps in (why?!)
	//			url = url.replaceFirst(";jsessionid=\\w+", "");
			} catch(MalformedURLException ex) {
				if (error!=null) error.value = ex;
				return prev;
			} catch(Exception ex) {
				if (error!=null) error.value = ex;
				// did we move forward?
				return url;				
			}
		}
		// Give up (same behaviour as FakeBrowser)
		if (error!=null) error.value = new WebEx.E50X(500, "Too many redirects "+url);
		return url;
	}

	/**
	 * Unravel long urls, e.g. from bitly to original. Convenience for {@link #getLongUrl(String, int)}
	 * with assumed-resolved length=40
	 * @param url
	 * @return resolved url
	 * @throws IOException
	 */
	public static String getLongUrl(String url) throws IOException {
		return getLongUrl(url, 40);
	}

	/**
	 * Cross Origin Resource Sharing
	 * See http://www.html5rocks.com/en/tutorials/cors/
	 * @param state
	 * @param forceSet Do we need this?? Why not always set CORS, using Origin if we can??
	 * If true, then an Access-Control-Allow-Origin header will always be set (unless it is already set), 
	 * even if we have no incoming Origin header ("*" will be used as a fallback).
	 * Due to security restrictions this does NOT work for withCredentials (ie with cookies) requests!
	 */
	public static void CORS(WebRequest state, boolean forceSet) {
		if (state.getRequest()==null) {
//			Log.d("CORS", "No request? "+state);
			return;
		}
		{ // debug weird stuff
			Collection<String> responseheaders = state.getResponse().getHeaderNames();
			Collection<String> ach = state.getResponse().getHeaders(ALLOW_CREDENTIALS_HEADER);
			assert ach==null || ach.size() < 2 : ach+" all-response-headers:"+responseheaders+" "+state;

			List<String> headers = Containers.getList(state.getRequest().getHeaderNames());
//			System.out.println(headers);
			Cookie[] cookies = state.getRequest().getCookies();		
			String ref = state.getReferer();
			String caller = state.get("caller");
//			Printer.out(ref);
//			Printer.out(caller);
		}
		
		// Note: wildcard '*' cannot be used in the 'Access-Control-Allow-Origin' header 
		// when the credentials flag is true (ie with cookies).
		// We rely on the caller to explicitly tell us this (see hooru.js). By default ajax does not!
		Boolean wc = state.get(new Checkbox("withCredentials"));
		String origin = state.getRequest().getHeader("Origin");
		String originOut = origin; 
		if (origin==null || origin.equals("null")) {
			if ( ! wc) originOut= "*"; //URI(state.getRequestUrl()).getHost();
		}
		if (forceSet && Utils.isBlank(state.getResponse().getHeader("Access-Control-Allow-Origin"))) {
			if ( ! wc) originOut = "*"; // Do we need this??
		}
		// see http://stackoverflow.com/questions/19743396/cors-cannot-use-wildcard-in-access-control-allow-origin-when-credentials-flag-i		
		if ( ! "*".equals(originOut)) {
//			// Bug seen in good-loop -- It's a mystery! This logging did not shed any light :(
//			if (state.getResponse().getHeader("Access-Control-Allow-Credentials") != null) {
//				Log.escalate(new WeirdException(
//						"2x?! Access-Control-Allow-Credentials: "
//						+state.getResponse().getHeader("Access-Control-Allow-Credentials")
//						+" header-names:"+
//						state.getResponse().getHeaderNames()
//						));
//			}
//			Log.d("cors", "set Access-Control-Allow-Credentials: true from "+ReflectionUtils.getSomeStack(8));			
			state.getResponse().setHeader(ALLOW_CREDENTIALS_HEADER, "true");
		}
		state.getResponse().setHeader("Access-Control-Allow-Origin", originOut);
		// debug - no light :(
//		Collection<String> responseheaders2 = state.getResponse().getHeaderNames();
//		Collection<String> ach = state.getResponse().getHeaders(ALLOW_CREDENTIALS_HEADER);
//		assert ach==null || ach.size() < 2 : ach+" all-response-headers:"+responseheaders2+" "+state;
	}

	/**
	 * @param url
	 * @return Chop off the ?parameters part of the url, if present
	 */
	public static String removeQuery(String url) {
		int i = url.indexOf('?');
		if (i==-1) return url;
		return url.substring(0, i);
	}

	/**
	 * Handle variations on the array/List theme, as created by different
	 * json serialisers.
	 * @param _jtags Can be null (returns null)
	 * @return List view
	 */
	public static List asList(Object _jtags) {
		if (_jtags==null) return null;
		if (_jtags instanceof List) {
			return (List) _jtags;
		}
		if (_jtags instanceof JSONArray) {
			return ((JSONArray) _jtags).getList();
		}
		return Containers.asList(_jtags);		
	}

	/**
	 * convert all emails to lowercase and remove the name part. E.g.
	 * "Bob &lt;Bob@FOO.COM&gt;" becomes "bob@foo.com"
	 * 
	 * @param address
	 * @return
	 * @throws AddressException
	 *             (wrapped) if the address is not a valid format
	 */
	public static String canonicalEmail(String address) {
		return WebUtils2.canonicalEmail(WebUtils2.internetAddress(address.trim()));
	}

	/**
	 * Wrap an exception as a WebEx exception.
	 * This does not re-throw or log the error -- just wraps it.
	 * It's a no-op if ex already is a WebEx 
	 * @param ex
	 * @return ex (wrapped if need be)
	 */
	public static WebEx runtime(Throwable ex) {
		if (ex instanceof WebEx) {
			return (WebEx) ex;
		}
		if (ex instanceof IOException) {
			// probably a 404
			return new WebEx.E40X(400, ex.getMessage(), ex); // wrap the stacktrace??
		}
		// send it via Utils which may improve some errors, eg SQL 
		RuntimeException wrap = Utils.runtime(ex);
		if (wrap instanceof WrappedException) {
			return new WebEx.E50X(wrap.getCause());	
		}
		return new WebEx.E50X(wrap);
	}

}


class HtmlDecoder implements IReplace {


	public final static HashMap<String,String> entities = new HashMap();

	private static void loadEntites() {
		if ( ! entities.isEmpty()) return;
		synchronized (entities) {
			Log.d("init.utils", "load html entities");
			InputStream in = WebUtils2.class.getResourceAsStream("entities.csv");
			assert in != null : "missing entities.csv";
			String s = FileUtils.read(in);
			String[] lines = StrUtils.splitLines(s);
			for (String line : lines) {
				if (line.isEmpty() || line.charAt(0)=='#') continue;
				String[] bits = line.split("\t");
				entities.put(bits[1].trim(), bits[0]);
			}
		}
	}
	
	private String plain;
	
	public HtmlDecoder(String plain) {
		loadEntites();
		this.plain = plain;
	}
	
	static final Pattern CODE_CHECK = Pattern.compile("(x[0-9A-Fa-f]{2,8}|\\d{1,8})");
	
	@Override
	public void appendReplacementTo(StringBuilder sb, Matcher match) {
		int s = match.start();
		char c = plain.charAt(s+1);
		// A unicode number?
		if (c=='#') {
			String key = plain.substring(s+2, match.end()-1);
			if ( ! CODE_CHECK.matcher(key).matches()) {
				// Log.w("WebUtils", "Malformed entity code: "+key); Happens -- misc junk from tweets
				sb.append(match.group());
				return;
			}
			int code;
			if (plain.charAt(s+2)=='x') {					
				key = "0"+key;
				code = Integer.decode(key);							
			} else {										
				code = Integer.valueOf(key);						
			}
			sb.append((char)code);
			return;
		}
		// an entity like &nbsp;?
		String key = plain.substring(s+1, match.end()-1);
		String e = entities.get(key);
		if (e==null) {
			// Log.w("WebUtils", "Unrecognised entity: "+key); Happens -- misc junk from tweets
			sb.append(match.group());			
			return;
		}
		sb.append(e);
	}
}