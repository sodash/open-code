/**
 * 
 */
package com.winterwell.utils;

/**
 * A lighter form of exception. Something is bad -- but it can probably be ignored.
 * @author daniel
 */
public class Warning extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public Warning(String msg) {
		super(msg);
	}

}
