package winterwell.utils;

import com.winterwell.utils.ReflectionUtils;

/**
 * Exception indicating that although your request was reasonable,
 * <i>something</i> has gone wrong, often with a 3rd party system.
 * 
 * @author daniel
 */
public class FailureException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String source;

	public FailureException() {
		super();
	}

	public FailureException(Throwable e) {
		super(e);
	}

	public FailureException(String msg) {
		super(msg + " @" + ReflectionUtils.getCaller());
	}

	public FailureException(String msg, Exception cause) {
		super(msg + " @" + ReflectionUtils.getCaller(), cause);
	}

	public FailureException(String source, String msg) {
		super(source + ": " + msg + " @" + ReflectionUtils.getCaller());
		this.source = source;
	}

	public String getSource() {
		return source;
	}

	/**
	 * Convenience to throw a FailureException 
	 * @param ex Can be null. Will select the appropriate constructor based on the object type. 
	 * If ex is a FailureException it is thrown as-is. 
	 */
	public static void fail(Object ex) throws FailureException {
		if (ex==null) throw new FailureException();
		if (ex instanceof FailureException) throw (FailureException) ex;
		if (ex instanceof Throwable) {
			throw new FailureException((Throwable)ex);
		}
		throw new FailureException(ex.toString());
	}

}
