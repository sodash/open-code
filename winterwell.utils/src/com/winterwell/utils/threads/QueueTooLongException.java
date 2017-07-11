package com.winterwell.utils.threads;

import com.winterwell.utils.FailureException;

public class QueueTooLongException extends FailureException {
	public QueueTooLongException(String string) {
		super(string);
	}

	private static final long serialVersionUID = 1L;
	
}