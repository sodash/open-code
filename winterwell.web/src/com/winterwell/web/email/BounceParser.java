package com.winterwell.web.email;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import com.winterwell.utils.Utils;

import com.winterwell.web.ExternalServiceException;

/**
 * Make sense of bounce messages (aka DSN, Delivery Status Notification, messages).
 * 
 * Use {@link #isBounce()} to see if it is a bounce. Note that not all bounces are permanent failures.
 * 
 * TODO
 * 	https://api.metacpan.org/source/RJBS/Mail-DeliveryStatus-BounceParser-1.540/lib/Mail/DeliveryStatus/BounceParser.pm
 * 
 * Refs:
 * http://stackoverflow.com/questions/5298285/detecting-if-an-email-is-a-delivery-status-notification-and-extract-informatio
 * @author daniel
 *
 */
public class BounceParser {

	private MimeMessage msg;
	private String[] recipients;
	private String messageID;
	private boolean dsn;
	private Object status;
	private KBounceReason reason;
	private boolean permanent;
	private String details;

	/**
	 * @return true if this is a permanent failure
	 * false does not neccesarrily mean anything.
	 */
	public boolean isPermanent() {
		return permanent;
	}
	/**
	 * 
	 * @param msg Can be any email message (use {@link #isBounce()} to see if it is a bounce)/
	 */
	public BounceParser(MimeMessage msg) {
		this.msg = msg;
		try {
			parse();
		} catch (Exception e) {
			throw Utils.runtime(e);
		}
	}
	
	void parse() throws MessagingException, IOException {
		// gmail?
		// See 			http://stackoverflow.com/questions/5298285/detecting-if-an-email-is-a-delivery-status-notification-and-extract-informatio
		// gmail ignores the dsn rfc and sends these
		String[] failed = msg.getHeader("X-Failed-Recipients");
		if (failed!=null && failed.length!=0) {
			recipients = failed;
			dsn = true;
			parseGmail();
		}
		// TODO standards compliant!
		String ct = msg.getContentType();
		if (ct.contains("delivery-status")) {
			dsn=true;
		}
	}

	static Pattern msgIdHeader = Pattern.compile("Message-ID:\\s?(.+)");
	static Pattern PATTERN_USER_UKNOWN = Pattern.compile("\\b550\\b.+mailbox\\b");
	
	private void parseGmail() throws IOException, MessagingException {
		// Look for the message ID
		String text = msg.getContent().toString();
		Matcher m = msgIdHeader.matcher(text);
		boolean ok = m.find();
		if (ok) {
			String mid = m.group(1);
			messageID = mid.trim();
		}
		// Can we find a reason code?
		m = PATTERN_USER_UKNOWN.matcher(text);
		ok = m.find();
		if (ok) {
			reason = KBounceReason.user_unknown;
			permanent = true;
		}
		// details
		details = text;
	}
	
	public String[] getFailedRecipients() {
		return recipients;
	}
	
	public String getMessageID() {
		return messageID;
	}

	public KBounceReason getReason() {
		return reason;
	}
	
	public static enum KBounceReason {
		user_unknown,
		over_quota,
		user_disabled,
		domain_error,
		spam,
		message_too_large,
		unknown
	}
	
	/**
	 * @return true if it is a DSN / bounce email.
	 * Note not all bounces are permanent failures!
	 */
	public boolean isBounce() {
		return dsn;
	}

	public Object getStatus() {
		return status;
	}
	public String getDetails() {
		return details;
	}

}
