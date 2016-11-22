package winterwell.web;


@Deprecated /** use the com.winterwell version */
public class ExternalServiceException extends com.winterwell.web.ExternalServiceException {

	
	/** @deprecated use the com.winterwell version 
	 * For security problems.
	 * 
	 * @author daniel
	 */
	public static class Security extends ExternalServiceException {
		private static final long serialVersionUID = 1L;
	}
	
	
	@Deprecated /** use the com.winterwell version */
	public static class Networking extends ExternalServiceException {
		private static final long serialVersionUID = 1L;
		public Networking(String string, Throwable cause) {
			super(string,cause);
		}

	}

	private static final long serialVersionUID = 42L;

	public ExternalServiceException() {
		this("");
	}

	public ExternalServiceException(Throwable e) {
		super(e);
	}

	public ExternalServiceException(String msg) {
		super(msg);
	}

	public ExternalServiceException(String msg, Exception e) {
		super(msg, e);
	}
	
	public ExternalServiceException(String msg, Throwable e) {
		super(msg, e);
	}

	

}
