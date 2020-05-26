package com.winterwell.web.app;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import org.junit.Test;

import com.winterwell.web.LoginDetails;
import com.winterwell.web.email.EmailConfig;
import com.winterwell.web.email.SimpleMessage;

/**
 * @author daniel
 *
 */
public class EmailerTest {

	@Test
	public void testSendAnEmail() throws AddressException {
		EmailConfig ec = AppUtils.getConfig("test", new EmailConfig(), null);		
		Emailer e = new Emailer(ec);		
		InternetAddress to = new InternetAddress("daniel@winterwell.com");
		SimpleMessage email = new SimpleMessage(new InternetAddress(ec.emailFrom), to, "Test hello from Emailer", 
				"Hello Daniel :)");
		e.send(email);
	}
}
