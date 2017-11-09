package com.winterwell.youagain.client;

public class LoginFailedException extends SecurityException {
	public LoginFailedException() {		
	}
	public LoginFailedException(String string) {
		super(string);
	}

	private static final long serialVersionUID = 1L;

}
