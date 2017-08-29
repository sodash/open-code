package com.winterwell.utils.io;

public class DBOptions {
	@Option
	public String dbUrl;
	@Option
	public String dbUser;
	@Option
	public String dbPassword;		
	
	@Option
	public Boolean ssl;
	@Option
	public String sslkey;
	@Option
	public String sslcert;
	
	@Override
	public String toString() {
		return getClass().getSimpleName()+"["+dbUser+"@"+dbUrl+"]";
	}
}