package com.winterwell.web;

import winterwell.utils.StrUtils;
import creole.data.XId;

public class WebEx extends RuntimeException {
	private static final long serialVersionUID = 1L;
	/** HTTP code, e.g. 404 for file not found */
	public final int code;

	public WebEx(int code, String msg) {
		super(code+": "+msg);		
		this.code = code;
	}
	
	public WebEx(int code, String msg, Throwable e) {
		super(code+": "+msg, e);		
		this.code = code;
	}

	/**
	 * It's YOUR fault dear visitor
	 */
	public static class E40X extends WebEx {
		private static final long serialVersionUID = 1L;

		public E40X(int code, String url) {
			super(code, url);
		}

		public E40X(int code, String url, String msg) {
			super(code, msg==null? url : url+"\t"+msg);
		}
		
		public E40X(int code, String msg, Exception e) {
			super(code, msg, e);
		}		
	}
	
	public static class RateLimitException extends E40X {
		private static final long serialVersionUID = 1L;
		public RateLimitException(XId user, String msg) {
			super(400, StrUtils.joinWithSkip(" ", user,  msg));
		}
	}
	
	/**
	 * Forbidden
	 */
	public static class E403 extends E40X {
		public E403(String url) {
			super(403, url);
		}
		public E403(String url, String msg) {
			super(403, url, msg);
		}
		private static final long serialVersionUID = 1L;
	}

	
	/**
	 * 401: Unauthorised. The request requires user authentication, and none or invalid was given. 
	 */
	public static class E401 extends E40X {
		public E401(String url) {
			super(401, url);
		}
		public E401(String url, String msg) {
			super(401, url, msg);
		}
		private static final long serialVersionUID = 1L;
	}
	
	public static class E404 extends E40X {
		public E404(String url) {
			super(404, url);
		}
		public E404(String url, String msg) {
			super(404, url, msg);
		}
		private static final long serialVersionUID = 1L;
	}
	
	/**
	 * The server cannot or will not process the request due to something that is perceived to be a client error 
	 * (e.g., malformed request syntax, invalid request message framing, or deceptive request routing).
	 */
	public static class E400 extends E40X {
		public E400(String msg) {
			super(400, null, msg);
		}		
		private static final long serialVersionUID = 1L;
	}

	/**
	 * Treat the (rare) 410 as a subclass of the common 404
	 * @author daniel
	 *
	 */
	public static class E410 extends E404 {
		public E410(String url) {
			super(url);
		}
		public E410(String url, String msg) {
			super(url, msg);
		}
		private static final long serialVersionUID = 1L;
	}
	
	/**
	 * Server error
	 */
	public static class E50X extends WebEx {
		/**
		 * Wrap another exception to mark it as a 50X in disguise.
		 * @param ex
		 */
		public E50X(Exception ex) {
			super(500, ex.getMessage(), ex);
		}
		public E50X(int code, String url) {
			super(code, url);
		}
		private static final long serialVersionUID = 1L;
	}


	public static class Redirect extends WebEx {
		private static final long serialVersionUID = 1L;
		public final String to;
	
		public Redirect(int code, String from, String to) {
			super(code, from+" -> "+to);
			this.to = to==null? null : to.trim();
		}
	}


	/**
	 * Note: FakeBrowser has separate and better error handling (which draws on more info).
	 * @param code
	 * @param url
	 * @return
	 */
	public static Exception fromErrorCode(int code, String url) {
		// Not an error?!
		if (code<300) return null;
		if (code>=300 && code <400) {
			return new WebEx.Redirect(code, url, null);
		}
		switch(code) {
		case 401: return new WebEx.E401(url);
		case 403: return new WebEx.E403(url);
		case 404: return new WebEx.E404(url);
		case 410: return new WebEx.E410(url);
		}
		if (code>=400 && code <500) return new WebEx.E40X(code, url);
		if (code>=500) return new WebEx.E50X(code, url);
		return null;
	}

}
