package com.winterwell.utils;

import java.io.PrintStream;
import java.io.PrintWriter;

/**
 * For wrapping other exceptions to convert them into {@link RuntimeException}s.
 * The stacktrace will be that of the original exception.
 * 
 * @author daniel
 * @testedby {@link WrappedExceptionTest}
 */
public class WrappedException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public WrappedException(String msg, Throwable e) {
		super(msg, e);
		// assert ! (e instanceof WrappedException) : e; TODO
	}

	public WrappedException(Throwable e) {
		super(e);
		// assert ! (e instanceof WrappedException) : e; TODO
	}

	/**
	 * The original Throwable
	 */
	@Override
	public final Throwable getCause() {
		Throwable ex = super.getCause();
		return ex;
	}

	@Override
	public final StackTraceElement[] getStackTrace() {
		return getCause().getStackTrace();
	}

	@Override
	public void printStackTrace(PrintStream s) {
		getCause().printStackTrace();
	}

	@Override
	public void printStackTrace(PrintWriter s) {
		getCause().printStackTrace();
	}

	/**
	 * Hide the WrappedException bit - show the underlying exception
	 */
	@Override
	public String toString() {
		Throwable e = getCause();
		return e == null ? super.toString() : "(wrapped)"
				+ getCause().toString();
	}
}
