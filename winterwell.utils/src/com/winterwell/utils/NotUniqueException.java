package com.winterwell.utils;

/**
 * Exception for when one unique result was expected, but multiple results were
 * found.
 * 
 * @author daniel
 */
public class NotUniqueException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public NotUniqueException() {
		//
	}

	public NotUniqueException(String msg) {
		super(msg);
	}

}
