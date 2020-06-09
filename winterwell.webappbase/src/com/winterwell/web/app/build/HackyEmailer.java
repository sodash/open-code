package com.winterwell.web.app.build;

import java.io.Closeable;
import java.io.File;
import java.util.Properties;

import com.winterwell.utils.StrUtils;
import com.winterwell.utils.io.FileUtils;
import com.winterwell.utils.log.Log;
import com.winterwell.web.ConfigException;
import com.winterwell.web.app.Emailer;
import com.winterwell.web.email.EmailConfig;
import com.winterwell.web.email.SMTPClient;
import com.winterwell.web.email.SimpleMessage;

/**
 * TODO replace with {@link Emailer}
 * Copy pasta from YouAgainEmailer
 * @author daniel
 *
 */
public class HackyEmailer implements Closeable {

	
	private static boolean initFlag;
	private static EmailConfig sin;
	private static String from;

	public static void init() {
		if (initFlag) return;
		initFlag = true;
		File propsFile = new File(FileUtils.getWinterwellDir(), "open-code/winterwell.webappbase/local-config/local.properties");
		if ( ! propsFile.exists()) {
			propsFile = new File(FileUtils.getWinterwellDir(), "logins/local.properties");
			if ( ! propsFile.exists()) {
				System.out.println("Please make a file with email login details here: "+propsFile);
				throw new ConfigException("Please symlink the logins/local.properties file or make a file with email login details here: "+propsFile+".");
			}
		}
		Properties props = FileUtils.loadProperties(propsFile);				
		sin = new EmailConfig();
		sin.emailServer = props.getProperty("server").trim();
		sin.emailFrom = props.getProperty("from").trim();
		sin.emailPassword = props.getProperty("password").trim();
		sin.emailPort = 25;
		sin.emailSSL = false;		
		from = sin.emailFrom;
	}
	
	public HackyEmailer() {
	}
	
	
	private boolean closed;
	private SMTPClient smtpClient;

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
		smtpClient = new SMTPClient(sin);		
		return smtpClient;
	}

	
	/**
	 * This is the base send method.
	 * @param email
	 */
	public void send(SimpleMessage email) {		
		SMTPClient client = getSMTPClient();
		assert client != null : "no SMTP client to send "+email;
		Log.d("Email", "Send to "+SimpleMessage.getTos(email)+"\t"+StrUtils.ellipsize(StrUtils.compactWhitespace(email.toString()), 100)
				+"\tsmtp:"+client.getLoginDetails());
		client.send(email);		
	}

	
	@Override
	public void close() {
		FileUtils.close(smtpClient);
		smtpClient = null;
		closed = true;
	}
	
	// Newly added post the Small Town crash - maybe remove for bug hunting?
	@Override
	protected void finalize() throws Throwable {		
		if (smtpClient!=null) {
			Log.e("email", this+" was not closed!");
			close();						
		}
		super.finalize();
	}

	public String getFrom() {
		return from;
	}

}
