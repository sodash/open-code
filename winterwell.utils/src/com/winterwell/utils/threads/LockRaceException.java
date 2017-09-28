package com.winterwell.utils.threads;

public class LockRaceException extends Exception {
	
	private static final long serialVersionUID = 1L;

	public LockRaceException(String string) {
		super(string);
	}

}
