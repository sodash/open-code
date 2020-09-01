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

	/**
	 * 
	 * @param msg This gets joined to the cause's message, so you can add in extra info.
	 * @param e
	 */
	public WrappedException(String msg, Throwable e) {
		super(msg, e);
	}

	public WrappedException(Throwable e) {
		this(e.getMessage(), e);
	}
	
	@Override
	public String getMessage() {
		String sm = super.getMessage();
		Throwable c = getCause();
		if (c==this) return sm;
		return sm+" (wraps) "+c.getMessage();
	}

	/**
	 * The original Throwable
	 */
	@Override
	public final Throwable getCause() {
		Throwable ex = super.getCause();
		if (ex==null) {
			return this; // Huh - nothing was wrapped? See ESException which does this.
		}
		// have we double wrapped something?
		if (ex instanceof WrappedException) {
			return ex.getCause();
		}
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
		if (e==null || e==this) return super.toString(); // paranoia
		if (super.getMessage() != e.getMessage()) {
			return super.getMessage() + " (wraps) " + e.toString();
		}
		return "(wrapped) " + e.toString();
	}
}
