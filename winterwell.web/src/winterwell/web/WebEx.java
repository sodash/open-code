package winterwell.web;


@Deprecated /** use the com.winterwell version */
public class WebEx extends com.winterwell.web.WebEx {
	private static final long serialVersionUID = 1L;

	public WebEx(int code, String msg) {
		super(code, msg);		
	}
	
	public WebEx(int code, String msg, Throwable e) {
		super(code, msg, e);		
	}

	/**
	 * It's YOUR fault dear visitor
	 */
	public static class E40X extends com.winterwell.web.WebEx.E40X {
		public E40X(int code, String url) {
			super(code, url);
		}

		public E40X(int code, String msg, Exception e) {
			super(code, msg, e);
		}		
	}
	
	/**
	 * Forbidden
	 */
	@Deprecated
	public static class E403 extends E40X {
		public E403(String url) {
			super(403, url);
		}
		private static final long serialVersionUID = 1L;
	}

	@Deprecated
	public static class E404 extends E40X {
		public E404(String url) {
			super(404, url);
		}
		private static final long serialVersionUID = 1L;
	}

	/**
	 * Treat the (rare) 410 as a subclass of the common 404
	 * @author daniel
	 *
	 */
	@Deprecated
	public static class E410 extends E404 {
		public E410(String url) {
			super(url);
		}
		private static final long serialVersionUID = 1L;
	}
	
	/**
	 * Server error
	 */
	public static class E50X extends com.winterwell.web.WebEx.E50X {
		/**
		 * Wrap another exception to mark it as a 50X in disguise.
		 * @param ex
		 */
		public E50X(Exception ex) {
			super(ex);
		}
		public E50X(int code, String url) {
			super(code, url);
		}
		private static final long serialVersionUID = 1L;
	}


	public static final class Redirect extends com.winterwell.web.WebEx.Redirect {
		private static final long serialVersionUID = 1L;

		public Redirect(int code, String from, String to) {
			super(code, from, to);
		}
	}

}
