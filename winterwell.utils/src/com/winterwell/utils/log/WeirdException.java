/**
 * 
 */
package com.winterwell.utils.log;

/**
 * An exception for when unexplained things happen.
 * 
 * @author daniel
 * 
 */
public class WeirdException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public WeirdException(String msg) {
		super(msg);
	}
}
