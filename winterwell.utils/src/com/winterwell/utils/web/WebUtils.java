/*
 * (c) Winterwell Associates Ltd, 2008-2011
 * All rights reserved except.
 */
package com.winterwell.utils.web;

import java.awt.Color;
import java.awt.Desktop;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.Attributes;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import com.winterwell.utils.Environment;
import com.winterwell.utils.FailureException;
import com.winterwell.utils.IReplace;
import com.winterwell.utils.Key;
import com.winterwell.utils.NotUniqueException;
import com.winterwell.utils.Printer;
import com.winterwell.utils.Proc;
import com.winterwell.utils.StrUtils;
import com.winterwell.utils.TodoException;
import com.winterwell.utils.Utils;
import com.winterwell.utils.WrappedException;
import com.winterwell.utils.containers.AbstractMap2;
import com.winterwell.utils.containers.ArraySet;
import com.winterwell.utils.containers.ITree;
import com.winterwell.utils.containers.Tree;
import com.winterwell.utils.gui.GuiUtils;
import com.winterwell.utils.io.FileUtils;
import com.winterwell.utils.io.ISerialize;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.time.Dt;
import com.winterwell.utils.time.TUnit;
import com.winterwell.utils.time.Time;

/**
 * Web and xml-related utils.
 *
 * @author daniel
 * @testedby  WebUtilsTest}
 * @see WebUtils2
 */
public class WebUtils {
	
	private static String _RENDER_WEBPAGE_JS;
	
	private static DocumentBuilderFactory docBuilderFactory;

	private static String fqdn;

	private static String hostname; // Cache for hostname

	private static final String HTTP_PREFIX = "http://";

	private static final String HTTPS_PREFIX = "https://";

	public static final Pattern IP4_ADDRESS = Pattern
			.compile("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}");

	/**
	 * [js, css]
	 */
	public static final String[] JQUERY_UI_URLS = new String[] {
			"http://ajax.googleapis.com/ajax/libs/jqueryui/1.8.5/jquery-ui.min.js",
			"http://soda.sh/static/style/jquery-ui-1.8.5.custom.css" // Bleurgh
																		// - a
																		// sodash
																		// url
	};

	public static final String JQUERY_URL = "http://ajax.googleapis.com/ajax/libs/jquery/1.3.2/jquery.min.js";

	public static final String MIME_TYPE_HTML = "text/html";

	public static final String MIME_TYPE_CSV = "text/csv";

	/**
	 * HTML, utf-8 encoded
	 */
	// NB: charset is case-insensitive, c.f. http://stackoverflow.com/questions/10888929/should-html-meta-charset-be-lowercase-or-uppercase
	public static final String MIME_TYPE_HTML_UTF8 = "text/html; charset=utf-8";

	/**
	 * application/javascript text/javascript is officially obsolete but still
	 * the most widely used.
	 */
	public static final String MIME_TYPE_JAVASCRIPT = "application/javascript";

	/**
	 * Official mime-type for json: application/json.
	 * <p>
	 * But older IE ( < 9) needs plain/text instead. See bug #4537.
	 * There is a hack for this in WebUtils2.sendJson()
	 */
	public static final String MIME_TYPE_JSON = "application/json";
	public static final String MIME_TYPE_JSONP = "application/javascript";
	
	public static final String MIME_TYPE_MULTIPART_ALT = "multipart/alternative";
	
	public static final String MIME_TYPE_MULTIPART_MIXED = "multipart/mixed";
	public static final String MIME_TYPE_RSS = "application/rss+xml";
	/**
	 * Apparently there is no official markdown MIME type :(
	 */
	public static final String MIME_TYPE_TXT_MARKDOWN = "text/x-web-markdown";

	/**
	 * Plain text, utf-8 encoded
	 */
	public static final String MIME_TYPE_TXT_UTF8 = "text/plain; charset=utf-8";

	public static final String MIME_TYPE_XML = "application/xml";
	
	/**
	 * Matches an xml comment - including some bad versions
	 */
	public static final Pattern pComment = Pattern.compile("<!-+.*?-+>",
			Pattern.DOTALL);

	/**
	 * Matches a doctype element.
	 */
	public static final Pattern pDocType = Pattern.compile("<!DOCTYPE.*?>",
			Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

	/**
	 * Matches href="whatever" and variants. Group 1 = the url.
	 */
	public static final Pattern pHref = Pattern.compile(
			"href=['\"]?([^'\"> \r\n\t]+)['\"]?", Pattern.CASE_INSENSITIVE);

	/**
	 * Used in strip tags to get rid of scripts and css style blocks altogether.
	 */
	public static final Pattern pScript = Pattern.compile(
			"<(script)[^<>]*>.+?</(script)>",
			Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

	public static final Pattern pStyle = Pattern.compile(
			"<(style)[^<>]*>.+?</(style)>",
			Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

	private static final String RELATIVE_PREFIX = "//";
	static SAXParserFactory saxParserFactory = null;
	/**
	 * Matches an xml tag, e.g. &lt;a>, &lt;br/>, or &lt;/a>.
	 */
	public static final Pattern TAG_REGEX = Pattern.compile(
			"<(/?[a-zA-Z][a-zA-Z0-9]*)[^>]*>", Pattern.DOTALL);

	/**
	 * Matches urls. Note: Excludes any trailing .	<br>
	 * Group 1: the host/domain (including subdomain) - can be "" for a file url	<br>
	 * Urls must contain the http(s) or ftp protocol (compare with Twitter's regex which doesn't require a protocol).
	 * 
	 * E.g. "https://twitter.com/foo"
	 * 
	 * @testedy {@link WebUtilsTest#testUrlRegex()}
	 * 
	 * See #URL {@link winterwell.jtwitter.Regex#VALID_URL}, which is more flexible */
	public static final Pattern URL_REGEX = Pattern
			.compile("[a-z]{3,6}://([a-zA-Z0-9_\\-\\.]*)\\/?([a-zA-Z0-9_%\\-\\.,\\?&\\/=\\+'~#!\\*:]+[a-zA-Z0-9_%\\-&\\/=\\+])?");

	
	private static final Pattern URL_WEB_DOMAIN_FALLBACK_REGEX = Pattern.compile(URL_REGEX.pattern()+"|[a-z0-9_\\-\\.]+\\.(\\w{2,24})");

	/**
	 * Match either a url or a domain (e.g. "sodash.com").
	 * @see #getDomain(String)
	 */
	public static final Pattern URL_WEB_DOMAIN_REGEX = URL_WEB_DOMAIN_REGEX();

	/**
	 * Note: XPaths are not thread safe, so best to create new ones as needed
	 */
	public static final XPathFactory XPATH_FACTORY = XPathFactory.newInstance();


	
	public static <X> String addQueryParameter(String url, Key<X> param, X value) {
		return addQueryParameter(url, param.getName(), value);
	}
	
	/**
	 * Set a parameter in a GET request. This will add the parameter or replace
	 * it's current value as necessary. I.e. if you add the same param twice,
	 * only the last value will stick. Convenience method for
	 * {@link #addQueryParameter(StringBuilder, String, Object)}.
	 *
	 * @param url
	 * @param param
	 * @param value
	 *            A normal string - this will be url encoded
	 * @return
	 * @see #buildUri(StringBuilder, Object...) TODO rationalise the url
	 *      building methods?
	 */
	public static String addQueryParameter(String url, String param, Object value) {
		StringBuilder sb = new StringBuilder(url);
		addQueryParameter(sb, param, value);
		return sb.toString();
	}

	/**
	 * Set a parameter in a GET request. This will add the parameter or replace
	 * it's current value as necessary. I.e. if you add the same param twice,
	 * only the last value will stick.
	 *
	 * @param url
	 * @param param
	 *            This will not be url-encoded. It should be either of a form
	 *            that does not need url encoding (ie [a-zA-Z0-9]+) or already
	 *            url encoded.
	 * @param value
	 *            A normal string - this will be url encoded. Can be null.
	 * @see #buildUri(StringBuilder, Object...)
	 */
	public static void addQueryParameter(StringBuilder url, String param,
			Object value) {
		assert url != null && param != null;
		// assert WebUtils.urlEncode(param).equals(param) : param;
		String v = WebUtils.urlEncode(value);
		// Remove any existing values of the arg
		// FIXME: Consider using a custom parser to avoid the String conversion?
		removeQueryParameter(url, param);
		if (url.lastIndexOf("?") == -1) {
			url.append('?');
		}
		char end = url.charAt(url.length() - 1);
		if (end != '?' && end != '&') {
			url.append("&");
		}
		url.append(param);
		url.append('=');
		url.append(v);
	}	
	
	/**
	 * Wrapper around {@link #addQueryParameters(String, Map)}
	 * @param url
	 * @param vars
	 *            keys & values will be converted using their toString() methods,
	 *            or if the key implements ISerialize (eg AField) then that converts the value.
	 * @return
	 */
	public static String addQueryParameters(String url, Map<?, ?> vars) {
		if (vars == null || vars.isEmpty())
			return url;
		StringBuilder sb = new StringBuilder(url);
		addQueryParameters(sb, vars);
		return sb.toString();
	}

	/**
	 * Append a query parameter map to a StringBuilder
	 * @param sb
	 * @param vars
	 */
	public static void addQueryParameters(StringBuilder sb, Map<?, ?> vars) {		
		for (Entry e : vars.entrySet()) {
			try {
				Object key = e.getKey();
				String k = key.toString();
				Object val = e.getValue();
				if (val==null) {
					addQueryParameter(sb, k, "");
					continue;
				}
				// Is it an AField, which handles its own conversion?
				String v;
				if (key instanceof ISerialize) {
					v = ((ISerialize)key).toString(val);
				} else {
					v = String.valueOf(val);
				}
				
				// ??Is it an array or List??
				
				addQueryParameter(sb, k, v);
			} catch(Exception ex) {
				throw Utils.runtime(ex);
			}
		}
	}
	
	
	
	public static List<Node> asList(final NodeList scripts) {
		return new AbstractList<Node>() {
			@Override
			public Node get(int index) {
				return scripts.item(index);
			}

			@Override
			public int size() {
				return scripts.getLength();
			}
		};
	}

	/**
	 * 
	 * @param node
	 * @return a Map view of the node's attributes. Edits write through to the node
	 */
	public static Map<String, String> getAttributeMap(Node node) {
		return new NNMap((Element) node);
	}
	
	
	/**
	 * Encode text so that it can be used as the value of an XML attribute. Does
	 * not add surrounding quote marks.
	 *
	 * @param text
	 *            Can be null, which will return as the empty string.
	 * @return
	 */
	public static String attributeEncode(String text) {
		if (text == null)
			return "";
		StringBuilder sb = new StringBuilder(text.length() + 5);
		attributeEncode(sb, text);
		return sb.toString();
	}

	/**
	 * Encode text so that it can be used as the value of an XML attribute. The
	 * W3 spec is a little hazy on this (no, really). We encode ', &quot; and
	 * &amp;. All other chars are left alone. Does not add surrounding quote
	 * marks.
	 *
	 * @param helpText
	 */
	public static void attributeEncode(StringBuilder out, CharSequence text) {
		for (int i = 0, len = text.length(); i < len; i++) {
			char c = text.charAt(i);
			if (c == '\'') {
				out.append("&#39;"); // alt &#x27; but NOT &apos;
			} else if (c == '"') {
				out.append("&quot;"); // alt &#x22;
			} else if (c == '&') {
				out.append("&amp;");
			} else {
				out.append(c);
			}
		}
	}

	/**
	 * Convert a color into an html code
	 *
	 * @param col
	 * @return E.g. "#ff0000" for Color.RED, "rgba(0,255,0,128)" for transparent
	 *         green. Will always use the #hex form if there is no transparency
	 *         (ie alpha=255)
	 * @see GuiUtils#getColor(String)
	 */
	public static String color2html(Color col) {
		StringBuilder html = new StringBuilder(7);
		int r = col.getRed();
		int g = col.getGreen();
		int b = col.getBlue();
		// alpha
		int a = col.getAlpha();
		if (a != 255) {
			float af = a / 255.0f;
			// this should work in CSS
			html.append("rgba(" + r + "," + g + "," + b + "," + af + ")");
			return html.toString();
		}
		// TODO TEST Would this be simpler & better? "#" + String.format("%02x", color.getRed()) + String.format("%02x", color.getGreen()) + String.format("%02x", color.getBlue());
		html.append('#');
		color2html2_hex(r, html);
		color2html2_hex(g, html);
		color2html2_hex(b, html);
		return html.toString();
	}
	private static void color2html2_hex(int r, StringBuilder html) {
		String hr = Integer.toHexString(r);
		if (hr.length() == 1) {
			html.append('0');
		} else {
			assert hr.length() == 2;
		}
		html.append(hr);
	}

	/**
	 * Basic function is to translate the checked exception into an unchecked so
	 * that this can be used to initialise static fields.
	 *
	 * @param expression
	 * @return
	 */
	public static XPathExpression compileXPathExpression(String expression) {
		try {
			return XPATH_FACTORY.newXPath().compile(expression);
		} catch (XPathExpressionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw Utils.runtime(e);
		}
	}

	/**
	 * Try to get the server name or IP address for a site. Linux only at
	 * present!
	 *
	 * @param site
	 *            alias or an IP address
	 * @param returnIP
	 *            If true, return an IPv4 address. If false, look for a
	 *            server-name.
	 * @return IP or server name. Never null (exception on failure)
	 * @testedby  WebUtilsTest#testGetIPof()} WARNING: this can fail
	 *           sometimes for no good reason that I can see. Retrying usually
	 *           succeeds.
	 */
	public static String dig(final String site, boolean returnIP) {
		try {
			return dig2(site, returnIP);
		} catch(FailureException ex) {
			// try again: dig can fail sporadically (5% of the time on my desktop with this test ^DW)
			return dig2(site, returnIP);
		}
	}

	private static String dig2(String site, boolean returnIP) {
		assert site != null;
		if (!Utils.OSisUnix())
			throw new TodoException();
		// Are you after a reverse lookup for a name?
		String x = "";
		if ( ! returnIP && IP4_ADDRESS.matcher(site).matches()) {
			x = "-x ";
		}
		Proc p = new Proc("dig +short " + x + site);
		p.run();
		int outcode = p.waitFor(2500); // this should be fast -- 2.5 seconds allows aeons of
							// time
		String out = p.getOutput();
		p.close();
		
		// look for an IPv4 address?
		if (returnIP) {
			Matcher m = IP4_ADDRESS.matcher(out);
			if (m.find())
				return m.group();
			throw new FailureException("Couldn't find IP address for " + site
					+ " dig output: " +out+" error: "+p.getError()+" with command: "+p.getCommand()+" (output-code: "+outcode+")");
		}

		// look for a name
		String[] bits = StrUtils.splitLines(out);
		String ip = null;
		for (String string : bits) {
			if (string.isEmpty()) {
				continue;
			}
			if (IP4_ADDRESS.matcher(string).matches()) {
				ip = string;
				continue;
			}
			if (string.endsWith(".")) {
				string = string.substring(0, string.length() - 1);
			}
			return string;
		}

		// try a reverse lookup
		if (ip == null)
			throw new FailureException("Couldn't find server name or ip for "
					+ site + " in [" + out + "] " + p.getError());
		return dig2(ip, false);
	}

	/**
	 * Open an html page in a web browser.
	 *
	 * @param page
	 *            This is an HTML page. It is not a url!
	 */
	public static void display(File file) {
		display(file.toURI());
	}

	/**
	 * Open an html page in a web browser.
	 *
	 * @param page
	 *            This is an HTML page. It is not a url!
	 */
	public static void display(String page) {
		try {
			File f = File.createTempFile("temp", ".html");
			FileUtils.write(f, page);
			display(f);
			// f.deleteOnExit(); bad idea
		} catch (IOException e) {
			throw new WrappedException(e);
		}
	}

	/**
	 *  Open a URI in a system web browser.
	 *
	 * If {@link GuiUtils#isInteractive()} is false, this will return
	 * immediately.
	 *
	 * WARNING (on Ubuntu), this leads to a daemon thread that can prevent
	 * programs terminating until the browser closes!
	 */
	public static void browseOnDesktop(String url) {
		if ( ! GuiUtils.isInteractive())
			return;
		try {
			Desktop d = Desktop.getDesktop();
			java.net.URI uri = URI(url);
			d.browse(uri);
		} catch (UnsupportedOperationException ex) {
			// KDE isn't supported :(
			if (Utils.getOperatingSystem().contains("linux")
					|| Utils.getOperatingSystem().contains("unix")) {
				Proc p = new Proc("xdg-open " + url);
				p.start();
			} else {
				// try for Firefox!
				Proc p = new Proc("firefox " + url);
				p.start();
			}
		} catch (IOException e) {
			throw new WrappedException(e);
		}
	}

	/**
	Convenience for {@link #browseOnDesktop(String)}
	 */
	public static void display(URI uri) {
		browseOnDesktop(uri.toString());
	}

	/**
	 * @param tag
	 *            E.g. "div" Can be badly formed xml
	 * @param includeTag
	 *            If false, return just the tag's content text (not the tag or
	 *            the attributes). Extract all instances of tag from the xml
	 *            page. 
	 * @return instances of tag. May be empty, never null
	 * 
	 * @warning Regex based -- which gives tolerant xml handling, but:<br>
	 * Does NOT cope with nested tags of the same type. <br>
	 * Does NOT cope with self-closing tags that lack the /> ending (e.g. &gt;img &lt;)

	 */
	public static List<String> extractXmlTags(String tag, String xml,
			boolean includeTag) {
		// Open-close
		Pattern p = Pattern.compile("<" + tag + "(\\s+[^>]*)?>(.*?)</" + tag
				+ ">", Pattern.DOTALL);
		Matcher m = p.matcher(xml);
		List<String> list = new ArrayList<String>();
		while (m.find()) {
			list.add(includeTag ? m.group() : m.group(2));
		}
		// One tag (e.g. <img />
		if (includeTag) {
			p = Pattern.compile("<" + tag + "[^>]*/>");
			m = p.matcher(xml);
			while (m.find()) {
				list.add(m.group());
			}
		}
		return list;
	}

	public static List<String> extractXmlTagsSelfClosing(String tag, String xml) {
		// Open-close
		Pattern p = Pattern.compile("<" + tag + "[^>]*>", Pattern.DOTALL
				| Pattern.CASE_INSENSITIVE);
		Matcher m = p.matcher(xml);
		List<String> list = new ArrayList<String>();
		while (m.find()) {
			list.add(m.group());
		}
		return list;
	}

	public static List<String> findUrls(String text) {
		Matcher m = URL_REGEX.matcher(text);
		List<String> urls = new ArrayList();
		while(m.find()) {
			urls.add(m.group());
		}
		return urls;
	}
	
	/**
	 * The hostname or empty string if it could not be determined.
	 *
	 * @return e.g. "egan.soda.sh", or null if the lookup fails Note: this is
	 *         cached
	 */
	public static String fullHostname() {
		if (fqdn != null)
			return fqdn;

		// Calling out to hostname --fqdn is more likely to do something
		// sensible
		for (int i = 0; i < 3; i++) { // Try a few times
			Proc cmd = new Proc("hostname --fqdn");
			cmd.run();
			cmd.waitFor();
			fqdn = cmd.getOutput().trim();
			Log.i("init.web", "Retrieved FQDN: " + fqdn);
			if (!Utils.isBlank(fqdn))
				return fqdn;
		}


		// Alas, the following is not a reliable means of determining the
		// hostname
		// In OpenJDK (at least) it returns a guess based on parsing /etc/hosts
		// It also appears to behave non-deterministically sometimes returing
		// the hostname
		// rather than the FQDN.
		try {
			fqdn = InetAddress.getByName(hostname()).getCanonicalHostName()
					.trim();
			assert !Utils.isBlank(fqdn);
			Log.w("init.web", "Using dodgy FQDN method: " + fqdn);
			return fqdn;
		} catch (UnknownHostException e) {
			Log.e("init.web", e);
			return null;
		}
	}


	/**
	 * Convenience for accessing an attribute value (the Node interface is
	 * rather ugly)
	 *
	 * @param attribute
	 * @param node
	 * @return value or null
	 */
	public static String getAttribute(String attribute, Node node) {
		NamedNodeMap att = node.getAttributes();
		Node w = att.getNamedItem(attribute);
		if (w == null)
			return null;
		return w.getTextContent();
	}

	/**
	 * FIXME problems in the following situation:
	 * getAttribute("href","<a href=abc.com >woo</a>")
	 *
	 * Crude XML tag parser: rips the attribute value out using a text scan. NB:
	 * Attributes must be quoted
	 *
	 * @param attribute
	 * @param tag
	 * @return attribute-value or null
	 */
	public static String getAttribute(String attribute, String tag) {
		attribute += '=';
		int i = tag.indexOf(attribute);
		if (i == -1)
			return null;
		i += attribute.length();
		if (i == tag.length())
			return null;
		// fail? 
		int closed = tag.indexOf('>');
		if (i>closed) {
			// The attribute is in a sub-tag, so doesn't count
			return null;
		}
		char q = tag.charAt(i);
		// quoted
		if (q == '"' || q == '\'') {
			for (int j = i + 1; j < tag.length(); j++) {
				char c = tag.charAt(j);
				// FIXME escaping chars
				if (c == q) {
					return tag.substring(i + 1, j);
				}					
			}
		} else {
			// unquoted
			for (int j = i; j < tag.length(); j++) {
				char c = tag.charAt(j);
				if (Character.isWhitespace(c) || '>' == c)
					return tag.substring(i, j);
			}
		}
		throw new IllegalArgumentException(tag + " is not valid xml");
	}

	/**
	 * The domain, excluding sub-domain
	 * @param url
	 * @return E.g. "soda.sh" from "http://blah.soda.sh/foo", or null on fail
	 * (which should only happen if url is a relative or bogus url).
	 */
	public static String getDomain(String url) {
		if (url==null) return null;
		url = url.trim();
		Matcher m = URL_REGEX.matcher(url);
		if (m.find()) {
			String domain = m.group(1);
			// Chop the front off to give the domain (hopefully)
			return getDomain2_chop(domain);	
		}
		// is it a domain already?
		assert URL_WEB_DOMAIN_REGEX != null;
		if (URL_WEB_DOMAIN_REGEX.matcher(url).matches()) {
			// chop
			return getDomain2_chop(url);
		}
		// try the lenient regex
		if (URL_WEB_DOMAIN_REGEX != URL_WEB_DOMAIN_FALLBACK_REGEX && URL_WEB_DOMAIN_FALLBACK_REGEX.matcher(url).matches()) {
			// chop
			return getDomain2_chop(url);
		}
		// not a domain
		return null;		
	}

	/**
	 * Chop the front off to give the domain (hopefully)
	 * @param domain
	 * @return
	 */
	private static String getDomain2_chop(String domain) {
		int i = domain.lastIndexOf('.');
		if (i==-1) return domain;
		i = domain.lastIndexOf('.', i-1);
		if (i==-1) return domain;
		int di = domain.length() - i;
		if (di < 7) {// .co.uk
			i = domain.lastIndexOf('.', i-1);
			if (i==-1) return domain;
		}
		return domain.substring(i+1);

	}

	/**
	 * Convert a link as found in social media (e.g. full links or fragments) into definitely a full link
	 * @param urlOrDomain e.g. google.com or https://fo.bar/yeah?whatever
	 * @return
	 */
	public static String getFullUrl(String urlOrDomain) {
		if (Utils.isBlank(urlOrDomain)) return null;
		if (urlOrDomain.startsWith("http")) return urlOrDomain;
		String u2 = "https://"+urlOrDomain;
		return u2;		
	}

	/**
	 * The host domain, including sub-domain
	 * @param url
	 * @return E.g. "blah.soda.sh" from "http://blah.soda.sh/foo", or null on fail
	 * (which should only happen if url is a relative or bogus url).
	 * @see #getDomain(String)
	 */
	public static String getHost(String url) {
		if (url==null) return null;
		Matcher m = URL_REGEX.matcher(url);
		if ( ! m.find()) {
			return null;
		}
		String domain = m.group(1);
		return domain;
	}

	/**
	 * Try to get the IP address(es) of the local machine. Unix only at present.
	 *
	 * @return
	 * @testedby  WebUtilsTest#testGetMyIP()}
	 */
	public static List<String> getMyIP() {
		if (Utils.OSisUnix()) {
			Proc p = new Proc("ifconfig");
			try {
				p.run();
				p.waitFor(2000);
				String out = p.getOutput();
				p.close();
				Matcher m = IP4_ADDRESS.matcher(out);
				ArrayList<String> ips = new ArrayList<String>();
				while (m.find()) {
					ips.add(m.group());
				}
				return ips;
			} catch (Exception e) {
				// ifconfig failed?!
				Log.report(e + " " + p.getError(), Level.SEVERE);
				return new ArrayList();
			}
		}
		throw new TodoException();
	}

	/**
	 * The crudest possible http-get.
	 *
	 * @see winterwell.web.FakeBrowser Or Apache's http client
	 * @param url
	 * @return
	 */
	public static String getPage(String url) {
		try {
			HttpURLConnection connection = (HttpURLConnection) new URL(url)
					.openConnection();
			InputStream in = connection.getInputStream();
			String html = FileUtils.read(in);
			return html;
		} catch (Exception ex) {
			throw Utils.runtime(ex);
		}
	}

	/**
	 * Note: There is no official need for urls to have file-type endings. But often they do, and it can
	 * be handy to check.
	 * @param url
	 * @return e.g. ".html" or ".pdf" -- or ""
	 */
	public static String getType(String url) {
		int e = url.indexOf('?');
		if (e==-1) e = url.indexOf('#');
		if (e==-1) e = url.length();
		int s = url.lastIndexOf('.', e);
		if (s==-1) {
			return "";
		}
		return url.substring(s, e);
	}

	/**
	 * @return an XMLReader. Tries to construct a fault-tolerant fast xml reader
	 *         (e.g. switches off DTD loading). This has implications for
	 *         entities! Which might not get properly supported!
	 * @testedby  WebUtilsTest#testGetXMLReader()}
	 */
	public static XMLReader getXMLReader() {
		if (saxParserFactory == null) {
			// Try to avoid using the built-in
			// Which will probably be Xerces, which is fussy (chokes on valid
			// xml, such as BBC RSS feeds)
			// & slow (always downloads the DTD).
			for (String klassName : new String[] { "com.bluecast.xml.JAXPSAXParserFactory" // Piccolo
			// ,"com.ctc.wstx.sax.WstxSAXParserFactory" // Woodstox
			}) {
				try {
					Class<?> klass = Class.forName(klassName);
					saxParserFactory = (SAXParserFactory) klass.newInstance();
				} catch (Exception e) {
					// oh well
				}
			}
			// oh well - fall back to built-in (Xerces)
			if (saxParserFactory == null) {
				saxParserFactory = SAXParserFactory.newInstance();
			}

			// keep it simple
			saxParserFactory.setValidating(false);
			saxParserFactory.setNamespaceAware(false);
			// hopeful switch-offs -- c.f. cf
			// http://stackoverflow.com/questions/155101/make-documentbuilder-parse-ignore-dtd-references
			for (String badFeature : new String[] {
					"http://xml.org/sax/features/namespaces",
					"http://xml.org/sax/features/validation",
					"http://apache.org/xml/features/nonvalidating/load-dtd-grammar",
					"http://apache.org/xml/features/nonvalidating/load-external-dtd" }) {
				try {
					saxParserFactory.setFeature(badFeature, false);
				} catch (Exception e) {
					// oh well
				}
			}
			// saxParserFactory.setXIncludeAware(false); // throws an error!
			// Default is false anyway
			Log.i("init.web", "Using SAX parser " + saxParserFactory);
		}
		try {
			XMLReader reader = saxParserFactory.newSAXParser().getXMLReader();
			// Hopefully switch off DTD -- cf
			// http://stackoverflow.com/questions/155101/make-documentbuilder-parse-ignore-dtd-references
			reader.setEntityResolver(new EntityResolver() {
				@Override
				public InputSource resolveEntity(String publicId,
						String systemId) throws SAXException, IOException {
					return new InputSource(new StringReader(""));
					// return null; ??
				}
			});
			return reader;
		} catch (Exception e) {
			throw Utils.runtime(e);
		}
	}

	/**
	 * The hostname or empty string if it could not be determined.
	 *
	 * @return e.g. "egan" not "egan.soda.sh", or null if the lookup fails Note:
	 *         this is cached.
	 *  Never throws an exception.
	 */
	public static String hostname() {
		if (hostname != null)
			return hostname;
		try {
			hostname = InetAddress.getLocalHost().getHostName();
			assert !hostname.contains("_") : hostname
					+ " will cause problems with internal XIds";
			// assert ! here.contains(".") : here;
			Log.d("network", "Retrieved hostname: " + hostname);
			return hostname;
		} catch (Throwable e) {
			Log.e("network", e);
			return null;
		}
	}

	/**
	 * @param v
	 *            Can be null (returns null)
	 * @deprecated Use WebUtils2.htmlEncode() instead (which wraps a Jakarta
	 *             StringEscapeUtils library). This method mainly exists as a
	 *             reminder! It does provide safety encoding: <>s to protect
	 *             against injection attacks
	 */
	@Deprecated
	public static String htmlEncode(String v) throws TodoException {
		if (v == null)
			return null;
		v = v.replace("<", "&lt;");
		v = v.replace(">", "&gt;");
		return v;
	}

	public static void main(String[] args) {
		if (args == null || args.length == 0) {
			Printer.out("Usage: java -jar com.winterwell.utils.jar COMMAND PARAMS");
			Printer.out("Where COMMAND = render, PARAMS = html-input-file pdf-output-file ");
			System.exit(0);
		}
		String cmd = args[0];
		assert cmd.equals("render");
		String html = FileUtils.read(new File(args[1]));
		File pdf = new File(args[2]);
		renderToPdf(html, pdf, false);
		Printer.out("Rendered pdf at: " + pdf);
	}

	/**
	 *
	 * @param xml
	 * @param namespaceAware
	 * @return
	 * @testedby  WebUtilsTest#testParseXml()}
	 * @see JSoup for html parsing
	 */
	public static Document parseXml(String xml) {
		// // ??Pop the first line if its a DTD spec
		// // This is to prevent the baked in xerces behaviour of making a web
		// call, then throwing an exception unless that web call succeeds
		// if (xml.startsWith("<!DOCTYPE")) {
		// int i = xml.indexOf('<', 1);
		// if (i != -1) {
		// xml = xml.substring(i);
		// }
		// }
		// But then we get other exceptions - with undeclared entities :(
		// TODO find a decent xml parser!
		parseXml2_getFactory();

		try {
			DocumentBuilder builder = docBuilderFactory.newDocumentBuilder();
			InputSource input = new InputSource(new StringReader(xml));
			Document doc = builder.parse(input);
			return doc;
		} catch (Exception e) {
			// why are java xml readers broken?
			Log.w("xml",
					e + " with " + docBuilderFactory + " in "
							+ StrUtils.ellipsize(xml, 140));
			throw Utils.runtime(e);
		}
	}
	
	private static void parseXml2_getFactory() {
		if (docBuilderFactory != null)
			return;
		// TODO Try to avoid using the built-in!
		// Which will probably be Xerces, which is fussy (chokes on valid xml,
		// such as BBC RSS feeds)
		// & slow (always downloads the DTD).
		for (String klassName : new String[] {
		// "com.bluecast.xml.JAXPSAXParserFactory" // Piccolo
		// ,"com.ctc.wstx.sax.WstxSAXParserFactory" // Woodstox
		}) {
			try {
				Class<?> klass = Class.forName(klassName);
				docBuilderFactory = (DocumentBuilderFactory) klass
						.newInstance();
			} catch (Exception e) {
				// oh well
			}
		}
		// oh well - fall back to built-in (Xerces)
		if (docBuilderFactory == null) {
			docBuilderFactory = DocumentBuilderFactory.newInstance();
		}

		// keep it simple
		docBuilderFactory.setNamespaceAware(false);
		docBuilderFactory.setValidating(false);
		// hopeful switch-offs -- c.f. cf
		// http://stackoverflow.com/questions/155101/make-documentbuilder-parse-ignore-dtd-references
		for (String badFeature : new String[] {
				"http://xml.org/sax/features/namespaces",
				"http://xml.org/sax/features/validation",
				"http://apache.org/xml/features/nonvalidating/load-dtd-grammar",
				"http://apache.org/xml/features/nonvalidating/load-external-dtd" }) {
			try {
				docBuilderFactory.setFeature(badFeature, false);
			} catch (Exception e) {
				// oh well
			}
		}
		// What bit of non-validating doesn't Xerces understand?!
		// factory.setXIncludeAware(false); unnecessary and causes errors
		Log.i("init", "Using XML parser " + docBuilderFactory);
	}
	
//	/**
//	 * A lighter-weight alternative to using Document and XPath
//	 *
//	 * @param xml
//	 * @return a tree. The root node has no XMLNode (it is the document
//	 *         super-node).
//	 *
//	 * @see CGIUtils#parseHtmlToTree(String)
//	 */
//	public static Tree<XMLNode> parseXmlToTree(String xml) {
//		XMLReader xmlReader = getXMLReader();
//		XmlTreeBuilder treeBuilder = new XmlTreeBuilder();
//		xmlReader.setContentHandler(treeBuilder);
//		try {
//			xmlReader.parse(new InputSource(new StringReader(xml)));
//			return treeBuilder.getTree();
//		} catch (Exception e) {
//			throw Utils.runtime(e);
//		}
//		// // Do it dirty
//		// treeBuilder = new XmlTreeBuilder();
//		// DirtyXmlReader xmlReader2 = new DirtyXmlReader();
//		// xmlReader2.setContentHandler(treeBuilder);
//		// xmlReader2.parse(new StringReader(xml));
//		// return treeBuilder.getTree();
//	}
	
	/**
	 * Change the protocol prefix of a URL.
	 * @param url A URL with any protocol prefix from "http://", "https://" and "//".
	 * @param protocolPrefix The string to prepend to the URL in place of the original protocol
	 * @return The supplied URL, with its protocol prefix replaced with protocolPrefix, or null if the supplied URL had no valid prefix.
	 */
	private static String protocolTo(String url, String protocolPrefix) {
		String toReturn = null;
		if (url.startsWith(HTTP_PREFIX)) {
			toReturn = url.substring(HTTP_PREFIX.length());
		} else if (url.startsWith(HTTPS_PREFIX)) {
			toReturn = url.substring(HTTPS_PREFIX.length());
		} else if (url.startsWith(RELATIVE_PREFIX)) {
			toReturn = url.substring(RELATIVE_PREFIX.length());
		} else return null;
		
		return protocolPrefix + toReturn;
	}

	/**
	 * Change the protocol prefix of a URL to "http://"
	 * @param url A URL with any protocol prefix from "http://", "https://" and "//".
	 * @return The supplied URL, with its protocol prefix changed to "http://", or null if the supplied URL had no valid prefix.
	 */
	public static String protocolToHttp(String url) {
		return protocolTo(url, HTTP_PREFIX);
	}

	/**
	 * Change the protocol prefix of a URL to "https://"
	 * @param url A URL with any protocol prefix from "http://", "https://" and "//".
	 * @return The supplied URL, with its protocol prefix changed to "https://", or null if the supplied URL had no valid prefix.
	 */
	public static String protocolToHttps(String url) {
		
		return protocolTo(url, HTTPS_PREFIX);
	}

	/**
	 * Change the protocol prefix of a URL to "//" (the protocol-relative prefix, meaning "whatever the referring page's protocol is")
	 * @param url A URL with any protocol prefix from "http://", "https://" and "//".
	 * @return The supplied URL, with its protocol prefix changed to "//", or null if the supplied URL had no valid prefix.
	 */
	public static String protocolToRelative(String url) {		
		return protocolTo(url, RELATIVE_PREFIX);
	}

	/**
	 * Remove a parameter setting. E.g. go from "http:mysite.com/foo?bar=1" to
	 * "http:mysite.com/foo"
	 *
	 * @param path
	 * @param params If they are not present, no problem, no edits are made.
	 * @return path without params
	 * @testedby  WebUtils2Test#testRemoveQueryParameter()}
	 */
	public static String removeQueryParameter(final String path, String... params) {
		// Patterns	
		String path2 = StrUtils.replace(path, Pattern.compile("("+StrUtils.join(params,"|")+")=[^#&]*(#|&|$)"), new IReplace() {			
			@Override
			public void appendReplacementTo(StringBuilder sb, Matcher match) {
				if (match.start()==0) {
					return; // wtf?
				}
				char s = path.charAt(match.start()-1);
				if (s!='?' && s!='&') {
					// not a valid match
					sb.append(match.group());
					return;
				}
				// final parameter?
				if (match.end() >= path.length()) {
					if (s=='?' || s=='&')
						StrUtils.pop(sb, 1);
					return;
				}				
				char e = path.charAt(match.end()-1);
				if (e=='#') {
					StrUtils.pop(sb, 1);
					sb.append('#');
					return; 
				}
			}
		});
		// Corner case: a blank path, which never gets edited by the replace
		if (path2.endsWith("?")) {
			return path2.substring(0, path2.length()-1);
		}
		return path2;
	}

	/**
	 * @deprecated Use {@link #removeQueryParameter(String, String...)}
	 * Is this just a convenience method? Looks slightly less efficient than
	 * working with Strings.
	 *
	 * @param url
	 * @param param
	 */
	@Deprecated
	public static void removeQueryParameter(StringBuilder url, String param) {
		String urlString = url.toString();
		urlString = removeQueryParameter(urlString, param);
		url.delete(0, url.length());
		url.append(urlString);
	}

	public static Proc renderToPdf(String html, File file) {
		try {
			File temp = File.createTempFile("html2pdf", ".html");
			FileUtils.write(temp, html);
			return renderToPdf(temp, file);
		} catch(Exception ex) {
			throw Utils.runtime(ex);
		}
	}

	@Deprecated
	public static void renderToPdf(String html, File file, boolean printStyle) {
		renderToPdf(html, file, printStyle, null);
	}

	/**
	 * @deprecated
	 * This relies on: linux, and PhantomJS and render-webpage.js (see config/bin) being installed and on the path.
	 * 
	 * @param html
	 * @param file
	 *            target pdf file
	 * @param printStyle true to use print media styling, false for WYSIWYG browser-styling.
	 *
	 * @testedby  WebUtilsTest#testRenderToPdf()}
	 */
	public static void renderToPdf(String html, File file, boolean printStyle, String footer) {
		// insert no-Google-header-footer
		String html2 = html.replaceFirst("</(head|HEAD)>", "\n<style>\n	@page { margin:0; size:A4; }\n	body { margin: 1.6cm; }\n</style>\n$0");
		html = html2;
		File temp1 = null;
		try { 			
			temp1 = File.createTempFile("page", ".html");
			System.out.println(temp1);
			FileUtils.write(temp1, html);
			assert temp1.exists();
			renderUrlToPdf(WebUtils.URI(temp1), file, printStyle, footer, TUnit.MINUTE.dt);
		} catch(IOException ex) {
			throw Utils.runtime(ex);
		} finally {
			// clean up
//			FileUtils.delete(temp1);			
		}
	}

	/**
	 * This relies on: linux, and wkhtmltopdf and convert (imagemagick) being
	 * installed
	 *
	 * @param html
	 * @param file
	 */
	public static void renderToPng(String html, File file) {
		File temp1 = null;
		try {
			temp1 = File.createTempFile("chart", ".pdf");
			renderToPdf(html, temp1);
			assert temp1.exists() && temp1.length() > 0;

			// 2. Render, trim and convert to PNG with convert
			pngFromPdf(temp1, file);
		} catch (Exception e) {
			throw Utils.runtime(e);
		} finally {
			// clean up
			if (temp1 != null) {
				FileUtils.delete(temp1);
			}
		}
	}
	
	public static void renderUrlToPng(String url, File file) {
		File temp1 = null;
		try {
			temp1 = File.createTempFile("chart", ".pdf");
			Proc p1 = renderUrlToPdf_usingChrome(url, temp1, "--include-background");
			p1.waitFor(TUnit.MINUTE.dt);
			assert temp1.exists() && temp1.length() > 0;

			// 2. Render, trim and convert to PNG with convert
			pngFromPdf(temp1, file);			
		} catch (Exception e) {
			throw Utils.runtime(e);
		} finally {
			// clean up
			if (temp1 != null) {
				FileUtils.delete(temp1);
			}
		}
	}
	
	/**
	 * Assumes: `convert` aka imagemagick is installed as a command line tool
	 * @param pdfIn
	 * @param pngOut
	 * @throws IOException
	 */
	public static void pngFromPdf(File pdfIn, File pngOut) throws IOException {
		if ( ! pdfIn.exists()) throw new FileNotFoundException("missing pdf input file: "+pdfIn);
//		String crop = "-crop 500x500 +repage "; // crop in case its giant?? Not working as yet :(
		String cmd = "convert -trim -antialias -density 300 "
//				+ crop
				+ pdfIn.getAbsolutePath() + " " + pngOut.getAbsolutePath();
		Log.d(cmd);
		try (Proc p2 = new Proc(cmd)) {
			p2.start();
			p2.waitFor(TUnit.MINUTE.getMillisecs());		
		
			if ( ! pngOut.exists()) {
				throw new IOException("Failed to create " + pngOut + "\t"
						+ p2.getError());
			}
		}
	}

	/**
	 * @deprecated
	 * This uses headless chrome and relies on: chromium-browser being installed and on the path.
	 * 
	 * TODO puppeteer provides more control over headless chrome
	 * 
	 * @param html
	 * @param file
	 *            target pdf file
	 * @param printStyle TODO true to use print media styling, false for WYSIWYG browser-styling.
	 * @param footer Optional footer html (can be null for no-footer)
	 *
	 * @testedby  WebUtilsTest#testRenderToPdf()}
	 */
	public static void renderUrlToPdf(String url, File file, boolean printStyle, String footer, Dt waitFor) {
		Proc p = null;
		if (waitFor==null) waitFor = TUnit.MINUTE.dt;
		try {
			String dgpu = Utils.OSisWindows()? "--disable-gpu " : "";
			p = new Proc(
					"chromium-browser --headless "+dgpu+"--print-to-pdf=\""+file+"\" "+url				
					);			
			p.start();
			int done = p.waitFor(waitFor);
			Log.d("html",
					"RenderToPdf: Command: "+p.getCommand()+" output: " + p.getOutput() + "\t" + p.getError());
			
			if ( ! file.exists())
				throw Utils.runtime(new IOException("render failed: " + p.getCommand() + " failed to create " + file + "\t"
						+ p.getError()));
			else if (file.length()==0)
				throw Utils.runtime(new IOException("render failed: " + p.getCommand() + " created but failed to write to " + file + "\t"
						+ p.getError()));
			if (done!=0) { // TODO throw an exception??
				Log.e("html", "RenderToPdf Fail?! " + p.getError() + " exit-code: " + done + " Command: " + p.getCommand());
			}
			
		} finally {
			FileUtils.close(p);
		}
	}

	private static String renderWebpage() {
		if (_RENDER_WEBPAGE_JS==null || true) {
			_RENDER_WEBPAGE_JS = Environment.getProperty(new Key("WebUtils.render-webpage.js"), "render-webpage.js");
			// Looks like a local filepath? resolve it 
			if (_RENDER_WEBPAGE_JS.contains("/")) {
				File rw = new File(FileUtils.getWorkingDirectory(), _RENDER_WEBPAGE_JS);
				if ( ! rw.exists()) {
					Log.e("render.pdf", "Cannot find render-webpage.js script at "+rw);
				}
				_RENDER_WEBPAGE_JS = rw.getAbsolutePath();
			}
		}
		return _RENDER_WEBPAGE_JS;
	}

	/**
	 * Convenience for calling {@link URI#resolve(String)}.
	 * <p>
	 * E.g. "http://winterstein.me.uk"+"images" returns "http://winterstein.me.uk/images"<br>
	 * "http://winterstein.me.uk"+"http://google.com" returns "http://google.com"<br>
	 * "http://winterstein.me.uk/text"+"/images" returns "http://winterstein.me.uk/images"<br>
	 * "http://winterstein.me.uk/text"+"images" returns "http://winterstein.me.uk/images"<br>
	 * <p>
	 * This differs from the built-in method (URI.resolve) because it removes "pages" from the
	 * base (e.g. the final "index.html" would be chopped out & replaced).
	 * If you don't want the path to be resolved in this (standard) way,
	 * then it is your responsibility to ensure that a trailing slash is
	 * present. The exception to this is where there is no path e.g.
	 * "http://www.google.com" For convenience in these cases a path of "/" will
	 * be supplied.
	 *
	 * @param base
	 *            Typically a website or a directory on a website. Can be null if extension is not null.
	 * @param extension
	 *            This may or may not be an extension (ie it can be an absolute url). Can be null if base is not null.
	 * @return
	 * @testedby WebUtilsTest#testResolveUri_X methods
	 */
	public static URI resolveUri(String base, String extension) {
		if (extension==null) {
			return URI(base);
		}		
		// Is it an absolute url? Then stop here
		URI ext = URI(extension);
		if (ext.isAbsolute()) return ext;
		// a relative url?!
		if (base==null) return ext;
		// if ( ! base.endsWith("/")) base += "/";
		// hack
		URI b = URI(base);
		if (Utils.isBlank(b.getPath())) {
			b = b.resolve("/");
		}
		return b.resolve(ext);
	}

	/**
	 * Sanitize a string for inclusion in a JSON string Because most browsers
	 * will interpret HTML tags e.g. </script> that appear in Javascript as
	 * document level elements. Works by replacing angle brackets with their
	 * character literal equivalents. See
	 * http://www.wwco.com/~wls/blog/2007/04/25
	 * /using-script-in-a-javascript-literal/ for discussion of the problem and
	 * the solution.
	 */
	public static String sanitizeForJson(String data) {
		data = data.replace("<", "\\x3C");
		data = data.replace(">", "\\x3E");
		// UI-destroying LINE-SEPERATOR character of doom. It's really rare, so lets just replace it.
		data = data.replace("\u2028", "");
		return data;
	}

	/**
	 * Encode text so that it can be used as a string in JavaScript. We encode
	 * ', &quot; and &amp;. All other chars are left alone. Does not add
	 * surrounding quote marks.
	 *
	 * @param msg
	 */
	public static String scriptEncode(String msg) {
		// ?? Is this correct?
		return attributeEncode(msg);
	}

	/**
	 * Strip any embedded scripts out of an HTML page. Use this to sanitise user
	 * content to defend against ajax hacking attacks.
	 * <p>
	 * WARNING: this is a bit over-zealous and may mangle some innocent text,
	 * e.g. "onwards=stuff"
	 *
	 * @param xml
	 *            Can be null (returns null)
	 */
	public static String stripScripts(String xml) {
		if (xml == null)
			return null;
		// strip script tags
		String noScript = stripTagContents("script", xml);
		// strip onXXX handlers (this is a bit over-zealous)
		noScript = noScript.replaceAll("\\s(on|ON)\\w+=", "");
		// strip href="javascript:..." urls
		noScript = noScript.replaceAll("href='?\"?javascript:", "");
		return noScript;
	}

	/**
	 * Strip any embedded CSS out of an HTML page. Call this <i>before</i>
	 * stripTags.
	 *
	 * @param xml
	 */
	public static String stripStyle(String xml) {
		return stripTagContents("style", xml);
	}

	/**
	 * Strip out anything embedded in a "tagName" tag from xml. Does not attempt
	 * to handle nested tags. Call <i>before</i> calling stripTags.
	 */
	public static String stripTagContents(String tagName, String xml) {
		assert xml != null && tagName != null : xml;
		Pattern re = Pattern.compile("<" + tagName + ".*?</" + tagName + ">",
				Pattern.DOTALL);
		return re.matcher(xml).replaceAll(" ");
	}

	/**
	 * Remove xml and html tags, e.g. to safeguard against javascript injection
	 * attacks, or to get plain text for NLP.
	 * <p>
	 * You may then wish to use {@link StrUtils#compactWhitespace(String)}
	 *
	 * @param xml
	 *            can be null, in which case null will be returned
	 * @return the text contents - ie input with all tags removed
	 * @testedby  WebUtilsTest#testStripTags()}
	 */
	public static String stripTags(String xml) {
		if (xml == null)
			return null;
		// short cut if there are no tags
		if (xml.indexOf('<') == -1)
			return xml;
		// first all the scripts (cos we must remove the tag contents too)
		Matcher m4 = pScript.matcher(xml);
		xml = m4.replaceAll("");
		Matcher m5 = pStyle.matcher(xml);
		xml = m5.replaceAll("");
		// comments
		Matcher m2 = pComment.matcher(xml);
		String txt = m2.replaceAll("");
		// now the tags
		Matcher m = TAG_REGEX.matcher(txt);
		String txt2 = m.replaceAll("");
		Matcher m3 = pDocType.matcher(txt2);
		String txt3 = m3.replaceAll("");
		return txt3;
	}

	private static String URI(File file) {
		return "file://"+file.getAbsolutePath();
	}

	public static URI URI(String uri) {
		try {			
			// HACK: Spaces are not permitted in URIs, but:
			// - every browser ever has handled this situation sensibly
			// - they are not uncommon on the web
			// So, we patch them up here
			uri = uri.replace(" ", "%20");
			return new URI(uri);
		} catch (URISyntaxException e) {
			throw Utils.runtime(e);
		}
	}

	public static URI URI(URL url) {
		try {
			return url.toURI();
		} catch (URISyntaxException e) {
			throw Utils.runtime(e);
		}
	}

	public static URI URI(URLConnection connection) {
		try {
			return connection.getURL().toURI();
		} catch (URISyntaxException e) {
			throw Utils.runtime(e);
		}
	}

	private static Pattern URL_WEB_DOMAIN_REGEX() {
		// Use the Twitter-provided one if we can
		try {
			Class<?> regex = Class.forName("winterwell.jtwitter.Regex");
			Field f = regex.getField("VALID_URL");
			Pattern p = (Pattern) f.get(null);
			return p;
		} catch(Throwable ex) {
//			Log.d("WebUtils.init", "url regex: Oh well, using fallback instead of Twitter's version: "+ex);
		}
		// Fallback to a simpler one (which will catch most cases)
		Pattern fallback = URL_WEB_DOMAIN_FALLBACK_REGEX;
		return fallback;
	}
	
	public static String urlDecode(String s) {
		try {
			s = URLDecoder.decode(s, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			s = URLDecoder.decode(s);
		}
		return s;
	}
	/**
	 *
	 * @param vars
	 * @return query string ripe for appending onto a url. Does not include a ?.
	 *         E.g., you could do "http://mysite.com?" + urlEncoded, if you were
	 *         so inclined.
	 */
	public static String urlEncode(Map vars) {
		StringBuilder encodedData = new StringBuilder();
		for (Object key : vars.keySet()) {
			Object v = vars.get(key);
			String val = urlEncode(v);
			encodedData.append(urlEncode(key));
			encodedData.append('=');
			encodedData.append(val);
			encodedData.append('&');
		}
		// Remove trailing &
		if (encodedData.length()!=0) StrUtils.pop(encodedData, 1);
		return encodedData.toString();
	}
	/**
	 * URL encode.
	 * Note: this will encode special url chars like /
	 * @param x
	 *            can be null (returns ""). Will be turned into a String using
	 *            String.valueOf()
	 * @testedby  WebUtilsTest#testUrlEncode()}
	 */
	public static String urlEncode(Object x) {
		if (x == null)
			return "";
		String s = String.valueOf(x);
		try {
			s = URLEncoder.encode(s, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			s = URLEncoder.encode(s);
		}
		s = s.replace("+", "%20"); // + for " " seems to be out of date.
//		s = s.replace("/", "%2F"); // Should we encode / eg to protect names in slugs? UrlEncoder does this already. 
		return s;
	}

	/**
	 * Use an xpath query to extract what is expected to be a single string
	 * valued node. This is a convenience method for a common case.
	 *
	 * @param xpathQuery
	 *            Either a String or a pre-compiled XPathExpression
	 * @param node
	 * @return the _first_ resulting node's text content, or null if there is no such
	 *         node.
	 */
	public static String xpathExtractString(Object xpathQuery, Node node)
			throws NotUniqueException {
		List<Node> titles = WebUtils.xpathQuery(xpathQuery, node);
		if (titles.isEmpty())
			return null;
		// Too many? Just log them & take the first (for robustness in the face of odd xml)
		if (titles.size() != 1) {
			Log.w("xpath", xpathQuery+" "+titles.size()+" results! 1. "+titles.get(0)+"="+titles.get(0).getTextContent()+" 2. "+titles.get(1)+"="+titles.get(1).getTextContent());
		}
		Node node2 = titles.get(0);
		String text = node2.getTextContent();
		// Although W3C say
		// "All line-endings reported as a single LF character.", the
		// piece-of-shit Apache parser that ships with Java disagrees.
		text = StrUtils.LINEENDINGS.matcher(text).replaceAll("\n");
		return text;
	}
	
	/**
	 * @see #xpathQuery(String, String, boolean)
	 * @param xpathQuery
	 *            Either a String or a pre-compiled {@link XPathExpression}
	 * @param node
	 * @return Can be empty, never null. Uses clone=true.
	 * @testedby TODO
	 */
	public static List<Node> xpathQuery(Object xpathQuery, Node node) {
		return xpathQuery2(xpathQuery, node, true);
	}
	
	/**
	 * 
	 * @param xpathQuery eg "//book[author="Joseph Heller"]/title"
	 * @param node
	 * @param clone false if you want to do xml surgery -- i.e. add/remove nodes.
	 * @return
	 */
	public static List<Node> xpathQuery2(Object xpathQuery, Node node, boolean clone) {
		try {
			// what kind of input do we have? String or pre-compiled?
			XPathExpression expr;
			if (xpathQuery instanceof String) {
				expr = XPATH_FACTORY.newXPath().compile((String) xpathQuery);
			} else {
				expr = (XPathExpression) xpathQuery;
			}

			// get a context limited to the node
			Node nclone = clone? node.cloneNode(true) : node;
			NodeList nodeList = (NodeList) expr.evaluate(nclone, XPathConstants.NODESET);
			List<Node> nodes = asList(nodeList);
			return nodes;
		} catch (XPathExpressionException e) {
			throw Utils.runtime(e);
		}
	}
	
	/**
	 * Convenience for {@link xpathQuery} without namespace aware parsing
	 *
	 * @param xpathQuery
	 *            E.g. "//book[author="Joseph Heller"]/title"
	 */
	public static List<Node> xpathQuery(String xpathQuery, String xml) {
		return xpathQuery(xpathQuery, xml, false);
	}
	
	/**
	 * Run an XPath query over an xml document.
	 * <p>
	 * <h3>XPath syntax</h3>
	 * E.g. given the document
	 *
	 * <pre>
	 * &lt;shelf>
	 * &lt;book year='1960'>&lt;title>Catch 22&lt;/title>&lt;author>Joseph Heller&lt;/author>&lt;/book>
	 * &lt;book year='2007'>&lt;title>The English Swordsman&lt;/title>&lt;author>Daniel Winterstein&lt;/author>&lt;/book>
	 * &lt;/shelf>
	 * </pre>
	 *
	 * You could have the queries:
	 *
	 * <pre>
	 * "//book[author='Joseph Heller']/title"
	 * "//book[@year='2007']"
	 * "/shelf/book/title"
	 * </pre>
	 *
	 * See http://www.zvon.org/xxl/XPathTutorial/General/examples.html for more
	 * info.
	 * <p>
	 * Note: This method is not optimally efficient if the same query is
	 * repeated, or the same document queried multiple times.
	 *
	 * @param xpathQuery
	 *            E.g. "//book[author="Joseph Heller"]/title"
	 * @param xml
	 * @param namespaceAware
	 * @return
	 */
	public static List<Node> xpathQuery(String xpathQuery, String xml,
			boolean namespaceAware) {
		// Parse XML
		assert !namespaceAware : "TODO";
		Document doc = parseXml(xml);
		// Build an XPath query
		try {
			XPath xp = XPATH_FACTORY.newXPath();
			XPathExpression expr = xp.compile(xpathQuery);
			NodeList nodeList = (NodeList) expr.evaluate(doc,
					XPathConstants.NODESET);
			List<Node> nodes = asList(nodeList);
			return nodes;
		} catch (XPathExpressionException e) {
			throw Utils.runtime(e);
		}
	}

	/**
	 * TODO standardise on chrome-headeless-render-pdf or phantomjs or chromium??
	 * 
	 * NB: This does not seem to be reliable here, though it does work for Jerbil?!
	 * 
	 * Render to pdf (using chrome-headeless-render-pdf)
	 * @param html
	 * @param pdf
	 * @return Proc This has NOT finished! Use proc.waitFor() to wait.
	 * e.g.
	 * <pre><code>
	try (Proc proc = WebUtils.renderToPdf(htmlFile, pdfFile)) {
		proc.waitFor();	
	}
	</code></pre>
	 */
	public static Proc renderToPdf(File html, File pdf) {
		return renderToPdf_usingChrome(html, pdf, null);
	}
	
	public static Proc renderToPdf_usingChrome(File html, File pdf, String options) {
		// works in Jerbil?? But can spit out raw html??
		Proc proc = new Proc(
				"chrome-headless-render-pdf"
				+ (options==null? "" : " "+options)				
				+ " --url=file://"+html.getAbsolutePath()+" --pdf="+pdf.getAbsolutePath());
		Log.d("pdf", proc.getCommand());
		proc.start();
		return proc;
	}
	
	/**
	 * Dependency: npm -i -g chrome-headless-render-pdf
	 * @param url
	 * @param pdf
	 * @param options e.g. for an A5 certificate "--no-margins --include-background --page-ranges 1 --scale 0.87 --landscape --paper-width 5.8 --paper-height 8.3"
	 * @return
	 * @throws MalformedURLException
	 */
	public static Proc renderUrlToPdf_usingChrome(String url, File pdf, String options) throws MalformedURLException {
		// works in Jerbil?? But can spit out raw html??
		URL u = new URL(url);
		Proc proc = new Proc(
				"chrome-headless-render-pdf"
				+ (options==null? "" : " "+options)				
				+ " --url="+u+" --pdf="+pdf.getAbsolutePath());
		Log.d("pdf", proc.getCommand());
		proc.start();
		return proc;
	}

	/**
	 * see https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Last-Modified
	 * @param lmt
	 * @return
	 */
	public static Time parseHeaderTime(String ht) {
		if (ht==null) return null;
		try {
			// <day-name>, <day> <month> <year> <hour>:<minute>:<second> GMT
			// e.g. "Wed, 21 Oct 2015 07:28:00 GMT"
			SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z");
			Date d = sdf.parse(ht);
			return new Time(d);
		} catch (ParseException e) {
			// swallow
			Log.w("WebUtils.parseHeaderTime", e);
			return null;
		}		
	}


}


class NNMap extends AbstractMap2<String, String> {

	private final Element base;

	public NNMap(Element nnMap) {
		this.base = nnMap;
	}
	
	@Override
	public String remove(Object key) {
		String old = get(key);
		this.base.removeAttribute((String) key);
		return old;
	}
	
	@Override
	public Set<String> keySet() throws UnsupportedOperationException {
		NamedNodeMap ba = base.getAttributes();
		int n = ba.getLength();
		ArraySet s = new ArraySet();
		for (int i = 0; i < n; i++) {
			Node item = ba.item(i);
			String name = item.getNodeName();
			s.add(name);
		}
		return s;
	}

	@Override
	public String get(Object attribute) {
		String w = base.getAttribute((String) attribute);
		return w;
	}
	
	@Override
	public String put(String attribute, String value) {
		String old = get(attribute);
		base.setAttribute(attribute, value);
		return old;
	}
	
}