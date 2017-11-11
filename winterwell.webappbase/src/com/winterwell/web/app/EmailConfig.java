package com.winterwell.web.app;

import com.winterwell.utils.io.Option;
import com.winterwell.web.LoginDetails;
import com.winterwell.web.email.SMTPClient;

public class EmailConfig {

	@Option
	String emailServer; 
	
	@Option
	String emailFrom;
	
	@Option
	String emailPassword;
	
	@Option
	int emailPort;
	
	@Option
	boolean emailSSL;
	
	public LoginDetails getLoginDetails() {
		LoginDetails ld = new LoginDetails(emailServer, emailFrom, emailPassword, emailPort);
		ld.put(SMTPClient.USE_SSL, emailSSL);
		return ld;
	}

}
