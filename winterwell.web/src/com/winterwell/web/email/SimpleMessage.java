/**
 * 
 */
package com.winterwell.web.email;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.Header;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import com.winterwell.utils.StrUtils;
import com.winterwell.utils.Utils;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.time.Time;
import com.winterwell.utils.web.WebUtils;

import com.winterwell.utils.io.FileUtils;
import com.winterwell.utils.web.WebUtils2;
import com.winterwell.web.ExternalServiceException;

/**
 * A Message that doesn't throw exceptions everywhere and has some convenience
 * methods.
 * <p>
 * This is designed to be used with {@link SMTPClient} and {@link IMAPClient}.
 * 
 * @author daniel
 * @testedby {@link SimpleMessageTest}
 */
public class SimpleMessage extends MimeMessage {

	public Date getReceivedDate() throws MessagingException {
		if (original!=null) {
			Date rd = original.getReceivedDate();
			if (rd!=null) return rd;
		}
		String[] receiveds = getHeader("Received");
		// assume first is the most recent
		if (receiveds==null || receiveds.length==0) {
			return null; // Odd!
		}
		String r0 = receiveds[0];
		String[] split = r0.split(";");
		if (split.length==0) {
			return null; // Odd!
		}
		String timestamp = split[split.length-1].trim();
		try {
			Time t = new Time(timestamp);
			return t.getDate();
		} catch(Exception ex) {
			return null;
		}
	}
	
	@Override
	public Address[] getAllRecipients() {
		try {
			return super.getAllRecipients();
		} catch (Exception ex) {
			// TODO be more robust about bad formatting
			Log.w("SimpleMessage", ex);
			try {
				String[] hTo = getHeader("To");
				String[] hCC = getHeader("Cc");			
				ArrayList<Address> list = new ArrayList();
				if (hTo!=null) {
					for(String a : hTo) {					
						Matcher m = EMAIL_REGEX2.matcher(a);
						while(m.find()) {
							javax.mail.internet.InternetAddress ia = new InternetAddress(m.group());
							list.add(ia);
						}
					}
				}
				if (hCC!=null) {
					for(String a : hCC) {
						Matcher m = EMAIL_REGEX2.matcher(a);
						while(m.find()) {
							javax.mail.internet.InternetAddress ia = new InternetAddress(m.group());
							list.add(ia);
						}
					}
				}
				return list.toArray(new Address[0]);
			} catch(MessagingException ex2) {
				// oh well
				throw Utils.runtime(ex);
			}
		}
	}
	
	
	/**
	 * @deprecated EMAIL_REGEX2 is probably better.
	 * Does NOT include a name part, e.g. "Bob &lt;bob@eg.com&gt;" will fail.
	 * Only matches on complete strings. ??Is that best??
	 */
	public static final Pattern EMAIL_REGEX = Pattern
			.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,4}$");
	
	/**
	 * Like {@link #EMAIL_REGEX} but matches words within strings
	 */
	public static final Pattern EMAIL_REGEX2 = Pattern
			.compile("\\b[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,4}\\b");
	
	// What moron thought it a good idea to have two classes called
	// RecipientType?
	private static final Message.RecipientType[] RECIPIENT_TYPES = new Message.RecipientType[] {
			Message.RecipientType.TO, Message.RecipientType.CC,
			Message.RecipientType.BCC };


	/**
	 * Ugly little convenience to convert the constructor's checked exception
	 * (which we can't catch) into an unchecked one. Will throw
	 * {@link ClassCastException} if msg is not a {@link MimeMessage}.
	 * If msg is already a SimpleMessage, then it be returned as-is.
	 * 
	 * @param msg
	 * @return
	 */
	public static SimpleMessage create(Message msg) {
		if (msg instanceof SimpleMessage) return (SimpleMessage) msg;
		try {
			return new SimpleMessage((MimeMessage) msg);
		} catch (MessagingException e) {
			throw new ExternalServiceException(e);
		}
	}

	/**
	 * Convenience constructor for replying to an email
	 * 
	 * @param incoming
	 * @param body
	 *            The reply to send. This is used as-is; we don't auto-include a
	 *            quoted version of the incoming message
	 * @return
	 */
	public static SimpleMessage createReply(Message incoming,
			InternetAddress replier, String body) {
		try {
			InternetAddress to = (InternetAddress) incoming.getReplyTo()[0];
			SimpleMessage msg = new SimpleMessage(replier, to, "Re: "
					+ incoming.getSubject(), body);
			msg.setInReplyTo(incoming);
			return msg;
		} catch (MessagingException e) {
			throw new ExternalServiceException(e);
		}
	}

	public static String getBCCs(Message message) {
		return getRecipients(message, javax.mail.Message.RecipientType.TO);
	}

	public static String getCCs(Message message) {
		return getRecipients(message, javax.mail.Message.RecipientType.TO);
	}

	/**
	 * Converts to String. Is this needed?
	 * 
	 * @param message
	 * @param rt
	 * @return
	 */
	public static String getRecipients(Message message, Message.RecipientType rt) {
		if (rt==null) {
			// null => Get them all
			StringBuilder s = new StringBuilder();
			for(Message.RecipientType _rt : new Message.RecipientType[]{
					javax.mail.Message.RecipientType.TO, 
					javax.mail.Message.RecipientType.CC,
					javax.mail.Message.RecipientType.BCC
			}) {
				String rs = getRecipients(message, _rt);
				if (rs.length()==0) continue;
				s.append(rs); s.append(',');
			}
			if (s.length()!=0) StrUtils.pop(s, 1);
			return s.toString();
		}
		
		try {
			Address[] mTos = message.getRecipients(rt);
			if (mTos == null || mTos.length == 0)
				return "";
			StringBuilder recipients = new StringBuilder();
			for (Address address : mTos) {
				recipients.append(WebUtils2.canonicalEmail(address));
				recipients.append(",");
			}
			// remove the trailing ,
			recipients.deleteCharAt(recipients.length() - 1);
			return recipients.toString();
		} catch (MessagingException e) {
			throw new ExternalServiceException(e);
		}
	}

	public static String getTos(Message message) {
		return getRecipients(message, javax.mail.Message.RecipientType.TO);
	}

	/**
	 * Needed to delete messages from IMAP servers.
	 */
	private Message original;

	/**
	 * @param from
	 *            Cannot be null
	 * @param to
	 * @param subject
	 * @param body
	 */
	public SimpleMessage(InternetAddress from, InternetAddress to,
			String subject, String body) {
		// use a null session
		super((Session) null);
		assert from != null;
		try {
			this.setFrom(from);
			this.setRecipient(javax.mail.Message.RecipientType.TO, to);
			this.setSubject(subject, "UTF-8");
			this.setText(body, "UTF-8");
		} catch (Exception e) {
			throw new ExternalServiceException(e);
		}
	}
	
	/**
	 * Sets up MIME multipart/alternative message with HTML and plaintext fallback
	 * @param from
	 * @param to
	 * @param subject
	 * @param bodyPlain
	 * @param bodyHtml
	 */
	public SimpleMessage(InternetAddress from, InternetAddress to,
			String subject, String bodyPlain, String bodyHtml) {
		this(from, to, subject, bodyPlain);
		try {
			MimeBodyPart plainTextPart = new MimeBodyPart();
			plainTextPart.setText(bodyPlain, "utf-8", "plain");
			MimeBodyPart htmlTextPart = new MimeBodyPart();
			htmlTextPart.setText(bodyHtml, "utf-8", "html");
			Multipart multiPart = new MimeMultipart("alternative");
			multiPart.addBodyPart(plainTextPart);
			multiPart.addBodyPart(htmlTextPart);
			this.setContent(multiPart);
		} catch (MessagingException e) {
			throw new ExternalServiceException(e);
		}
	}
	
	/**
	 * Create a blank message -- use the setX methods to build it up before sending.
	 * @param from
	 *            Cannot be null
	 */
	public SimpleMessage(InternetAddress from) {
		// use a null session
		super((Session) null);
		assert from != null;
		try {
			this.setFrom(from);		
		} catch (Exception e) {
			throw new ExternalServiceException(e);
		}
	}

	public SimpleMessage(MimeMessage msg) throws MessagingException {
		super(msg);
		original = msg;
	}

	@Override
	public void setText(String text) {
		try {
			super.setText(text);
		} catch (MessagingException e) {
			throw Utils.runtime(e);
		}
	}
	
	/**
	 * Convenience for
	 * {@link #SimpleMessage(InternetAddress, InternetAddress, String, String)}
	 * 
	 * @param from
	 * @param to
	 * @param subject
	 * @param body
	 */
	public SimpleMessage(String from, String to, String subject, String body) {
		this(WebUtils2.internetAddress(from), WebUtils2.internetAddress(to), subject, body);
	}

	private void addAttachment(BodyPart part) throws MessagingException,
			IOException {
		// convert to multipart if need be
		Object cntent = getContent();
		Multipart mp;
		if (cntent instanceof String) {
			mp = new MimeMultipart();
			setContent(mp);
			// create and fill the first message part
			MimeBodyPart mbp1 = new MimeBodyPart();
			mbp1.setText((String) cntent);
			mp.addBodyPart(mbp1);
		} else {
			mp = (Multipart) cntent;
		}
		// create and fill the second message part
		// create the Multipart and its parts to it
		mp.addBodyPart(part);
		//
		String ct = getContentType();
		assert !ct.startsWith("text") : ct;
	}
	
	@Override
	public void setContent(Multipart mp) throws MessagingException {
		super.setContent(mp);
		// workaround for a bug in java mail:
		// if we re-set the content, the type does not update.
		setHeader("Content-Type", mp.getContentType());
	}

	/**
	 * TODO test this
	 * 
	 * @param attachment
	 */
	public void addAttachment(File attachment) {
		try {
			MimeBodyPart mbp2 = addAttachment2_makePart(attachment);
			addAttachment(mbp2);
		} catch (Exception ex) {
			throw Utils.runtime(ex);
		}
	}

	private MimeBodyPart addAttachment2_makePart(File attachment)
			throws MessagingException {
		MimeBodyPart mbp2 = new MimeBodyPart();
		mbp2.setFileName(attachment.getName());
		DataSource src = new FileDataSource(attachment);
		DataHandler dataHandler = new DataHandler(src);
		mbp2.setDataHandler(dataHandler);
		// another little bug in mail's handling of content types
		String partType = src.getContentType();
		mbp2.setHeader("Content-Type", partType);

		String ct2 = dataHandler.getContentType();
		String ct3 = mbp2.getContentType();
		return mbp2;
	}

	public void addBCC(String string) {
		try {
			addRecipient(javax.mail.Message.RecipientType.BCC,
					new InternetAddress(string));
		} catch (AddressException e) {
			throw new IllegalArgumentException(e);
		} catch (MessagingException e) {
			throw new ExternalServiceException(e);
		}
	}

	public void addCC(String string) {
		try {
			addRecipient(javax.mail.Message.RecipientType.CC,
					new InternetAddress(string));
		} catch (AddressException e) {
			throw new IllegalArgumentException(e);
		} catch (MessagingException e) {
			throw new ExternalServiceException(e);
		}
	}

	public void addTo(InternetAddress to) {
		try {
			addRecipient(javax.mail.Message.RecipientType.TO, to);
		} catch (MessagingException e) {
			throw new ExternalServiceException(e);
		}
	}


	/**
	 * Create a forward for this email. This will copy the body and any
	 * attachments.
	 * 
	 * @param sendAs
	 *            Who to send the forward as. Can be null for
	 *            "send as the original sender".
	 * @param prefix
	 *            E.g. "Fwd: " Can be blank, must not be null
	 * @param fwdTo
	 * @param fwdHeaderInline
	 *            If true, add a short header summary above the forwarded text
	 * @return
	 */
	public SimpleMessage createForward(InternetAddress sendAs, String prefix,
			InternetAddress fwdTo, boolean fwdHeaderInline) {
		assert prefix != null;
		String body = getBodyText();
		if (sendAs == null) {
			sendAs = getSender();
		}
		if (fwdHeaderInline) {
			body = "\n-------- Original Message --------\n" + "Subject: "
					+ getSubject() + "\n" + "From: " + getSender() + "\n"
					+ "Date: " + getSentDate() + "\n"
					// +"Reply-To: "+getReplyTo()[0]+"\n"
					// +"To/CC: "+getAllRecipients()+"\n\n"
					+ "\n" + body;
		}
		SimpleMessage fwd = new SimpleMessage(sendAs, fwdTo, prefix
				+ getSubject(), body);
		List<Part> attachments = getAttachments();
		try {
			for (Part part : attachments) {
				fwd.addAttachment((BodyPart) part);
			}
		} catch (Exception e) {
			throw new ExternalServiceException(e);
		}
		return fwd;
	}

	/**
	 * Return a text/plain version of the first part of a multipart message. If
	 * there is a text/plain version of the body, this is it. If there is only a
	 * text/html version, tags are stripped. Returns null if the message is
	 * empty.
	 * 
	 * @param preferHtml If true, prefer html parts and return unstripped html.
	 * 
	 * @throws MessagingException
	 * @throws IOException
	 */
	private String getBodyText3_firstTextPart(Multipart multipart, boolean preferHtml)
			throws MessagingException, IOException {
		// empty?
		if (multipart.getCount() == 0)
			return null;
		List<Part> parts = new ArrayList();
		for(int i=0; i<multipart.getCount(); i++) {
			Part part = multipart.getBodyPart(i);
			parts.add(part);
		}
		// preference (plain text or html) if we can
		for (Part part : parts) {
			String type = part.getContentType();
			
			// NB: Inline Images have type image/
//			Log.d("email", "has-part "+type+" msg-id:"+getMessageID());			
			
			if (type.startsWith("text/plain") && ! preferHtml) {
				return part.getContent().toString();
			}
			if (preferHtml && type.startsWith("text/html")) {
				String html = part.getContent().toString();
				return html;
			}
		}
		// Fallback
		for (Part part : parts) {
			String type = part.getContentType();
			if (type.startsWith("text/html")) {
				String html = part.getContent().toString();
				return preferHtml? html : WebUtils.stripTags(html);
			}
			if (type.startsWith("text")) {
				Log.w("email", "Unusual text-part type: "+type);
				return part.getContent().toString();
			}
		}	
		// recurse?	
		for (Part part : parts) {
			String type = part.getContentType();
			if (type.startsWith("multipart")) {
				Object pc = part.getContent(); 
				if (pc instanceof Multipart) {
					String bodyText = getBodyText3_firstTextPart((Multipart) pc, preferHtml);
					if ( ! Utils.isBlank(bodyText)) return bodyText;
				}				
			}
		}
		// fail :(
		Log.w("email", "Failed to find body text in "+getSubject());
		return null;
	}

	/**
	 * Get a list of attachments to the message
	 * 
	 * @return a list of the attachments. May be empty, never null.
	 * @testedby {@link SimpleMessageTest#testGetAttachments1()}
	 * @testedby {@link SimpleMessageTest#testGetAttachments2()}
	 * @testedby {@link SimpleMessageTest#testGetAttachments3()}
	 * @testedby {@link SimpleMessageTest#testGetAttachments4()}
	 */
	public List<Part> getAttachments() {
		List<Part> parts = new ArrayList<Part>();
		try {
			if ( ! getContentType().contains("multipart/mixed"))
				return parts;
			Object myContent = getContent();
			Multipart contentParts = (Multipart) myContent;
			// treat all but first part as attachments
			for (int i = 1; i < contentParts.getCount(); i++) {
				Part part = contentParts.getBodyPart(i);
				// do we need to recurse??				
				parts.add(part);
			}
			return parts;
		} catch (Exception e) {
			throw new ExternalServiceException(e);
		}
	}

	/**
	 * A hacky method that tries to return the email text, regardless of the email format.
	 * <p>
	 * If the email is single-part and text only, return the body text.<br> 
	 * If it's
	 * single-part and HTML, return the CDATA of the HTML. 
	 * If it's multipart/alternative, return the plain text version (if we can find one).<br> 
	 * TODO If it's multipart/mixed, return a concatenation of every part that has mimetype
	 * text/*.
	 * 
	 * @throws MessagingException
	 */
	public String getBodyText() {
		try {
			// Normal case - 99.9% of the time
			Object body = getContent();
			return getBodyText2(getContentType(), body);
			// error handling...
		} catch (UnsupportedEncodingException e) {
			// This threw things in one of my emails: ansi_x3.110-1983
			// we could try to recover gracefully via
			// DataFlavor[] flavors = getDataHandler().getTransferDataFlavors();
			// or
			// expect some mangling from this!
			Log.w("email", getShortHeader() + ": " + e);
			try {
				InputStream raw = getDataHandler().getInputStream();
				byte[] bytes = FileUtils.readRaw(raw);
				String rawText = new String(bytes);
				return rawText;
			} catch (Exception e2) {
				// log this, throw the original
				Log.w("email", "Swallowing secondary exception: "+ e2);
				throw new ExternalServiceException(e);
			}
		} catch (Exception e) {
			Log.e("email", e);
			throw new ExternalServiceException(e);
		}
	}

	@Override
	public String getContentType() {
		try {
			String type = super.getContentType();
			return type;
		} catch (Exception e) {
			throw new ExternalServiceException(e);
		}
	}

	/**
	 * {@link #getSender()} is preferred, unless you really want to handle
	 * multiple senders.
	 */
	@Deprecated
	@Override
	public Address[] getFrom() throws MessagingException {
		return super.getFrom();
	}

	/**
	 * Get all the headers for this header name, returned as a single
     * String, with headers separated by the delimiter. If the
     * delimiter is <code>null</code>, only the first header is 
     * returned.
     *
     * @param name		the name of this header
     * @param delimiter		separator between values. Can be null
     * @return the value fields for all headers with this name, or null if none
     * @exception       	MessagingException
	 */
	@Override
	public String getHeader(String name, String delimiter) {
		try {
			return super.getHeader(name, delimiter);
		} catch (MessagingException e) {
			throw new ExternalServiceException(e);
		}
	}

	/**
	 * Returns the value of the "Message-ID" header field. Returns null if this
	 * field is unavailable or its value is absent.
	 * <p>
	 * Message-Id is a unique id generated by the server and does not change
	 * over time. No two messages should have the same Message-Id.
	 */
	@Override
	public String getMessageID() {
		try {
			return super.getMessageID();
		} catch (Exception e) {
			throw new ExternalServiceException(e);
		}
	}

	public Message getOriginal() {
		return original;
	}

	/**
	 * @return the sender
	 */
	@Override
	public InternetAddress getSender() {
		try {
			Address[] froms = getFrom();
			if (froms == null)
				return null;
			return ((InternetAddress) froms[0]);
		} catch (Exception e) {
			try {
				// try to fallback
				String s = getHeader("From", ",");
				if (s == null || s.isEmpty() || s.equals("<>")) {
					Log.d("email", "exception parsing sender -- and no good From: ["+s+"]");
				    s = getHeader("Sender",",");
				}
				Log.d("email", "exception parsing sender: "+s+" "+e);
				String[] ss = s.split(",");
				String s1 = ss[0];
				javax.mail.internet.InternetAddress[] addr = InternetAddress.parse(s1, false);
				return addr[0];				
			} catch(Exception e2) {
				throw new ExternalServiceException(e);
			}
		}
	}

	@Override
	public Date getSentDate() {
		try {
			return super.getSentDate();
		} catch (MessagingException e) {
			throw new ExternalServiceException(e);
		}
	}

	public Session getSession() {
		return session;
	}

	/**
	 * Useful for debugging. This will never throw an exception - but it may
	 * return an exception message instead!
	 * 
	 * @return subject, from, to
	 */
	public String getShortHeader() {
		try {
			StringBuilder sb = new StringBuilder();
			// sb.append(getContentID()+"\n");
			sb.append(getSubject() + "\n");
			sb.append(getSender() + "\n");
			sb.append(getTos(this) + "\n");
			sb.append("Content-type:" + getContentType());
			return sb.toString();
		} catch (Exception e) {
			// arse
			return "Cannot get message header: " + e;
		}
	}

	@Override
	public String getSubject() {
		try {
			return super.getSubject();
		} catch (MessagingException e) {
			throw new ExternalServiceException(e);
		}
	}

	public void setInReplyTo(Message in) {
		try {
			String msgId = ((MimeMessage) in).getMessageID();			
			assert msgId != null : in;
			setInReplyTo(msgId);
		} catch (MessagingException e) {
			throw new ExternalServiceException(e);
		}
	}

	/**
	 * Set the In Reply To header, which msut consiste of an email message ID.
	 * @param msgId
	 */
	public void setInReplyTo(String msgId) {
		// does this have a plausible format?
		// "by the more modern standard, In-Reply-To may contain only message IDs."
		assert msgId.contains("<") : "Bogus email msgId: "+ msgId;
		try {
			setHeader("In-Reply-To", msgId);
		} catch (MessagingException e) {
			throw new ExternalServiceException(e);
		}
	}

	/**
	 * Set the mime-type for this email
	 * 
	 * @param mimeType
	 * @see #getContentType()
	 */
	public void setMimeType(String mimeType) {
		try {
			super.setContent(getContent(), mimeType);
			// Why text/html and not mimeType??
			addHeaderLine("Content-type:text/html");
		} catch (Exception e) {
			throw new ExternalServiceException(e);
		}
	}

	public void setReplyTo(InternetAddress email) {
		try {
			setReplyTo(new Address[] { email });
		} catch (MessagingException e) {
			throw new ExternalServiceException(e);
		}
	}

	public void setSession(Session session) {
		this.session = session;
	}

	@Override
	public void setSubject(String subject) {
		try {
			super.setSubject(subject);
		} catch (MessagingException e) {
			throw new ExternalServiceException(e);
		}
	}


	private String getBodyText2(String mimetype, Object contents) throws MessagingException, IOException {
		if (contents instanceof Multipart) {
			Multipart mp = (Multipart) contents;
			// assume the 1st part is the email, and any other parts are attachments
			return getBodyText3_firstTextPart(mp, false);
		}
		if (mimetype.contains("text/plain")) {
			return contents.toString();
		} else if (mimetype.contains("text/html")) {			
			return WebUtils.stripTags(String.valueOf(contents));
		}
		return null;		
	}

	/**
	 * @return the html for this email, or null
	 * @see #getBodyText()
	 */
	public String getBodyHtml()  {
		try {
			Object contents = getContent();
			if (contents instanceof String)
				return contents.toString();
			if (contents instanceof Multipart) {
				Multipart mp = (Multipart) contents;
				return getBodyText3_firstTextPart(mp, true);
			}
			return null;			
		} catch(Exception ex) {
			throw new ExternalServiceException(ex);
		}
	}
	
	/** Return textual representation of the email */
	@Override
	public String toString() {
		try {
			String subject = getSubject();
			Object myContent = getContent();
			if (myContent instanceof Multipart) {
				Multipart contentParts = (Multipart) myContent;
				ByteArrayOutputStream text = new ByteArrayOutputStream();
				contentParts.writeTo(text);
				return subject + "\n\n" + text;
			} else
				return subject + "\n\n" + myContent;
		} catch (Exception e) {
			// oh well
			return "SimpleMessage["+e+"]";
		}
	}

	
	/**
	 * Set headers that mark this as an automatic email. This switches off the return path 
	 * It should avoid out-of-office responses.
	 * <p>
	 * See: http://stackoverflow.com/questions/154718/precedence-header-in-email 
	 * @param isAuto
	 */
	public void setAutoEmail(boolean isAuto) {
		try {
			if ( ! isAuto) {
				// TODO remove headers if set as auto?
			} else {
				setHeader("Precedence", "bulk");
				// x-Auto-Response-Suppress is for M$ see http://stackoverflow.com/questions/1027395/detecting-outlook-autoreply-out-of-office-emails
				// It means don't send an auto response to this email.
				setHeader("X-Auto-Response-Suppress", "OOF");
				// No return path
				setHeader("Return-Path", "<>");
				String[] irt = getHeader("In-Reply-To");
				if (irt != null && irt.length != 0) {
					// see http://tools.ietf.org/html/rfc3834
					setHeader("Auto-Submitted", "auto-replied");
					// else auto-generated -- but what if the reply details are set later?
				}
			}
		} catch (MessagingException e) {
			throw new ExternalServiceException(e);
		}		
	}
	
	public boolean isAutoEmail() {
		try {
			String[] p = getHeader("Precedence");
			if (p!=null && p.length!=0) {
				if ("bulk".equals(p[0]) || "junk".equals(p[0])) {
					return true;
				}
			}
			String[] p2 = getHeader("Auto-Submitted");
			if (p2!=null && p2.length!=0) {
				return true;
			}
			// A DSN?
			if (getContentType().contains("delivery-status")) {
				return true;
			}
			String[] failed = getHeader("X-Failed-Recipients");
			if (failed!=null) {
				return true;
			}
			return false;
		} catch(MessagingException mex) {
			throw new ExternalServiceException(mex);
		}
	}	


	public void setHtmlContent(String html) {
		try {
			setContent(html, WebUtils.MIME_TYPE_HTML_UTF8);
		} catch (MessagingException e) {
			throw new ExternalServiceException(e);
		}
	}

	/**
	 * Copy out the headers into a map. Does not really support repeated headers (last value wins).
	 * @return the headers for this message.
	 */
	public Map<String,String> getHeaderMap() {
		Enumeration<Header> hs = headers.getAllHeaders();
		HashMap map = new HashMap();
		while(hs.hasMoreElements()) {
			Header h = hs.nextElement();
			map.put(h.getName(), h.getValue());
		}
		return map;
	}

	/**
	 * @return ID for the previous message in the email thread, or null.
	 * ?? is this ever not a single message ID??
	 */
	public String getInReplyTo() {
		String replyTo = getHeader("In-Reply-To", " ");
		return replyTo;
	}
}
