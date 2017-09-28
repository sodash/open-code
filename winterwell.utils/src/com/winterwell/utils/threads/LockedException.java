package com.winterwell.utils.threads;

public class LockedException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public LockedException(Lock lock) {
		super("Lock "+lock.slug);
	}

}