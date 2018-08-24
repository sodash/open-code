package com.winterwell.web.email;

import java.io.Closeable;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;

import javax.mail.AuthenticationFailedException;
import javax.mail.FetchProfile;
import javax.mail.Flags.Flag;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.search.ComparisonTerm;
import javax.mail.search.ReceivedDateTerm;
import javax.mail.search.SearchTerm;
import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;

//import com.sun.mail.imap.IMAPFolder;
//import com.sun.mail.imap.IMAPMessage;
//import com.sun.mail.imap.IMAPStore;
//import com.sun.mail.util.MailSSLSocketFactory;
import com.winterwell.utils.FailureException;
import com.winterwell.utils.Key;
import com.winterwell.utils.Printer;
import com.winterwell.utils.TimeOut;
import com.winterwell.utils.Utils;
import com.winterwell.utils.containers.Containers;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.time.TUnit;
import com.winterwell.utils.time.Time;
import com.winterwell.utils.web.XStreamUtils;
import com.winterwell.web.ConfigException;
import com.winterwell.web.ExternalServiceException;
import com.winterwell.web.LoginDetails;

import sun.security.provider.certpath.AdjacencyList;
import sun.security.provider.certpath.SunCertPathBuilderException;

/**
 * Access an IMAP mail account.
 * <p>
 * Pretty much every method can throw an {@link ExternalServiceException}.
 * 
 * <h3>Life-cycle</h3>
 * The ImapClient maintains a current folder, from which emails are fetched or
 * added. <br>
 * You must close an {@link IMAPClient} when you are done.
 * 
 * TODO maintain a local cache of headers and use folder-listener to keep up to
 * date
 * 
 * Ref: https://javamail.java.net/nonav/docs/api/com/sun/mail/imap/package-summary.html
 * 
 * @author daniel
 * @testedby {@link IMAPClientTest}
 */
public final class IMAPClient implements Closeable {

	transient boolean debug;
	
	public void setDebug(boolean debug) {
		this.debug = debug;
	}
	
	
	/**
	 * Set this property with LoginDetails.put to switch ssl on/off. The default
	 * is *on*.
	 */
	public static final Key<Boolean> USE_SSL = new Key<Boolean>(
			"IMAPClient.ssl");

	private static final String LOGTAG = "imap";

	private Folder folder;
	private String host;
	private String password;
	private Integer port;
	private int readWriteMode = Folder.READ_ONLY;
	private Session session;

	private Store store;

	private String user;
	/**
	 * Remote search versus switch to grab-everything-and-handle-locally if
	 * false (the default);
	 */
	private boolean useRemoteSearch;
	public void setUseRemoteSearch(boolean useRemoteSearch) {
		this.useRemoteSearch = useRemoteSearch;
	}

	private boolean usingSSL = true;

	/**
	 * If > 0, cap the number of returned results to this.
	 */
	private int max;

	private boolean gmail;

	/**
	 * 
	 * @param loginDetails
	 *            Must not be null. Must have a server, login-name and password.
	 *            Will also look for port and {@link #USE_SSL}
	 */
	public IMAPClient(LoginDetails loginDetails) {
		this.host = loginDetails.server;
		this.user = loginDetails.loginName;
		this.password = loginDetails.password;
		this.port = loginDetails.port;
		if (Utils.isBlank(this.host))
			throw new ConfigException("host needed for imap");
		// smtp we can do without user/password authentication, but imap?
		if (Utils.isBlank(this.user) || Utils.isBlank(this.password))
			throw new ConfigException("user+password needed for imap");
		// To SSL or not to SSL?
		// switch off SSL 'cos it doesn't fall back gracefully in
		// the face of certificates of unknown authority (such as Winterwell's)
		// But: SSL is needed by GMail or it just hangs (yuck)
		Boolean useSSL = loginDetails.get(USE_SSL);
		if (useSSL != null) {
			setUsingSSL(useSSL);
		}
		// Assume remote search will work? We have seen bugs with it in the past.
		setUseRemoteSearch(true);
		Log.d("imap", XStreamUtils.serialiseToXml(loginDetails));
	}

	public String getUser() {
		return user;		
	}
	
	/**
	 * Add an Message to the current folder.
	 * 
	 * @param Message
	 */
	public void add(Message msg) {
		Message[] msgs = new Message[] { msg };
		try {
			folder.appendMessages(msgs);
		} catch (MessagingException e) {
			throw new ExternalServiceException(e);
		}
	}

	/**
	 * Close. Does not throw any exceptions.
	 */
	@Override
	public void close() {
		// Folder
		try {
			closeFolder();
		} catch (ExternalServiceException e) {
			// Swallow it
			Log.report(e);
		}
		// Store
		try {
			if (store != null && store.isConnected()) {
				store.close();
			}
		} catch (MessagingException e) {
			// Swallow it
			Log.report(e);
		}
	}

	/**
	 * Close the currently open folder, if there is one
	 */
	private void closeFolder() {
		if (folder == null || !folder.isOpen())
			return;
		try {
			Log.e(LOGTAG, "close folder "+folder.getName());
			folder.close(false);
			folder = null;
		} catch (MessagingException e) {
			throw new ExternalServiceException(e);
		}
	}

	private void connect() throws ConfigException {
		Log.e(LOGTAG, "connect "+user+"...");
		assert !isConnected();
		// add a timeout to this code
		TimeOut timeOut = null;
		Properties props = null;
		try {
			timeOut = new TimeOut(TUnit.MINUTE.getMillisecs());
			// Get a Session object
			if (session == null) {
				props = System.getProperties();
				if (usingSSL) {
					connect2_sslprops(props);
				}
				
				// FIXME delete after fixing, Feb 2014
//				Log.d(LOGTAG, "Session props: "+props);
//				props.setProperty("mail.debug", "true");
				
				session = Session.getInstance(props);
			}

			// Get a Store object
			store = session.getStore("imap");
			// Connect
			store.connect(host, user, password);
			Log.e(LOGTAG, "...connected "+user);
			// done
		} catch (Exception e) {
			Log.e("imap.error", e+" Session props: "+props);
			// TODO: make behaviour configurable.
			 if (e.getMessage().contains("SSL") && isUsingSSL()) {
				 setUsingSSL(false);
				 Log.w(LOGTAG, "Switching to plaintext for "+user+" after: "+e);
				 connect();
				 return;
			 }
			 // Certificate exceptions are deeply nested - look for one
			 Throwable ex = e;
			 while (ex != null) {
				if (ex instanceof SunCertPathBuilderException) {
					SunCertPathBuilderException sex = (SunCertPathBuilderException) ex;
					installCertificate(sex);
					connect();
					return;
				}
				ex = ex.getCause();
			}
			// It wasn't a certificate problem after all
			if (e instanceof AuthenticationFailedException) {
				// add in some more info about the setup
				String info = " host:" + host + " user:" + user + " password:"
						+ (password == null ? "null" : "HIDDEN (not-null)");
				throw new ConfigException(e + info);
			}
			throw Utils.runtime(e);
		} finally {
			TimeOut.cancel(timeOut);
		}
	}

	private void connect2_sslprops(Properties props) throws GeneralSecurityException {
		Log.d(LOGTAG, "using SSL...");		
//		java.security.Security
//				.addProvider(new com.sun.net.ssl.internal.ssl.Provider());
		
//		MailSSLSocketFactory socketFactory = new MailSSLSocketFactory();
//		SocketFactory socketFactory = SSLSocketFactory.getDefault();
//		socketFactory.setTrustAllHosts(true);
		
//		// ??trust everyone?? (untested)
//		if (user.equals("bot@soda.sh")) {
//			TrustManager[] trustManagers = socketFactory.getTrustManagers();		
//			DummyTrustManager trustAll = new DummyTrustManager();
//			TrustManager[] tms = trustManagers==null? new TrustManager[1] : Arrays.copyOf(trustManagers, trustManagers.length+1);
//			tms[tms.length-1] = trustAll;
//			socketFactory.setTrustManagers(trustManagers);
//		}
		
//		props.put("mail.imap.socketFactory", socketFactory);
		final String imap = gmail? "gimap" : "imap";
		props.setProperty("mail."+imap+".ssl.enable", "true");
//		props.setProperty("mail.imap.socketFactory.class",
//				MailSSLSocketFactory.class.getCanonicalName()
//				"javax.net.ssl.SSLSocketFactory"
//				);
		// fall back to normal IMAP connections on failure.
		props.setProperty("mail."+imap+".socketFactory.fallback",
				"true");
		// use the simap port for imap/ssl connections.
		// -- unless told otherwise
		int _port = port==null || port<1? 993 : port; 
		props.setProperty(
				"mail."+imap+".socketFactory.port",
				 String.valueOf(_port));

	}

	/**
	 * Create a new folder and open it as the current folder.
	 * 
	 * @param folderName
	 */
	public void createFolder(String folderName) {
		try {
			if (!isConnected()) {
				connect();
			}
			// Close previous folder if any
			closeFolder();
			folder = store.getFolder(folderName);
			if (folder.exists())
				throw new IllegalArgumentException(folderName
						+ " already exists");
			boolean ok = folder.create(Folder.HOLDS_FOLDERS);
			if (!ok)
				throw new ExternalServiceException("Failed to create folder "
						+ folderName);
			// Open as the current folder
			folder.open(readWriteMode);
		} catch (MessagingException e) {
			throw new ExternalServiceException(e);
		}
	}

	public void delete(Message message) {
		// is this needed??
		if (message instanceof SimpleMessage) {
			message  = ((SimpleMessage) message).getOriginal();
		}
		assert folder != null;
		assert readWriteMode == Folder.READ_WRITE;
		try {
			message.setFlag(Flag.DELETED, true);
			// expunge immediately (could be inefficient for multiple deletes)
//			Message[] expunged = folder.expunge(new Message[] { message });
//			assert Containers.indexOf(message, expunged) != -1;
		} catch (MessagingException e) {
			throw new ExternalServiceException(e);
		}
	}

	/**
	 * Closes the connection if necessary. You should not rely on this - it is a
	 * backup approach. Call {@link #close()} explicitly.
	 */
	@Override
	protected void finalize() throws Throwable {
		try {
			if (isConnected()) {
				Log.report("email", this + " was not closed! ",
				// +Printer.toString(new Exception(), true), // no point -- it's
				// the finalizer thread we get, not the culprit
						Level.WARNING);
				close();
			}
		} catch (Exception e) {
			// Ignore
		}
		super.finalize();
	}

	/**
	 * @return a lazy-fetching array of all message objects in the current
	 *         folder. May be empty, never null. These should be lightweight
	 *         objects which retrieve their body only if necessary (eg. if
	 *         passed into {@link SimpleMessage}).
	 */
	public Message[] getEmailHeaders() {
		// Connect?
		if (!isConnected()) {
			connect();
		}
		// Open inbox?
		if (folder == null || !folder.isOpen()) {
			openFolder(null);
		}
		try {
			// Get message headers
			Message[] msgs = folder.getMessages();
			assert msgs != null;
			// Use a suitable FetchProfile to draw down the headers only
			FetchProfile fp = new FetchProfile();
			fp.add(FetchProfile.Item.ENVELOPE);
			// fp.add(FetchProfile.Item.FLAGS)
			folder.fetch(msgs, fp);
			// Return them
			return msgs;
		} catch (Exception e) {
			throw Utils.runtime(e);
		}
	}
	
	/**
	 * See https://docs.oracle.com/javaee/6/api/javax/mail/Folder.html#getMessages(int,%20int)
	 * @param j 
	 * @param i 
	 * @return a lazy-fetching array of all message objects in the current
	 *         folder. May be empty, never null. These should be lightweight
	 *         objects which retrieve their body only if necessary (eg. if
	 *         passed into {@link SimpleMessage}).
	 */
	public Message[] getEmailIds(int start, int end) {
		// Connect?
		if (!isConnected()) {
			connect();
		}
		// Open inbox?
		if (folder == null || !folder.isOpen()) {
			openFolder(null);
		}
		try {
			// Get message headers
			Message[] msgs = folder.getMessages(start,end);
			assert msgs != null;
			// Return them
			return msgs;
		} catch (Exception e) {
			throw Utils.runtime(e);
		}
	}

	/**
	 * Warning: this pulls down the whole folder contents!
	 * 
	 * @see #getEmailHeaders()
	 * @see #getEmailsReceivedAfter(Date)
	 */
	public List<SimpleMessage> getEmails() {
		Message[] msgs = getEmailHeaders();
		if (max>0 && msgs.length > max) {
			Log.d(LOGTAG, "Cap at max "+max+" of "+msgs.length);
			msgs = Arrays.copyOf(msgs, max);
		}
		return getEmails(msgs);
	}
	
	public List<SimpleMessage> getEmails(Message[] headers) {
		List<SimpleMessage> mails = new ArrayList<SimpleMessage>(headers.length);
		for (Message msg : headers) {
			mails.add(SimpleMessage.create(msg));
			if (max>0 && mails.size() >= max) {
				Log.d(LOGTAG, "Stop at max "+max+" of "+headers.length);
				break;
			}
		}
		return mails;
	}

	/**
	 * 
	 * @param searchTerm
	 * @param receivedAfter Can be null. If set, this should also be part of searchTerm!
	 * It is used to fast-filter emails (because remote servers, even gmail, are lousy 
	 * at implementing IMAP search). 
	 * @return
	 */
	public List<SimpleMessage> getEmails(SearchTerm searchTerm, Time receivedAfter) {
		Log.d(LOGTAG, user+" getEmails "+searchTerm+" "+receivedAfter+"...");
		// ?? Is there a way of determining what is and isn't supported
		// by the server?
		assert searchTerm != null;
		// Connect?
		if (!isConnected()) {
			connect();
		}
		// Open inbox?
		if (folder == null || !folder.isOpen()) {
			openFolder(null);
		}
		try {
			// Attributes & Flags for all messages ..			
			Message[] msgs = folder.search(searchTerm);
			Log.d(LOGTAG, user+" getEmails found "+msgs.length+" with remote-filter "+XStreamUtils.serialiseToXml(searchTerm)+". Local filtering...");
			// Return them
			List<SimpleMessage> mails = new ArrayList<SimpleMessage>(
					msgs.length);
			// Wrap as SimpleMessage -- and check they do fit the filter (remote filtering is unreliable).
			for (Message message : msgs) {
				Log.d(LOGTAG, user+" getEmails check "+message.getSubject()+" "+message.getSentDate()+"...");
				// fast check the date before we fetch any data
				if (receivedAfter!=null && message.getReceivedDate()!=null) {
					Time rd = new Time(message.getReceivedDate());
					if (rd.isBefore(receivedAfter)) {
						System.out.print(".");
						continue; // APril 2016: GMail ignores the date search term :(
					}
				}
				// pull down the whole message
				Log.d(LOGTAG, user+" getEmails create SimpleMessage (may fetch data) "+message);
				SimpleMessage sm = SimpleMessage.create(message);
				// Check it matches the search term -- the remote server can ignore them!
				if (searchTerm!=null && ! searchTerm.match(sm)) {
					Log.d(LOGTAG, user+" getEmails Skip "+sm.getMessageID()+" "+sm.getSubject()+" !~ "+searchTerm);
					continue;
				}
				mails.add(sm);
				
				if (max>0 && mails.size() >= max) {
					Log.d(LOGTAG, user+" getEmails Stop at max "+max+" of "+msgs.length);
					break;
				}
			}
			Log.d(LOGTAG, user+" ...getEmails done. Returning "+mails.size()+" of "+msgs.length+". Filter: "+XStreamUtils.serialiseToXml(searchTerm));
			return mails;
		} catch (Exception e) {
			Log.e(LOGTAG, e);
			throw Utils.runtime(e);
		}
	}

	/**
	 * @param date
	 * @return all emails in the current folder received after date
	 */
	public List<SimpleMessage> getEmailsReceivedAfter(Time time) {
		Date date = time.getDate();
		try {
			if (useRemoteSearch)
				return getEmailsReceivedAfter2_remoteSearch(date);
			return getEmailsReceivedAfter2_localSearch(date);
		} catch (MessagingException e) {
			throw new ExternalServiceException(e);
		}
	}

	private List<SimpleMessage> getEmailsReceivedAfter2_localSearch(Date date)
			throws MessagingException {
		/*
		 * Given buggy behaviour using search with winterwellassociates from
		 * Windows: Tue Jan 27 21:23:58 GMT 2009 is before Tue Jan 27 21:52:16
		 * GMT 2009 We have switched to caching headers, then searching locally
		 * as the default behaviour.
		 * 
		 * TODO It may be that this is too costly though for regular use on
		 * large inboxes
		 * 
		 * We could move to using this as a periodic safety net for when remote
		 * search screws up
		 */
		Message[] msgs = getEmailHeaders();
		List<SimpleMessage> emails = new ArrayList<SimpleMessage>();
		// Filter 'em for bugs
		for (Message msg : msgs) {
			Date rd = msg.getReceivedDate();
			if (rd==null) {
				// Bit of a Hack: allow for 1 day in transit
				rd = msg.getSentDate();
				if (rd!=null) rd = new Date(rd.getTime() + TUnit.DAY.millisecs);
			}
			if (date.before(rd)) {
				emails.add(SimpleMessage.create(msg));
			}
			if (max>0 && emails.size() >= max) {
				Log.d(LOGTAG, "LocalSearch: Stop at max "+max+" of a possible "+msgs.length);
				break;
			}
		}
		return emails;
	}

	private List<SimpleMessage> getEmailsReceivedAfter2_remoteSearch(Date date)
			throws MessagingException {
		ReceivedDateTerm sinceDate = new ReceivedDateTerm(ComparisonTerm.GT, date);
		List<SimpleMessage> emails = getEmails(sinceDate, new Time(date));
		// check for bugs in remote search
		for (SimpleMessage msg : emails.toArray(new SimpleMessage[0])) {
//			sinceDate.match(msg); Do we need the extra lenience below?
			Date rd = msg.getReceivedDate();
			if (rd==null) {
				// Bit of a Hack: allow for 1 day in transit
				rd = msg.getSentDate();
				if (rd!=null) rd = new Date(rd.getTime() + TUnit.DAY.millisecs);
			}
			if (rd!=null && date.after(rd)) {
				emails.remove(msg);
				Log.report(
						"Error using remote IMAP search (fixed by local filter)",
						Level.FINE);
			}
		}
		return emails;
	}

	/**
	 * @return
	 */
	public List<String> getFolderNames() {
		return getFolderNames(true);
	}
	
	/**
	 * 
	 * @param topLevel Usually true to list top-level folders. Otherwise lists the sub-folders of the open folder.
	 * @return
	 */
	public List<String> getFolderNames(boolean topLevel) {
		if ( ! isConnected()) {
			connect();
		}
		Folder[] folders;
		try {
			if (topLevel) {
				Folder defaultFolder = store.getDefaultFolder();
				folders = defaultFolder.list();
			} else { 
				folders = folder.list();
			}
		} catch (MessagingException e) {
			throw new ExternalServiceException(e);
		}
		List<String> names = new ArrayList<String>(folders.length);
		for (Folder fldr : folders) {
			names.add(fldr.getName());
		}
		return names;
	}

	/**
	 * @param folders
	 * @return
	 */
	private Folder getInbox(Folder[] folders) {
		for (Folder f : folders) {
			String fn = f.getName();
			if (!"inbox".equalsIgnoreCase(fn)) {
				continue;
			}
			return f;
		}
		return null;
	}

	/**
	 * Get fresh emails. These are recent (new since last folder opening -- it
	 * is not clear how this acts in practice) and not seen.
	 * <p>
	 * Warning: it has been reported that GMail does not properly implement the
	 * {@link Flag#RECENT} flag, and this method may skip emails.
	 * 
	 * @return
	 */
	public List<SimpleMessage> getNewEmails() {
		Message[] msgs = getEmailHeaders();
		List<SimpleMessage> emails = new ArrayList<SimpleMessage>();
		try {
			for (Message message : msgs) {
				if (!message.getFlags().contains(Flag.RECENT)) {
					continue;
				}
				if (!message.getFlags().contains(Flag.SEEN)) {
					continue;
				}
				emails.add(SimpleMessage.create(message));
			}
		} catch (MessagingException e) {
			throw new ExternalServiceException(e);
		}
		return emails;
	}

	/**
	 * @return the currently open folder, or null if no folder is open.
	 */
	public Folder getOpenFolder() {
		if (folder == null || !folder.isOpen())
			return null;
		return folder;
	}

	public Session getSession() {
		return session;
	}

	private void installCertificate(SunCertPathBuilderException sex) {
		AdjacencyList adjl = sex.getAdjacencyList();
		Log.d("imap", "AdjacenyList: "+adjl);
		Log.d("imap", "ssl: "+isUsingSSL());
		throw new FailureException(toString()+
				" Need to write code to install SSL certificates: "+sex+" "+adjl);
	}

	public boolean isConnected() {
		return store != null && store.isConnected();
	}

	/**
	 * @return the usingSSL
	 */
	public boolean isUsingSSL() {
		return usingSSL;
	}

	/**
	 * Open a folder. The folder can subsequently be accessed via
	 * {@link #getOpenFolder()}
	 * 
	 * @param folderName
	 *            null for "try to guess inbox". The folder must already exist.
	 *            This method has quite lenient fallback behaviour: if the
	 *            folderName does not exist as-is, it will try pattern variants:
	 *            first any-prefix, then any-prefix and any-suffix.
	 * @return the folder's full name
	 * @throws IllegalArgumentException
	 *             if the folder does not exist
	 * 
	 */
	/*
	 * ?? If the server uses a non-blank prefix for personal folders (eg,
	 * Courier uses "INBOX.") the folder will not be found. We need to decide on
	 * the semantics of this method: 1) Open the named personal folder (using
	 * the NAMESPACE extension to determine the correct prefix) 2) Open the
	 * named shared folder (again using the NAMESPACE extension to determine the
	 * prefix) 3) Search all available namespaces to find a folder with the
	 * given name 4) Open the folder with the given literal name, and let the
	 * client determine the correct prefix.
	 * 
	 * It probably makes sense to provide (1) and (2) as separate functions,
	 * implemented using (4).
	 */
	public String openFolder(String folderName) {
		if (!isConnected()) {
			connect();
		}
		// Same as the current one?
		if (folder != null && folder.isOpen()
				&& folder.getFullName().equals(folderName)) {
			return folderName; // do nothing
		}
		// Close previous folder if any
		closeFolder();
		try {
			// open new one
			Log.d(LOGTAG, "openFolder: get folder...");
			if (folderName == null) {
				// Search for Inbox
				folder = openFolder2_findInbox();
			} else {
				folder = store.getFolder(folderName);
			}
			if (folder != null && folder.exists()) {
				// open
				Log.d(LOGTAG, "openFolder: open "+folder.getName()+"...");
				folder.open(readWriteMode);
				Log.d(LOGTAG, "...openFolder "+folder.getName()+" done");
				return folder.getFullName();
			}
			// fallback behaviour
			Log.d(LOGTAG, "openFolder: fallback behaviour for "+folderName);
			// ...1st try any-prefix
			Folder dflt = store.getDefaultFolder();
			Folder[] folders = dflt.list("*" + folderName);
			folder = openFolder2(folderName, folders);
			// ...then try any-prefix and any-suffix
			if (folder != null) {
				Log.d(LOGTAG, "...openFolder "+folder.getName()+" done");
				return folder.getFullName();
			}
			folders = dflt.list("*" + folderName + "*");
			folder = openFolder2(folderName, folders);
			if (folder != null) {
				Log.d(LOGTAG, "...openFolder "+folder.getName()+" done");
				return folder.getFullName();
			}
			// give up
			Log.d(LOGTAG, "openFolder: failed to find "+folderName);
			throw new IllegalArgumentException("Invalid folder: " + folderName
					+ "\nValid folders: " + Printer.toString(dflt.list("*")));
		} catch (MessagingException e) {
			throw new ExternalServiceException(e);
		}
	}

	/**
	 * sub-method of {@link #openFolder(String)}. opens the folder, if it's
	 * passed one-and-only-one
	 * 
	 * @param folderName
	 * @param folders
	 * @return open folder if successful, null if there is no folder
	 * @throws MessagingException
	 * @throws IllegalArgumentException
	 */
	private Folder openFolder2(String folderName, Folder[] folders)
			throws MessagingException {
		if (folders.length == 0)
			return null;
		if (folders.length == 1) {
			folder = folders[0];
			Log.d(LOGTAG, "openFolder2: open "+folder.getName()+"...");
			folder.open(readWriteMode);
			return folder;
		}
		throw new IllegalArgumentException("Invalid folder: " + folderName
				+ "\nValid folders: "
				+ Printer.toString(store.getDefaultFolder().list("*")));
	}

	/**
	 * Searches for the user's INBOX. First it tries their personal namespaces,
	 * then the root folder, then anything under the root folder. Returns null
	 * if it failed to find a folder called INBOX.
	 * 
	 * @return
	 * @throws MessagingException
	 */
	private Folder openFolder2_findInbox() throws MessagingException {
		Folder personalInbox = getInbox(store.getPersonalNamespaces());
		if (personalInbox != null)
			return personalInbox;
		Folder defaultFolder = store.getDefaultFolder();
		String fn = defaultFolder.getName();
		if ("inbox".equalsIgnoreCase(fn))
			return defaultFolder;
		return getInbox(defaultFolder.list());
	}

	/**
	 * @param readAndWrite
	 *            if true, sets the interaction mode to
	 *            {@link Folder#READ_WRITE}. Default is {@link Folder#READ_ONLY}
	 *            . Must be called before opening a folder for writing.
	 */
	public void setReadWriteMode(boolean readAndWrite) {
		readWriteMode = readAndWrite ? Folder.READ_WRITE : Folder.READ_ONLY;
		// Whatever folder we have open is now in the wrong mode
		closeFolder();
	}

	/**
	 * @param usingSSL
	 *            the usingSSL to set
	 */
	public void setUsingSSL(boolean usingSSL) {
		assert !isConnected();
		this.usingSSL = usingSSL;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + ":" + user + "@" + host;
	}

	public void setMax(int perRunRequestLimit) {
		this.max = perRunRequestLimit;
	}

	public IMAPClient setGMail(boolean b) {
		gmail = b;
		return this;
	}

	

}
