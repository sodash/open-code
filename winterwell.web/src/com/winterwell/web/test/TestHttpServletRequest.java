/**
 *
 */
package com.winterwell.web.test;

import java.io.BufferedReader;
import java.io.IOException;
import java.security.Principal;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

import javax.servlet.AsyncContext;
import javax.servlet.DispatcherType;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpUpgradeHandler;
import javax.servlet.http.Part;

import com.winterwell.utils.ReflectionUtils;
import com.winterwell.utils.TodoException;
import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.utils.log.Log;

/**
 * @author Joe Halliwell <joe@winterwell.com>
 * 
 */
public class TestHttpServletRequest implements HttpServletRequest {

	private String contentType;
	private Map<String, String> parameters;
	private HttpSession session;
	
	static int throttleCnt;

	public TestHttpServletRequest() {
		this(new ArrayMap(0));
	}

	public TestHttpServletRequest(Map<String, String> parameters) {
		this.parameters = parameters;
	}

	public void setParameters (Map<String, String> parameters){
		this.parameters = parameters;
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.servlet.ServletRequest#getAttribute(java.lang.String)
	 */
	@Override
	public Object getAttribute(String name) {
		Logd("TestHttpServletRequest", "method not implemented "+ReflectionUtils.stacktrace());		
		return null;
	}

	private void Logd(String tag, String msg) {
		if (throttleCnt > 20) return;
		Log.d(tag, msg);
		throttleCnt++;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.servlet.ServletRequest#getAttributeNames()
	 */
	@Override
	public Enumeration getAttributeNames() {
		Logd("TestHttpServletRequest", "method not implemented "+ReflectionUtils.stacktrace());
		return null;
	}

	@Override
	public String getAuthType() {
		Logd("TestHttpServletRequest", "method not implemented"+ReflectionUtils.stacktrace());
		return null;
	}

	@Override
	public String getCharacterEncoding() {
		Logd("TestHttpServletRequest", "method not implemented"+ReflectionUtils.stacktrace());
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.servlet.ServletRequest#getContentLength()
	 */
	@Override
	public int getContentLength() {
		Logd("TestHttpServletRequest", "method not implemented "+ReflectionUtils.stacktrace());
		return 0;
	}

	@Override
	public String getContentType() {
		return contentType;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.servlet.http.HttpServletRequest#getContextPath()
	 */
	@Override
	public String getContextPath() {
		Logd("TestHttpServletRequest", "method getContextPath not implemented");
		return "Bah";
	}

	Cookie[] cookies;
	/**
	 * dummy uri -- null by default
	 */
	private String uri;
	
	@Override
	public Cookie[] getCookies() {
		return cookies;
	}
	
	public void setCookies(Cookie[] cookies) {
		this.cookies = cookies;
	}

	@Override
	public long getDateHeader(String name) {
		Logd("TestHttpServletRequest", "method not implemented "+ReflectionUtils.stacktrace());
		return 0;
	}
	
	@Override
	public String getHeader(String name) {
		return headers.get(name);
	}

	@Override
	public Enumeration getHeaderNames() {
		Logd("TestHttpServletRequest", "method not implemented "+ReflectionUtils.stacktrace());
		return null;
	}

	@Override
	public Enumeration getHeaders(String name) {
		Logd("TestHttpServletRequest", "method not implemented "+ReflectionUtils.stacktrace());
		return null;
	}


	@Override
	public ServletInputStream getInputStream() throws IOException {
		Logd("TestHttpServletRequest", "method not implemented "+ReflectionUtils.stacktrace());
		throw new TodoException();
	}

	@Override
	public int getIntHeader(String name) {
		Logd("TestHttpServletRequest", "method not implemented "+ReflectionUtils.stacktrace());
		return 0;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.servlet.ServletRequest#getLocalAddr()
	 */
	@Override
	public String getLocalAddr() {
		Logd("TestHttpServletRequest", "method not implemented "+ReflectionUtils.stacktrace());
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.servlet.ServletRequest#getLocale()
	 */
	@Override
	public Locale getLocale() {
		Logd("TestHttpServletRequest", "method not implemented "+ReflectionUtils.stacktrace());
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.servlet.ServletRequest#getLocales()
	 */
	@Override
	public Enumeration getLocales() {
		Logd("TestHttpServletRequest", "method not implemented "+ReflectionUtils.stacktrace());
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.servlet.ServletRequest#getLocalName()
	 */
	@Override
	public String getLocalName() {
		Logd("TestHttpServletRequest", "method not implemented "+ReflectionUtils.stacktrace());
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.servlet.ServletRequest#getLocalPort()
	 */
	@Override
	public int getLocalPort() {
		Logd("TestHttpServletRequest", "method not implemented "+ReflectionUtils.stacktrace());
		return 0;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.servlet.http.HttpServletRequest#getMethod()
	 */
	@Override
	public String getMethod() {
		Logd("TestHttpServletRequest", "method not implemented "+ReflectionUtils.stacktrace());
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.servlet.ServletRequest#getParameter(java.lang.String)
	 */
	@Override
	public String getParameter(String name) {
		return parameters.get(name);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.servlet.ServletRequest#getParameterMap()
	 */
	@Override
	public Map getParameterMap() {
		return Collections.unmodifiableMap(parameters);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.servlet.ServletRequest#getParameterNames()
	 */
	@Override
	public Enumeration getParameterNames() {
		final Iterator<String> it = parameters.keySet().iterator();
		return new Enumeration() {
			@Override
			public boolean hasMoreElements() {
				return it.hasNext();
			}

			@Override
			public Object nextElement() {
				return it.next();
			}
		};
	}

	@Override
	public String[] getParameterValues(String name) {
		Logd("TestHttpServletRequest", "method not implemented getParameterValues for "+name);
		Object v = getParameterMap().get(name);
		return v==null? null : new String[]{v.toString()};
	}

	@Override
	public String getPathInfo() {
	//	Logd("TestHttpServletRequest", "method not implemented "+ReflectionUtils.stacktrace());
		return ("");

	}

	@Override
	public String getPathTranslated() {
		Logd("TestHttpServletRequest", "method not implemented "+ReflectionUtils.stacktrace());
		return null;
	}

	@Override
	public String getProtocol() {
		Logd("TestHttpServletRequest", "method not implemented "+ReflectionUtils.stacktrace());
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.servlet.http.HttpServletRequest#getQueryString()
	 */
	@Override
	public String getQueryString() {
		Logd("TestHttpServletRequest", "method not implemented "+ReflectionUtils.stacktrace());
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.servlet.ServletRequest#getReader()
	 */
	@Override
	public BufferedReader getReader() throws IOException {
		Logd("TestHttpServletRequest", "method not implemented "+ReflectionUtils.stacktrace());
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.servlet.ServletRequest#getRealPath(java.lang.String)
	 */
	@Override
	public String getRealPath(String path) {
		Logd("TestHttpServletRequest", "method not implemented "+ReflectionUtils.stacktrace());
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.servlet.ServletRequest#getRemoteAddr()
	 */
	@Override
	public String getRemoteAddr() {
		Logd("TestHttpServletRequest", "method not implemented "+ReflectionUtils.stacktrace());
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.servlet.ServletRequest#getRemoteHost()
	 */
	@Override
	public String getRemoteHost() {
		Logd("TestHttpServletRequest", "method not implemented "+ReflectionUtils.stacktrace());
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.servlet.ServletRequest#getRemotePort()
	 */
	@Override
	public int getRemotePort() {
		Logd("TestHttpServletRequest", "method not implemented "+ReflectionUtils.stacktrace());
		return 0;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.servlet.http.HttpServletRequest#getRemoteUser()
	 */
	@Override
	public String getRemoteUser() {
		Logd("TestHttpServletRequest", "method not implemented "+ReflectionUtils.stacktrace());
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.servlet.ServletRequest#getRequestDispatcher(java.lang.String)
	 */
	@Override
	public RequestDispatcher getRequestDispatcher(String path) {
		Logd("TestHttpServletRequest", "method not implemented "+ReflectionUtils.stacktrace());
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.servlet.http.HttpServletRequest#getRequestedSessionId()
	 */
	@Override
	public String getRequestedSessionId() {
		Logd("TestHttpServletRequest", "method not implemented "+ReflectionUtils.stacktrace());
		return null;
	}
	
	@Override
	public String getRequestURI() {
		return uri;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.servlet.http.HttpServletRequest#getRequestURL()
	 */
	@Override
	public StringBuffer getRequestURL() {
		Logd("TestHttpServletRequest", "method not implemented "+ReflectionUtils.stacktrace());
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.servlet.ServletRequest#getScheme()
	 */
	@Override
	public String getScheme() {
		Logd("TestHttpServletRequest", "method not implemented "+ReflectionUtils.stacktrace());
		return null;
	}

	@Override
	public String getServerName() {
		Logd("TestHttpServletRequest", "method not implemented "+ReflectionUtils.stacktrace());
		return null;
	}

	@Override
	public int getServerPort() {
		Logd("TestHttpServletRequest", "method not implemented "+ReflectionUtils.stacktrace());
		return 0;
	}

	@Override
	public String getServletPath() {
		Logd("TestHttpServletRequest", "method not implemented "+ReflectionUtils.stacktrace());
		return null;
	}

	@Override
	public HttpSession getSession() {
		if (session == null) {
			setSession(new TestHttpSession());
		}
		return session;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.servlet.http.HttpServletRequest#getSession(boolean)
	 */
	@Override
	public HttpSession getSession(boolean create) {
		Logd("TestHttpServletRequest", "method not implemented "+ReflectionUtils.stacktrace());
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.servlet.http.HttpServletRequest#getUserPrincipal()
	 */
	@Override
	public Principal getUserPrincipal() {
		Logd("TestHttpServletRequest", "method not implemented "+ReflectionUtils.stacktrace());
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * javax.servlet.http.HttpServletRequest#isRequestedSessionIdFromCookie()
	 */
	@Override
	public boolean isRequestedSessionIdFromCookie() {
		Logd("TestHttpServletRequest", "method not implemented "+ReflectionUtils.stacktrace());
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.servlet.http.HttpServletRequest#isRequestedSessionIdFromUrl()
	 */
	@Override
	public boolean isRequestedSessionIdFromUrl() {
		Logd("TestHttpServletRequest", "method not implemented "+ReflectionUtils.stacktrace());
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.servlet.http.HttpServletRequest#isRequestedSessionIdFromURL()
	 */
	@Override
	public boolean isRequestedSessionIdFromURL() {
		Logd("TestHttpServletRequest", "method not implemented "+ReflectionUtils.stacktrace());
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.servlet.http.HttpServletRequest#isRequestedSessionIdValid()
	 */
	@Override
	public boolean isRequestedSessionIdValid() {
		Logd("TestHttpServletRequest", "method not implemented "+ReflectionUtils.stacktrace());
		return false;
	}

	@Override
	public boolean isSecure() {
		Logd("TestHttpServletRequest", "method not implemented "+ReflectionUtils.stacktrace());
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.servlet.http.HttpServletRequest#isUserInRole(java.lang.String)
	 */
	@Override
	public boolean isUserInRole(String role) {
		Logd("TestHttpServletRequest", "method not implemented "+ReflectionUtils.stacktrace());
		return false;
	}

	@Override
	public void removeAttribute(String name) {
		Logd("TestHttpServletRequest", "method not implemented "+ReflectionUtils.stacktrace());
	}

	@Override
	public void setAttribute(String name, Object o) {
		Logd("TestHttpServletRequest", "method not implemented "+ReflectionUtils.stacktrace());
	}

	@Override
	public void setCharacterEncoding(String env) {		
	}

	public TestHttpServletRequest setContentType(String mimeType) {
		this.contentType = mimeType;
		return this;
	}

	private void setSession(HttpSession testSession) {
		this.session = testSession;
	}

	@Override
	public AsyncContext getAsyncContext() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DispatcherType getDispatcherType() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ServletContext getServletContext() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isAsyncStarted() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isAsyncSupported() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public AsyncContext startAsync() throws IllegalStateException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public AsyncContext startAsync(ServletRequest arg0, ServletResponse arg1)
			throws IllegalStateException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean authenticate(HttpServletResponse arg0) throws IOException,
			ServletException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Part getPart(String arg0) throws IOException, ServletException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<Part> getParts() throws IOException, ServletException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void login(String arg0, String arg1) throws ServletException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void logout() throws ServletException {
		// TODO Auto-generated method stub
		
	}

	public void setHeader(String name, String val) {
		headers.put(name, val);
	}
	final Map<String,String> headers = new HashMap();

	@Override
	public long getContentLengthLong() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String changeSessionId() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T extends HttpUpgradeHandler> T upgrade(Class<T> arg0) throws IOException, ServletException {
		// TODO Auto-generated method stub
		return null;
	}

}
