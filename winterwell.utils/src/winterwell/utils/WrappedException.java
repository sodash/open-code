package winterwell.utils;

import java.io.PrintStream;
import java.io.PrintWriter;

/**
 * @deprecated Use the com.winterwell version
 * For wrapping other exceptions to convert them into {@link RuntimeException}s.
 * The stacktrace will be that of the original exception.
 * 
 * @author daniel
 * @testedby {@link WrappedExceptionTest}
 */
public class WrappedException extends com.winterwell.utils.WrappedException {

	private static final long serialVersionUID = 1L;

	public WrappedException(String msg, Throwable e) {
		super(msg, e);
		// assert ! (e instanceof WrappedException) : e; TODO
	}

	public WrappedException(Throwable e) {
		super(e);
		// assert ! (e instanceof WrappedException) : e; TODO
	}

}
