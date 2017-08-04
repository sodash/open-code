package com.winterwell.web.app;

import java.io.Closeable;
import java.io.File;
import java.util.Date;
import java.util.Properties;

import javax.mail.internet.InternetAddress;

import com.sun.mail.imap.IMAPFolder;
import com.winterwell.utils.Dep;
import com.winterwell.utils.Key;
import com.winterwell.utils.StrUtils;
import com.winterwell.utils.Utils;
import com.winterwell.utils.io.FileUtils;
import com.winterwell.utils.log.Log;
import com.winterwell.web.ConfigException;
import com.winterwell.web.LoginDetails;
import com.winterwell.web.data.XId;
import com.winterwell.web.email.SMTPClient;
import com.winterwell.web.email.SimpleMessage;

/**
 * Copy pasta from YouAgainEmailer
 * @author daniel
 *
 */
public class HackyEmailer implements Closeable {

	
	private static boolean initFlag;
	private static LoginDetails sin;
	private static String from;

	public static void init() {
		if (initFlag) return;
		initFlag = true;
		Properties props = FileUtils.loadProperties(new File(FileUtils.getWinterwellDir(), "open-code/winterwell.webappbase/local-config/local.properties"));
		LoginDetails ld = new LoginDetails(props.getProperty("server").trim(), 
				props.getProperty("from").trim(), props.getProperty("password").trim(), 25);
		ld.put(SMTPClient.USE_SSL, false);
		XId xid = new XId(ld.loginName, "email");
		sin = ld;
		from = sin.loginName;
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
