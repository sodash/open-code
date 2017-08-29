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

	@Option
	public int loginTimeout = 30;

	@Option
	public int connectTimeout = 60;

	@Option
	public int socketTimeout = 600;

	
	@Override
	public String toString() {
		return getClass().getSimpleName()+"["+dbUser+"@"+dbUrl+"]";
	}
}