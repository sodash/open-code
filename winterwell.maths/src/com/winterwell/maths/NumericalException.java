package com.winterwell.maths;

import com.winterwell.utils.FailureException;

/**
 * Indicates an error due to a numerical problem. This could be an infinity, a
 * NaN, or other problems.
 * 
 * @author daniel
 * 
 */
public class NumericalException extends FailureException {
	private static final long serialVersionUID = 1L;

	public NumericalException() {
		super("");
	}

}
