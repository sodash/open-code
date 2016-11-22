/**
 *
 */
package winterwell.web.test;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collection;
import java.util.Locale;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import winterwell.utils.reporting.Log;
import winterwell.web.app.TestHttpServletResponseTest;

/**
 * Use {@link #getOutputBufferContents()} to see what got "sent" out.
 * 
 * @author Joe Halliwell <joe@winterwell.com>
 * @testedby {@link TestHttpServletResponseTest}
 */
public class TestHttpServletResponse implements HttpServletResponse {

	String contentType;
	StringBuilder sb;
	private StringWriter writer;

	/**
	 * @param type
	 */
	public TestHttpServletResponse() {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * javax.servlet.http.HttpServletResponse#addCookie(javax.servlet.http.Cookie
	 * )
	 */
	@Override
	public void addCookie(Cookie cookie) {
		Log.report("TestHttpServletResponse method not implemented");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * javax.servlet.http.HttpServletResponse#addDateHeader(java.lang.String,
	 * long)
	 */
	@Override
	public void addDateHeader(String name, long date) {
		Log.report("TestHttpServletResponse method not implemented");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.servlet.http.HttpServletResponse#addHeader(java.lang.String,
	 * java.lang.String)
	 */
	@Override
	public void addHeader(String name, String value) {
		Log.report("TestHttpServletResponse method not implemented");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * javax.servlet.http.HttpServletResponse#addIntHeader(java.lang.String,
	 * int)
	 */
	@Override
	public void addIntHeader(String name, int value) {
		Log.report("TestHttpServletResponse method not implemented");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * javax.servlet.http.HttpServletResponse#containsHeader(java.lang.String)
	 */
	@Override
	public boolean containsHeader(String name) {
		Log.report("TestHttpServletResponse method not implemented");
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * javax.servlet.http.HttpServletResponse#encodeRedirectUrl(java.lang.String
	 * )
	 */
	@Override
	public String encodeRedirectUrl(String url) {
		Log.report("TestHttpServletResponse method not implemented");
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * javax.servlet.http.HttpServletResponse#encodeRedirectURL(java.lang.String
	 * )
	 */
	@Override
	public String encodeRedirectURL(String url) {
		Log.report("TestHttpServletResponse method not implemented");
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.servlet.http.HttpServletResponse#encodeUrl(java.lang.String)
	 */
	@Override
	public String encodeUrl(String url) {
		Log.report("TestHttpServletResponse method not implemented");
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.servlet.http.HttpServletResponse#encodeURL(java.lang.String)
	 */
	@Override
	public String encodeURL(String url) {
		Log.report("TestHttpServletResponse method not implemented");
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.servlet.ServletResponse#flushBuffer()
	 */
	@Override
	public void flushBuffer() throws IOException {
		Log.report("TestHttpServletResponse method not implemented");

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.servlet.ServletResponse#getBufferSize()
	 */
	@Override
	public int getBufferSize() {
		Log.report("TestHttpServletResponse method not implemented");
		return 0;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.servlet.ServletResponse#getCharacterEncoding()
	 */
	@Override
	public String getCharacterEncoding() {
		Log.report("TestHttpServletResponse method not implemented");
		return null;
	}

	@Override
	public String getContentType() {
		return contentType;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.servlet.ServletResponse#getLocale()
	 */
	@Override
	public Locale getLocale() {
		Log.report("TestHttpServletResponse method not implemented");
		return null;
	}

	/**
	 * Retrieve the current contents of the output buffer.
	 * 
	 * @return
	 */
	public String getOutputBufferContents() {
		return sb.toString();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.servlet.ServletResponse#getOutputStream()
	 */
	@Override
	public ServletOutputStream getOutputStream() throws IOException {
		Log.report("Using OutputStream");
		assert writer == null;
		sb = new StringBuilder();
		return new ServletOutputStream() {
			@Override
			public void print(String s) throws IOException {
				sb.append(s);
			}

			@Override
			public void write(int b) throws IOException {
				// this mangles non-ASCII!
				sb.append((char) b);
			}
		};
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.servlet.ServletResponse#getWriter()
	 */
	@Override
	public PrintWriter getWriter() throws IOException {
		Log.report("Using PrintWriter");
		assert sb == null;
		writer = new StringWriter();
		PrintWriter pw = new PrintWriter(writer);
		return pw;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.servlet.ServletResponse#isCommitted()
	 */
	@Override
	public boolean isCommitted() {
		Log.report("TestHttpServletResponse method not implemented");
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.servlet.ServletResponse#reset()
	 */
	@Override
	public void reset() {
		Log.report("TestHttpServletResponse method not implemented");

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.servlet.ServletResponse#resetBuffer()
	 */
	@Override
	public void resetBuffer() {
		Log.report("TestHttpServletResponse method not implemented");

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.servlet.http.HttpServletResponse#sendError(int)
	 */
	@Override
	public void sendError(int sc) throws IOException {
		Log.report("TestHttpServletResponse method not implemented");

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.servlet.http.HttpServletResponse#sendError(int,
	 * java.lang.String)
	 */
	@Override
	public void sendError(int sc, String msg) throws IOException {
		Log.report("TestHttpServletResponse method not implemented");

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * javax.servlet.http.HttpServletResponse#sendRedirect(java.lang.String)
	 */
	@Override
	public void sendRedirect(String location) throws IOException {
		Log.report("TestHttpServletResponse method not implemented");

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.servlet.ServletResponse#setBufferSize(int)
	 */
	@Override
	public void setBufferSize(int size) {
		Log.report("TestHttpServletResponse method not implemented");

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.servlet.ServletResponse#setCharacterEncoding(java.lang.String)
	 */
	@Override
	public void setCharacterEncoding(String charset) {
		Log.report("TestHttpServletResponse method not implemented");

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.servlet.ServletResponse#setContentLength(int)
	 */
	@Override
	public void setContentLength(int len) {
		Log.report("TestHttpServletResponse method not implemented");

	}

	@Override
	public void setContentType(String type) {
		contentType = type;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * javax.servlet.http.HttpServletResponse#setDateHeader(java.lang.String,
	 * long)
	 */
	@Override
	public void setDateHeader(String name, long date) {
		Log.report("TestHttpServletResponse method not implemented");

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.servlet.http.HttpServletResponse#setHeader(java.lang.String,
	 * java.lang.String)
	 */
	@Override
	public void setHeader(String name, String value) {
		Log.report("TestHttpServletResponse method not implemented");

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * javax.servlet.http.HttpServletResponse#setIntHeader(java.lang.String,
	 * int)
	 */
	@Override
	public void setIntHeader(String name, int value) {
		Log.report("TestHttpServletResponse method not implemented");

	}

	@Override
	public void setLocale(Locale loc) {
		Log.report("TestHttpServletResponse method not implemented");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.servlet.http.HttpServletResponse#setStatus(int)
	 */
	@Override
	public void setStatus(int sc) {
		Log.report("TestHttpServletResponse method not implemented");

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.servlet.http.HttpServletResponse#setStatus(int,
	 * java.lang.String)
	 */
	@Override
	public void setStatus(int sc, String sm) {
		Log.report("TestHttpServletResponse method not implemented");

	}

	@Override
	public String getHeader(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<String> getHeaderNames() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<String> getHeaders(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getStatus() {
		// TODO Auto-generated method stub
		return 0;
	}

}
