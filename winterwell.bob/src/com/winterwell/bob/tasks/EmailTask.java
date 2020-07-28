package com.winterwell.bob.tasks;

import java.util.Date;

import javax.mail.PasswordAuthentication;

import com.winterwell.bob.BuildTask;
import com.winterwell.utils.Utils;
import com.winterwell.utils.io.FileUtils;
import com.winterwell.utils.log.Log;
import com.winterwell.web.LoginDetails;
import com.winterwell.web.email.SMTPClient;
import com.winterwell.web.email.SimpleMessage;

/**
 * TODO replace with the more advanced code in HackyEmailer
 * 
 * @author daniel. Uses free code supplied by Sudhir Ancha
 * 
 */
public class EmailTask extends BuildTask {

	private final String sender;
	LoginDetails smtpServer;
	private final String subject;
	private final String text;
	private final String to;
	private transient SMTPClient smtp;

	public EmailTask(String to, String subject, String text, String sender,
			LoginDetails smtpServer) {
		Utils.check4null(smtpServer, to, subject, text, sender, smtpServer);
		this.smtpServer = smtpServer;
		this.to = to;
		this.text = text;
		this.sender = sender;
		this.subject = subject;
	}

	@Override
	public void doTask() throws Exception {
		smtp = new SMTPClient(smtpServer);
		send(to, sender, subject, text);
	}

	@Override
	public void close() {
		FileUtils.close(smtp);
	}
	
	/**
	 * "send" method to send the message.
	 */
	private void send(String to, String from, String subject, String body) {
		try {
			// -- Create a new message --
			SimpleMessage msg = new SimpleMessage(from, to, subject, body);
			// -- Set some other header information --
			msg.setHeader("X-Mailer", "WinterwellEmail");
			msg.setSentDate(new Date());
			// -- Send the message --
			smtp.send(msg);
		} catch (Exception ex) {
			Log.w(LOGTAG, ex);
		}
	}

}

/**
 * SimpleAuthenticator is used to do simple authentication when the SMTP server
 * requires it.
 */
class SMTPAuthenticator extends javax.mail.Authenticator {

	private final String password;
	private final String userName;

	public SMTPAuthenticator(String userName, String password) {
		this.userName = userName;
		this.password = password;
	}

	@Override
	public PasswordAuthentication getPasswordAuthentication() {
		return new PasswordAuthentication(userName, password);
	}
}
