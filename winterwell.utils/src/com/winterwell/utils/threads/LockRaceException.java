package com.winterwell.utils.threads;

public class LockRaceException extends RuntimeException {
	
	private static final long serialVersionUID = 1L;

	public LockRaceException(String string) {
		super(string);
	}

}
