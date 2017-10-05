package com.winterwell.utils.threads;

import com.winterwell.utils.FailureException;
import com.winterwell.utils.time.Dt;

/**
 * Thrown if {@link Lock#getLock(IHasId, Dt)} times-out without getting the lock.
 */
public class LockOutFailureException extends FailureException {
	public LockOutFailureException(String msg) {
		super(msg);
	}

	private static final long serialVersionUID = 1L;
}