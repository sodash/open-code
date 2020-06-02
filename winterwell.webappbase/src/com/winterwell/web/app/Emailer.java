package com.winterwell.web.app;

import java.io.Closeable;

import javax.mail.internet.InternetAddress;

import com.winterwell.utils.Key;
import com.winterwell.utils.StrUtils;
import com.winterwell.utils.Utils;
import com.winterwell.utils.io.FileUtils;
import com.winterwell.utils.log.Log;
import com.winterwell.web.ConfigException;
import com.winterwell.web.LoginDetails;
import com.winterwell.web.email.EmailConfig;
import com.winterwell.web.email.SMTPClient;
import com.winterwell.web.email.SimpleMessage;

/**
 * Send emails by SMTP, using a bot/admin email account.
 * @testedby {@link EmailerTest}
 * @author daniel
 *
 */
public class Emailer implements Closeable {

	@Override
	public String toString() {
		return "Emailer["+config+"]";
	}
	
	private EmailConfig config;
	private String displayName;

//	private String app;
	
	public Emailer(EmailConfig config) {
		this.config = config;
		LoginDetails ld = config.getLoginDetails();		
		this.displayName = (String) Utils.or(
			ld.get(new Key("displayName")),
			ld.get(new Key("displayname")),
			! Utils.isBlank(AMain.appName)? StrUtils.toTitleCase(AMain.appName)+" Notifications" : null,
			ld.loginName
		);				
	}
	
	
	private boolean closed;
	private SMTPClient smtpClient;

	public InternetAddress getFrom() {
		try {
			InternetAddress addr = new InternetAddress(config.emailFrom, displayName);
			return addr;
		} catch (Exception e) {
			throw Utils.runtime(e);
		}
	}
	
	/**
	 * aka getFrom()
	 * sodash_notifications@sodash.net
	 * Email address to use for automated site email. This is also
	 * the login for the bot user, who performs other site actions
	 * which don't require the powerful privileges of admin.
	 */
	public InternetAddress getBotEmail() {
		try {
			InternetAddress addr = new InternetAddress(config.emailFrom,					 
					StrUtils.toTitleCase(AMain.appName)+" Notifications");
			return addr;
		} catch (Exception e) {
			throw Utils.runtime(e);
		}
	}
	

	/**
	 * 
	 * @return
	 * @throws ConfigException
	 *             if imap is not configured
	 */
	private SMTPClient getSMTPClient() throws ConfigException {
		assert ! closed : "email client has been closed";
		if (smtpClient != null)
			return smtpClient;
		smtpClient = new SMTPClient(config);		
		return smtpClient;
	}

	
	/**
	 * This is the base send method.
	 * @param email
	 * @return 
	 */
	public boolean send(SimpleMessage email) {		
		SMTPClient client = getSMTPClient();
		assert client != null : "no SMTP client to send "+email;
		Log.d("Email", "Send to "+SimpleMessage.getTos(email)+"\t"+StrUtils.ellipsize(StrUtils.compactWhitespace(email.toString()), 100)
				+"\tsmtp:"+client.getLoginDetails());
		client.send(email);
		return true;
	}

	
	@Override
	public void close() {
		FileUtils.close(smtpClient);
		smtpClient = null;
		closed = true;
	}
	
	@Override
	protected void finalize() throws Throwable {		
		if (smtpClient!=null) {
			Log.e("email", this+" was not closed!");
			close();						
		}
		super.finalize();
	}

}
