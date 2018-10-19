package com.winterwell.web;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.http.client.RedirectException;

import com.winterwell.utils.FailureException;
import com.winterwell.utils.Printer;
import com.winterwell.utils.StrUtils;
import com.winterwell.utils.TodoException;
import com.winterwell.utils.Utils;
import com.winterwell.utils.WrappedException;
import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.utils.containers.Containers;
import com.winterwell.utils.io.FileUtils;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.web.Cooldown;
import com.winterwell.utils.web.WebUtils;
import com.winterwell.utils.web.WebUtils2;
import com.winterwell.web.data.XId;

import lgpl.haustein.Base64Encoder;

/**
 * A pretend web browser. Stores cookies so it can step through sessions.
 * <i>Not</i> thread safe.
 * 
 * HTMLUnit is a much more complete version of this (it includes Javascript
 * handling). Apache's HttpClient is a more complex version of this (more boilerplate but
 * also more options).
 * 
 * FIXME: SSL is currently hacked to disable any kind of certificate checks or
 * man-in-the-middle detection. At the very least this should be optional.
 * 
 * @author daniel
 * @testedby {@link FakeBrowserTest}
 */
public class FakeBrowser {

	static final int DEFAULT_TIMEOUT = 60000;

	private static SSLContext INSECURE_SSL_CONTEXT;

	static Pattern keyValue = Pattern
			.compile("([^; \\t\\r\\n\\]]+)=([^; \\t\\r\\n\\]]+)");

	public static final String MIME_TYPE_URLENCODED_FORM = "application/x-www-form-urlencoded";

	boolean reqGzip = false;
	
	/**
	 * Toggles the "Accept-Encoding" header to request gzip compression
	 * @param gzip
	 */
	public void setRequestGzip(boolean gzip) {
		this.reqGzip = gzip;
		if (gzip) {
			// Add a "we take gzip here" marker
			reqHeaders.put("Accept-Encoding", "gzip");
			// + (gzip) to the user-agent 
			// This hack is for Google API See https://developers.google.com/discovery/v1/performance#gzip
			String ua = getUserAgent();
			if ( ! ua.contains("gzip")) {
				ua += " (gzip)";
				setUserAgent(ua);
			}
		} else {
			// No gzip!
			Object ae = reqHeaders.get("Accept-Encoding");
			if ("gzip".equals(ae)) {
				reqHeaders.remove("Accept-Encoding");
			}
		}
	}
	
	static final Pattern pInput = Pattern.compile("<input[^>]+>",
			Pattern.DOTALL);

	static final Pattern pName = Pattern.compile("name=['\"](.*?)['\"]",
			Pattern.DOTALL);

	static final Pattern pName2 = Pattern
			.compile("name=(\\S+)", Pattern.DOTALL);

	// TODO unit tests for these patterns
	private static final Pattern pSrc = Pattern.compile("src=['\"](.+?)['\"]",
			Pattern.DOTALL);

	static final Pattern pValue = Pattern.compile("value=['\"](.*?)['\"]",
			Pattern.DOTALL);

	static final Pattern pValue2 = Pattern.compile("value=(\\S+)",
			Pattern.DOTALL);

	private static final int MAX_REDIRECTS = 10;

	private static final String LOGTAG = "FakeBrowser";

	/**
	 * Chrome v50
	 */
	private static final String DEFAULT_USER_AGENT = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/50.0.2661.75 Safari/537.36";

	/**
	 * Use this if you want to let servers know its FakeBrowser
	 */
	public static final String HONEST_USER_AGENT = "GoodLoopJavaBrowser";	

	static {
		try {
			INSECURE_SSL_CONTEXT = SSLContext.getInstance("SSL");
			INSECURE_SSL_CONTEXT.init(null,
					new TrustManager[] { new X509TrustManager() {

						@Override
						public void checkClientTrusted(X509Certificate[] arg0,
								String arg1) throws CertificateException {
							// TODO Auto-generated method stub

						}

						@Override
						public void checkServerTrusted(X509Certificate[] arg0,
								String arg1) throws CertificateException {
							// TODO Auto-generated method stub
						}

						@Override
						public X509Certificate[] getAcceptedIssuers() {
							// TODO Auto-generated method stub
							return null;
						}

					} }, null);
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException("Unable to initialise SSL context", e);
		} catch (KeyManagementException e) {
			throw new RuntimeException("Unable to initialise SSL context", e);
		}
	}

	private int code;

	private HttpURLConnection connection;

	/**
	 * Map host to key/value pairs TODO this doesn't support deleting cookies,
	 * or proper domain/path handling
	 */
	private final Map<String, Map<String,String>> cookieJar = new HashMap();

	private File downloadFile;
	/**
	 * Request image and css files to look that bit more genuine.
	 */
	private boolean downloadImages = false;

	/**
	 * response headers
	 */
	private Map<String, List<String>> headers;

	/**
	 * If true (the default) binary data is ignored. If false, data is stored in
	 * {@link #downloadFile}
	 */
	private boolean ignoreBinaryFiles = true;

	/**
	 * Store the last uri requested
	 */
	private String location;

	/**
	 * Only allow 10 mb?!
	 * -1 => unlimited
	 */
	private long MAX_DOWNLOAD = 10 * 1024 * 1024;

	String name;

	private boolean parsePagesFlag;

	String password;

	/**
	 * Actually run JavaScript. Requires a DOM and is never going to be 100%
	 * compliant.
	 */
	private boolean runScripts = false;

	/**
	 * If true, the web page/object will be saved to a file regardless of its
	 * type or the ignoreBinaryFiles setting.
	 */
	private boolean saveToFile;

	/** Delay a second before posts? */
	private final boolean slowPosts = false;
	int timeOutMilliSecs = DEFAULT_TIMEOUT;

	private File userDownloadFile;

	private boolean followRedirects = true;
	
	private boolean debug;
	
	/**
	 * Sets whether HTTP redirects (requests with response code 3xx) should be automatically followed. 
	 * True by default.
	 */
	public void setFollowRedirects(boolean followRedirects) {
		this.followRedirects = followRedirects;
	}

	private void disconnect() {
		if (connection == null)
			return;
		// Do we also need to call close on the streams??
		connection.disconnect();
		connection = null;
	}

	/**
	 * Fetch a binary file such as an image
	 * 
	 * @param uri
	 * @return file containing the data
	 * @throws IllegalArgumentException
	 *             if uri returns text
	 */
	public File getFile(String uri) {
		// if (ignoreBinaryFiles) throw new
		// IllegalStateException("Setup to ignore binary data!");
		boolean oldSTF = saveToFile;
		saveToFile = true;
		try {
			String p = getPage(uri);
			if (p != null)
				throw new IllegalArgumentException(uri + " returned text: " + p);
			return downloadFile;
		} finally {
			saveToFile = oldSTF;
		}
	}

	/**
	 * Extract the form fields from a page
	 * 
	 * @param page
	 * @param formAction
	 * @return the key/value pairs from the form, or null if the form was not
	 *         found
	 */
	public Map<String, String> getFormFields(String page, String formAction) {
		Map<String, String> map = new HashMap<String, String>();
		Pattern pForm = Pattern.compile("<form[^>]+action=." + formAction
				+ ".+?</form>", Pattern.DOTALL);
		String[] form = StrUtils.find(pForm, page);
		if (form == null)
			return null;
		Matcher m = pInput.matcher(form[0]);
		while (m.find()) {
			String input = m.group();
			String[] bits = StrUtils.find(pName, input);
			String[] bits2 = StrUtils.find(pValue, input);
			if (bits != null) {
				map.put(bits[1], bits2 == null ? null : bits2[1]);
			}
		}
		return map;
	}

	/**
	 * @param host e.g. "winterwell.com"
	 * @return cookie-to-value
	 */
	public Map<String,String> getHostCookies(String host) {
		Map<String,String> cookies = cookieJar.get(host);
		return cookies;
	}

	/**
	 * FIXME need to have two connection objects: one for pages, one for images,
	 * etc.
	 * 
	 * @return The last url fetched. Note: If a redirect happens, then this is the redirected-to url!
	 */
	public String getLocation() {
		return location;
	}

	/**
	 * Convenience for {@link #getPage(String, Map)} with no parameters.
	 */
	public String getPage(String uri) throws WrappedException {
		return getPage(uri, null);
	}

	/**
	 * 
	 * @param uri
	 * @param vars
	 * @return the web page as text, or null if binary data. The page will also
	 *         be stored in the FakeBrowser object if parsing is switched on.
	 * @throws IORException
	 *             if something goes wrong
	 */
	public String getPage(String uri, Map<String, String> vars) {
		return getPage2(uri, vars, 0);
	}
	
	/**
	 * 
	 * @param uri
	 * @param vars
	 * @param depth Track redirects c.f. http://en.wikipedia.org/wiki/URL_redirection#Redirect_loops
	 * @return
	 */
	String getPage2(String uri, Map<String, String> vars, int depth) {
		// e.g. 2 retries = 3 tries in total
		int tries = Math.max(retryOnError + 1, 1);		
		Exception err = null;
		for(int t=0; t<tries; t++) {
			try {
				assert uri != null : "URI is null!";
				assert timeOutMilliSecs > 0 : "problem with timeOutMilliSecs";
				// Build URI with query params
				uri = WebUtils.addQueryParameters(uri, vars);
				if (debug) {
					Log.d("get", uri);
				}
				// Setup a connection
				setupConnection(uri, timeOutMilliSecs);
				location = uri;
				// Open a connection and process response
				String response = processResponse();
				return response;
			} catch (WebEx.Redirect e) {
				return getPage3_redirect(uri, vars, depth, e);
			} catch (Exception ex) {
				// pause, unless that was our last try (in which case we'll exit the for loop and throw an exception)
				if (t < tries-1) {					
					Utils.sleep(t*t*100);
				}
				err = ex;
			} finally {
				disconnect();
			}
		}
		// fail if here (out of tries)
		throw Utils.runtime(err);
	}
	

	private String getPage3_redirect(String uri, Map<String, String> vars, int depth, WebEx.Redirect e) 
	{
		if ( ! followRedirects) {
			throw e; 
		}
		if (depth > MAX_REDIRECTS) {
			// Give up (let's call it a 500 since it's the remote server's fault)
			throw new WebEx.E50X(500, uri, "Too many redirects"); 
		}
		// Handle a redirect
		String redirect = e.to;
		URI r2 = WebUtils.resolveUri(uri, redirect);
		redirect = r2.toString();
		if (redirect.equals(uri)) {
			throw new WebEx.E50X(500, uri, "Loopy redirect");
		}
		// Redirect
		connection.disconnect();
		connection = null;
		return getPage2(redirect, vars, depth+1);
	}

	/**
	 * @param string
	 * @return
	 */
	public List<String> getResponseHeaders(String header) {
		return headers.get(header);
	}
	
	public Map<String, List<String>> getResponseHeaders() {
		return headers;
	}

	/**
	 * @return
	 * @throws IOException
	 */
	public int getStatus() throws IOException {
		return code;
	}

	String requestMethod;

	private String errorPage;

	private transient Map<String, String> debugVars;
	
	/**
	 * Fake a form POST.
	 * 
	 * @param uri
	 *            The uri to post to.
	 * @param vars
	 *            The form variables to send. These are URL encoded before
	 *            sending.
	 * @return The response from the server.
	 */
	public String post(String uri, Map<String, String> vars) {
		this.debugVars = vars;
		String encodedData = WebUtils.urlEncode(vars);
		return post(uri, MIME_TYPE_URLENCODED_FORM, encodedData);
	}
	

	/**
	 * @param uri
	 *            The uri to post to.
	 * @param body
	 *            This will be URL encoded before sending.
	 * @return The response from the server.
	 */
	public String post(String uri, String body) {
		String encodedData = WebUtils.urlEncode(body);
		return post(uri, MIME_TYPE_URLENCODED_FORM, encodedData);
	}

	/**
	 * TODO merge with {@link #post(String, Map)} if successful
	 * 
	 * @param uri
	 * @param contentType e.g. FakeBrowser.MIME_TYPE_URLENCODED_FORM
	 * @param encodedPostBody
	 *            ??
	 * @return
	 * @throws WrappedException of IOException
	 */
	public String post(String uri, String contentType, String encodedPostBody) 
	throws WrappedException
	{
		// People are never too fast
		if (slowPosts) {
			Utils.sleep(new Random().nextInt(750));
		}
		// uri = uri.replace("format", format.toString());
		if (debug) {
			String sheaders = StrUtils.join(Containers.apply(reqHeaders.keySet(), h -> {
				Object v = reqHeaders.get(h);
				return v==null? "" : " -H '"+h+": "+v+"'";
			}), " ");
			String postBody = WebUtils2.urlDecode(encodedPostBody);
			String curl = StrUtils.compactWhitespace("curl -XPOST -d '"+postBody+"'"+sheaders+" '"+uri+"'");
			Log.d(LOGTAG, curl);
			if (debugVars==null) debugVars = new ArrayMap("encodedBody", encodedPostBody);
		}
		try {
			connection = setupConnection(uri, DEFAULT_TIMEOUT);
			// Post out
			connection.setDoOutput(true);
			if (requestMethod==null) {
				connection.setRequestMethod("POST");
			}
			if (contentType!=null) {
				connection.setRequestProperty("Content-Type", contentType);				
			}
			byte[] bytes = encodedPostBody.getBytes();			
			connection.setRequestProperty("Content-Length",
					String.valueOf(bytes.length));
			OutputStream os = connection.getOutputStream();
			os.write(bytes);
			FileUtils.close(os);
			// Response
			// TODO handle redirect, copy fetch code
			return processResponse();
		} catch (IOException ex) {
			throw new WrappedException(ex);
		} finally {
			disconnect();
		}
	}

	/**
	 * 
	 * @param connection
	 * @return text or null if binary data. Binary data will be either ignored
	 *         or stored in a file
	 * @throws IOException
	 */
	private String processResponse() throws IOException {
		errorPage = null;
		// Cookies
		try {
			updateCookies();
		} catch(IllegalArgumentException ex) {
			// First access of http headers -- can throw this due to a bug in Sun's connection handling
			//   java.lang.IllegalArgumentException: protocol = http host = null
	        // ... at sun.net.www.protocol.http.HttpURLConnection.getHeaderFields(HttpURLConnection.java:2236)
			// Seen with http://green.blogs.nytimes.com/2010/01/28/brewer-invests-in-watershed-protection/@web
			throw new IOException("Java bug fail: "+getLocation()+" "+ex);
		} catch(NoSuchElementException ex) {
			// WTF?
			throw new IOException("Odd fail: "+getLocation()+" "+ex);
		}
		// Process error codes
		processResponse2_errorCodes();
		// data stream
		headers = connection.getHeaderFields();
		InputStream inStream = connection.getInputStream();
		List<String> enc = headers.get("Content-Encoding");
		if (enc!=null && ! enc.isEmpty() && enc.get(0).equalsIgnoreCase("gzip")) {
			inStream = new GZIPInputStream(inStream);
		}
		// ...defend against giant files
		if (MAX_DOWNLOAD>0) {
			inStream = new LimitedInputStream(inStream, MAX_DOWNLOAD);
		}
		
		// Interpret as text by default
		String type = "text";

		// NB Alas some services e.g. protx do not provide a content-type
		// header...
		if (connection.getContentType() != null) {
			type = connection.getContentType().trim().toLowerCase();
		}
		// Save to a file?
		if (saveToFile || type.startsWith("image")) { // TODO video, audio, etc
			processResponse2_binary(type, inStream);
			return null;
		}

		// Normal Page
		String response = FileUtils.read(inStream);
		if (downloadImages) {
			requestImages(connection.getURL(), response);
			requestStyles(response);
		}
		return response;
	}

	private void processResponse2_binary(String type, InputStream inStream)
			throws IOException {
		if (ignoreBinaryFiles && !saveToFile)
			return;
		// Make a file...
		String fileType = FileUtils.getType(getLocation());
		// Chop out url vars
		int i = fileType.indexOf('?');
		if (i != -1) {
			fileType = fileType.substring(0, i);
		}
		// Make safe
		fileType = FileUtils.safeFilename(fileType, false);
		// Shorten if long
		if (fileType.length() > 4) {
			fileType = fileType.substring(0, 4);
		}
		// .thing
		if ( ! fileType.startsWith(".")) fileType = "."+fileType;
		// too short? ie empty
		if (fileType.length() < 2) {
			fileType += "xx";
		}
		// Create (or use the previous/set value)
		downloadFile = userDownloadFile; 
		if (downloadFile==null) {
			downloadFile = File.createTempFile("download", fileType);
		}
		// Copy into file
		FileUtils.copy(inStream, downloadFile);
	}
	
	@Override
	public String toString() {
		return "FakeBrowser[location="+getLocation()+"]";
	}

	/**
	 * Check a response for error codes See
	 * http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html
	 * 
	 * @param connection
	 * @throws IOException
	 *             This is a {@link RedirectException} if a redirect is
	 *             requested
	 */
	private void processResponse2_errorCodes()
			throws IOException {
		// Code
		try {
			code = connection.getResponseCode();
		} catch(IOException ex) {
			if (ex.getMessage()==null || ! ex.getMessage().contains("http")) {
				// include the url
				throw new com.winterwell.utils.WrappedException("Error from "+connection.getURL().toString()+" - "+ex, ex);
			}
			throw ex; 
		}
		// Location (which can be changed by redirects)
		location = connection.getURL().toString();
		// All good
		if (code >= 200 && code < 300)
			return;
		// Process code		
		Map<String, List<String>> headers = connection.getHeaderFields();
		// Redirect
		if (code >= 300 && code < 400) {
			List<String> locns = headers.get("Location");
			if (locns!=null && locns.size() != 0 && locns.get(0)!=null)
				throw new WebEx.Redirect(code, location, locns.get(0));
			throw new IOException(code + " (redirect) "
					+ Printer.toString(headers));
		}
		String errorMessage = Printer.toString(headers.get(null));
		InputStream es = connection.getErrorStream();
		if (es != null) {
			try {
				errorPage = FileUtils.read(es);				
				// allow for quite a bit of error 'cos it can be vital for debugging
				errorMessage += StrUtils.ellipsize(WebUtils.stripTags(errorPage), 1500);
			} catch(Exception ex) {
				// ignore
			}
		}
		// Client error
		String url = connection.getURL().toString();
		if (code >= 400 && code < 500)  {			
			if (code==404) {
				throw new WebEx.E404(url, errorMessage);
			}
			if (code==403) {
				throw new WebEx.E403(url, errorMessage);
			}
			if (code==401) {
				throw new WebEx.E401(url, errorMessage);
			}	
			if (code==410) {
				throw new WebEx.E410(url, errorMessage);
			}			
			if (code==431) {
				// Request Header Fields Too Large?? What is this caused by??
				throw new WebEx.E431(StrUtils.joinWithSkip(" ", errorMessage, url, debugVars));
			}
			throw new WebEx.E40X(code, errorMessage+" "+url);
		}
		// Server error
		if (code >= 500) {			
			throw new WebEx.E50X(code, url, "(server error): " + errorMessage);
		}
		// ?
		throw new IOException(code + " (unrecognised error): " + errorMessage);
	}

	
	/**
	 * @return error-page from the last request, or null
	 * The error-page is sometimes a json blob with useful debug info.
	 */
	public String getErrorPage() {
		return errorPage;
	}
	
	private void requestImages(URL base, String response) {
		Matcher m = pSrc.matcher(response);
		while (m.find()) {
			String huh = m.group();
			String huh2 = response.substring(m.start(), m.end() + 50);
			String src = m.group(1);
			URI srcUri = WebUtils.resolveUri(base.toString(), src);
			// fetch and ignore
			try {
				getPage(srcUri.toString(), null);
			} catch (Exception e) {
				// ignore
			}
		}
	}

	private void requestStyles(String response) {
		// TODO Auto-generated method stub
		throw new TodoException();
	}

	/**
	 * Set basic authentication
	 * @param name Can be null (unusual but valid)
	 * @param password
	 * @return 
	 */
	public FakeBrowser setAuthentication(String name, String password) {
		this.name = name;
		this.password = password;
		return this;
	}

	/**
	 * Set a header for basic authentication login.
	 */
	private void setBasicAuthentication(URLConnection connection, String name,
			String password) {
		assert password != null;
		String token = (name==null? "" : name) + ":" + password;
		String encoding = Base64Encoder.encode(token);
		encoding = encoding.replace("\r\n", ""); // Patch for Java bug http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6459815
		connection.setRequestProperty("Authorization", "Basic " + encoding);
	}

	private void setCookies(URLConnection connection) {
		Map<String, String> cookies = getHostCookies(connection.getURL()
				.getHost());
		if (cookies == null)
			return;
		StringBuilder cList = new StringBuilder();
		for (Map.Entry<String,String> c : cookies.entrySet()) {
			cList.append(c.getKey());
			cList.append('=');
			cList.append(c.getValue());
			cList.append("; ");
		}
		StrUtils.pop(cList, 1);
		connection.setRequestProperty("Cookie", cList.toString());
	}

	public void setDownloadImages(boolean b) {
		downloadImages = b;
	}

	/**
	 * If true (the default) binary data is ignored. If false, data is stored in
	 * {@link #downloadFile}
	 */
	public void setIgnoreBinaryFiles(boolean b) {
		ignoreBinaryFiles = b;
	}


	/**
	 * max 10 mb by default. -1 for unlimited
	 */
	public void setMaxDownload(int mb) {
		if (mb==-1) {
			MAX_DOWNLOAD = -1; //unlimited
			return;
		}
		assert mb > 0 : mb;
		MAX_DOWNLOAD = mb * 1024L * 1024L;
	}

	/**
	 * Set the maximum amount of time to hold a connection for.
	 * 
	 * @param milliSecs
	 */
	public void setTimeOut(long milliSecs) {
		timeOutMilliSecs = (int) milliSecs;
	}
		
	Map<String,Object> reqHeaders = new ArrayMap(
			// pretend to be modernish Firefox
			"User-Agent", DEFAULT_USER_AGENT
			);

	private Proxy proxy;

	public String getUserAgent() {
		return (String) reqHeaders.get("User-Agent");
	}
	
	public FakeBrowser setUserAgent(String ua) {
		reqHeaders.put("User-Agent", ua);
		return this;
	}
	
	public void setRequestHeader(String header, Object value) {
		reqHeaders.put(header, value);
	}
	
	Cooldown cooldown;

	private int retryOnError;

	/**
	 * Create a connection and set it up (authentication, cookies) - but do not
	 * open it
	 * 
	 * @param uri
	 * @param timeOutMilliSecs
	 * @return
	 * @throws IOException
	 */
	HttpURLConnection setupConnection(String uri, int timeOutMilliSecs)
			throws IOException 
	{
		URL url = new URL(uri);
		if (cooldown!=null) {
			String host = url.getHost();
			if (cooldown.isCoolingDown(new XId(host, "domain", false))) {
				throw new WebEx.E50X(new FailureException("Pre-emptive fail: "+host+" is in Cooldown"));
			}
		}
		assert connection == null : connection.getURL();
		if (proxy==null) {
			connection = (HttpURLConnection) url.openConnection();
		} else {
			connection = (HttpURLConnection) url.openConnection(proxy);
		}
		// GET or POST
		if (requestMethod!=null) {
			connection.setRequestMethod(requestMethod);
		}
		// HACK: Disable SSL certificate/hostname checks
		if (connection instanceof HttpsURLConnection) {
			HttpsURLConnection sec = ((HttpsURLConnection) connection);
			sec.setSSLSocketFactory(INSECURE_SSL_CONTEXT.getSocketFactory());
			sec.setHostnameVerifier(new HostnameVerifier() {

				@Override
				public boolean verify(String hostname, SSLSession session) {
					return true;
				}
			});

		}		
		// Authenticate?
		if (password!=null) {
			assert password != null;
			setBasicAuthentication(connection, name, password);
		}		
		// Set outgoing request headers
		for(String h : reqHeaders.keySet()) {
			Object v = reqHeaders.get(h);
			if (v==null) continue;
			connection.setRequestProperty(h, v.toString());
		}
		connection.setDoInput(true); // we always want input?
		connection.setReadTimeout(timeOutMilliSecs);
		connection.setInstanceFollowRedirects(false); // See bug 8293 -- our own handling seems to be better
		// Set cookies!
		setCookies(connection);
		return connection;
	}

	void updateCookie(String host, String cookie) {
		// TODO handle cookie expiry
		Matcher m = keyValue.matcher(cookie);
		boolean ok = m.find();
		if (!ok)
			return;
		String k = m.group(1);
		String v = m.group(2);
		setCookie(host, k, v);
	}
	
	/**
	 * Set a "fake" cookie 
	 * @param host
	 * @param cookieName
	 * @param cookieValue
	 */
	public void setCookie(String host, String cookieName, Object cookieValue) {
		Map<String, String> hostJar = cookieJar.get(host);
		if (hostJar==null) {
			hostJar = new HashMap();
			cookieJar.put(host, hostJar);
		}
		if (cookieValue==null || cookieValue.toString().isEmpty()) {
			hostJar.remove(cookieName);
		} else {
			hostJar.put(cookieName, String.valueOf(cookieValue));
		}
	}

	private void updateCookies() {
		String host = connection.getURL().getHost();
		Map<String, List<String>> headers = connection.getHeaderFields();
		List<String> cookies = headers.get("Set-Cookie");
		if (cookies == null)
			return;
		for (String cookie : cookies) {
			updateCookie(host, cookie);
		}
	}

	public void setSaveToFile(File file) {
		userDownloadFile = file;
	}

	/**
	 * If true, output all get urls to Log
	 * @param debug
	 * @return this
	 */
	public FakeBrowser setDebug(boolean debug) {
		this.debug = debug;
		return this;
	}

	/**
	 * Set the request method. This will override the normal use of GET or POST, 
	 * for all requests until it is reset to null.
	 * @param method Can be null for default behaviour.
	 * @return this
	 */
	public FakeBrowser setRequestMethod(String method) {
		this.requestMethod = method;
		return this;
	}

	/**
	 * 
	 * @param proxy Can be null for no-proxy
	 */
	public void setProxy(Proxy proxy) {
		this.proxy = proxy;
	}

	/**
	 * 
	 * @param proxyIP Can be null for no-proxy
	 * @param port
	 */
	public void setProxy(String proxyIP, int port) {
		if (proxyIP==null) {
			proxy = null; 
			return;
		}
		Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyIP, port));
		setProxy(proxy);
	}

	public void setCooldown(Cooldown cooldown) {
		this.cooldown = cooldown;
	}

	public Cooldown getCooldown() {
		return cooldown;
	}

	/**
	 * @param token
	 * See https://en.wikipedia.org/wiki/JSON_Web_Token#Use
	 * @return 
	 */
	public FakeBrowser setAuthenticationByJWT(String token) {		
		setRequestHeader("Authorization", "Bearer "+token);
		return this;
	}

	public void setRetryOnError(int retries) {
		this.retryOnError = retries;
	}

	
}

