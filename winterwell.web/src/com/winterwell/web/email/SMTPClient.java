package com.winterwell.web.email;

import java.io.Closeable;
import java.util.Date;
import java.util.Properties;
import java.util.logging.Level;

import javax.mail.Address;
import javax.mail.Authenticator;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.MimeMessage;

import com.winterwell.utils.Key;
import com.winterwell.utils.ReflectionUtils;
import com.winterwell.utils.Utils;
import com.winterwell.utils.io.FileUtils;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.time.Time;
import com.winterwell.web.ExternalServiceException;
import com.winterwell.web.LoginDetails;

/**
 * SimpleAuthenticator is used to do simple authentication when the SMTP server
 * requires it.
 */
final class SMTPAuthenticator extends javax.mail.Authenticator {

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

/**
 * Send emails via SMTP. 
 * 
 * See:
 * https://javaee.github.io/javamail/docs/api/com/sun/mail/smtp/package-summary.html
 * 
 * <p>
 * Weird behaviour: if multiple Session objects are created, some
 * Session.getInstance() requests may fail. I think either Session.getInstance()
 * or a lower level system is doing some caching in a bad way. The code
 * currently works but is a bit slow. TODO Caching that works would be nice
 * 
 * @author daniel. Uses free code supplied by Sudhir Ancha
 * @testedby  SMTPClientTestRemote}
 */
public class SMTPClient implements Closeable {

	/**
	 * 1 minute time out. TODO How does this work when handling large emails?
	 */
	private static final int TIMEOUT_MILLISECS = 60 * 1000;

	/**
	 * Key for use in {@link LoginDetails} to specify the use of smtps and ssl.
	 * The default is to use smtp without ssl.
	 */
	public static final Key<Boolean> USE_SSL = new Key<Boolean>(
			"SMTPClient.ssl");

	private IMAPClient imapClient;

	private final LoginDetails loginDetails;

	/**
	 * smtp by default. Can also be smtps
	 */
	private String protocol = "smtp";

	private String sentFolderName;

	private Session smtpSession;

	private EmailConfig config;

	public SMTPClient(EmailConfig config) {
		this.loginDetails = config.getLoginDetails();
		this.config = config;
	}

	public SMTPClient(LoginDetails ld) {
		this(configFromLoginDetails(ld));
	}

	private static EmailConfig configFromLoginDetails(LoginDetails ld) {
		EmailConfig ec = new EmailConfig();
		ec.emailPort = ld.port;
		ec.emailFrom = ld.loginName;
		ec.emailPassword = ld.password;
		ec.emailServer = ld.server;
		// copy properties, like SSL or plaintext (SMTPClient.USE_SSL)?
		for(Key k : ld.getKeys()) {
			Object v = ld.get(k);
			try {				
				ReflectionUtils.setPrivateField(ec, k.name, v);
			} catch(Throwable ex) {
				Log.e("smtp", "Unable to pass on LoginDetails prop "+k+"="+v+" to EmailConfig");
			}
		}

		return ec;
	}

	/**
	 * You should call this when done to release any resources (eg. the IMAP
	 * folder for sent mail, if set).
	 */
	@Override
	public void close() {
		if (imapClient != null) {
			imapClient.close();
		}
		imapClient = null;
		smtpSession = null;
	}

	private void connect() {
		Properties props = System.getProperties();
		// see
		// http://java.sun.com/products/javamail/javadocs/com/sun/mail/smtp/package-summary.html
		// for some not very good documentation on this not very good interface
		Boolean ssl = loginDetails.get(USE_SSL);
		if (ssl != null && ssl) {
			protocol = "smtps";
			props.put("mail.transport.protocol", protocol);			
		}
		// default is infinite timeout - let's not do that
		// set connection and IO timeouts
		props.put("mail." + protocol + ".connectiontimeout", TIMEOUT_MILLISECS);
		props.put("mail." + protocol + ".timeout", TIMEOUT_MILLISECS);
		// -- Attaching to default Session, or we could start a new one --
		props.put("mail." + protocol + ".host", loginDetails.server);
		if (Utils.yes(config.starttls)) {
			props.put("mail." + protocol + ".starttls.enable", "true");
		}
		int port = loginDetails.port;
		if (port != 0) {
			props.put("mail." + protocol + ".port", port);
		}
		// ??would this make things more robust?
		// c.f. http://www.cosmocode.de/en/blogs/detman/20070301091006/
		// if (loginDetails.server.equals("localhost")) {
		// props.put("mail.smtp.localhost", "localhost");
		// }
		Authenticator auth = null;
		if (loginDetails.loginName != null && loginDetails.password != null) {
			// ??What should we do when loginDetails.password == null??
			props.put("mail." + protocol + ".auth", "true");
			// Get smtp session
			auth = new SMTPAuthenticator(loginDetails.loginName,
					loginDetails.password);
		} else {
			props.put("mail." + protocol + ".auth", "false");
		}
		smtpSession = Session.getInstance(props, auth);
	}

	@Override
	protected void finalize() throws Throwable {
		try {
			close();
		} catch (Throwable e) {
		}
		super.finalize();
	}

	/**
	 * @return IMAP client for copy-to-sent behaviour if set. null by default.
	 */
	public IMAPClient getImapClient() {
		return imapClient;
	}

	public LoginDetails getLoginDetails() {
		return loginDetails;
	}

	/**
	 * @return the SMTP session. Never null. Calling this can trigger a
	 *         connection to be opened.
	 */
	public Session getSession() {
		if (smtpSession == null) {
			connect();
		}
		return smtpSession;
	}

	public boolean isConnected() {
		return smtpSession != null;
	}

	/**
	 * "send" method to send the message.
	 * <p>
	 * Note: if email does not use this client's session object, we
	 * (temporarily) reset the session object.
	 * @throws IllegalArgumentException if there are no recipients.
	 */
	public void send(Message email) {
		if (!isConnected()) {
			connect();
		}
		try {
			Log.d("smtp", "Sending "+email.getSubject()+" to "+SimpleMessage.getRecipients(email, null));
		} catch (Exception ex) {
			// oh well
		}
		try {
			assert smtpSession != null;
			SimpleMessage msg = email instanceof SimpleMessage ? (SimpleMessage) email
					: new SimpleMessage((MimeMessage) email);
			// -- Send the message using this session
			Session popSession = msg.getSession();
			msg.setSession(smtpSession);

			// Who to?
			Address[] recipients = msg.getAllRecipients();
			if (recipients.length==0) {
				throw new IllegalArgumentException("Email has no recipients: "+email.getSubject());
			}
			
			// ??It might be more efficient to cache a Transport object
			// This will get an smtp Transport, connect to it, send the message,
			// then close it
			Transport transport = smtpSession.getTransport(protocol);
			// msg.saveChanges(); is this needed/wanted?
			try {
				transport.connect(); // SMTP_HOST_NAME, SMTP_HOST_PORT,
										// SMTP_AUTH_USER, SMTP_AUTH_PWD);
				transport.sendMessage(msg, recipients);
			} finally {
				transport.close();
			}

			msg.setSession(popSession);
			// Logging switched off! (it's mostly noise around exceptions in the SoDash logs)
//			String to = SimpleMessage.getRecipients(email, Message.RecipientType.TO);
//			Log.d("smtp", "Message "+ email.getSubject()+ " sent OK to "+to);
			// Copy to sent?
			if (imapClient != null) {
				send2_copyToSentFolder(email);
			}
			// make sure we have a sent-date (fixes a "bug" seen in SoDash) 
			if (msg.getSentDate()==null) msg.setSentDate(new Time().getDate());
		} catch (MessagingException ex) {
			throw new ExternalServiceException(ex);
		}
	}

	private void send2_copyToSentFolder(Message email)
			throws MessagingException {
		// sent date doesn't get set by the smtp transport - set it here
		if (email.getSentDate() == null) {
			email.setSentDate(new Date());
		}
		// make sure we're in the sent folder
		Folder folder = imapClient.getOpenFolder();
		if (folder == null || ! sentFolderName.equals(folder.getName())) {
			imapClient.openFolder(sentFolderName);
		}
		// add email
		// TODO remove-tracking-pixel
		imapClient.add(email);		
		Log.d("smtp", "Copied email "+email.getSubject()+" to sent folder "+sentFolderName+" via "+imapClient);
	}

	/**
	 * Setup "copy to sent folder" behaviour (off by default).
	 * 
	 * @param imapClient
	 *            This will have it's folders manipulated - sent will get opened
	 *            whenever a message is sent. And it will be closed when the
	 *            SMTP client closes. Can be null to switch off.
	 * @param folderName
	 *            Uses the lenient folder opening policy of
	 *            {@link IMAPClient#openFolder(String)}
	 */
	public void setSentFolder(IMAPClient imapClient, String folderName) {
		if (imapClient==null) {
			FileUtils.close(imapClient);
			imapClient = null;
			return;
		}
		Utils.check4null(imapClient, folderName);
		sentFolderName = folderName;
		imapClient.openFolder(folderName); // This checks the folder exists
		if (!folderName.toLowerCase().contains("sent")) {
			Log.report("Saving sent mail to " + folderName
					+ ". Is this correct?", Level.WARNING);
		}
		this.imapClient = imapClient;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + ":" + loginDetails.loginName;
	}

}
