package com.winterwell.web.email;

import com.winterwell.utils.io.Option;
import com.winterwell.web.LoginDetails;

public class EmailConfig {

	@Option
	public String emailServer; 
	
	@Option
	public String emailFrom;
	
	@Option
	public String emailPassword;
	
	@Option
	public Boolean starttls;
	
	@Option
	public int emailPort;
	
	@Option
	public boolean emailSSL;
	
	public LoginDetails getLoginDetails() {
		if (emailServer==null) return null;
		LoginDetails ld = new LoginDetails(emailServer, emailFrom, emailPassword, emailPort);
		ld.put(SMTPClient.USE_SSL, emailSSL);
		return ld;
	}

	@Override
	public String toString() {
		return "EmailConfig [emailServer=" + emailServer + ", emailFrom=" + emailFrom + ", emailPort=" + emailPort
				+ ", emailSSL=" + emailSSL + "]";
	}

}
