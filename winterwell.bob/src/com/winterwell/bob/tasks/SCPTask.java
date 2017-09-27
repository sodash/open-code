package com.winterwell.bob.tasks;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;

import com.winterwell.bob.BuildTask;
import com.winterwell.utils.io.FileUtils;

/**
 * Use SCP to copy a local file to a remote server.
 *
 * If the target is a directory, the target path *must* end with a "/"
 *
 * @testedby SCPTaskTest
 * @author daniel
 *
 */
public class SCPTask extends ProcessTask {

	private static final String GET = "get";
	private static final String PUT = "put";
	BuildTask mkdirTask = null;
	private final String op;
	private final File localFile;
	private String remotePath;
	private String tempPath;
	private String userAtServer;
	// hack to allow editing of the global static
	private boolean atomic = _atomic;


	/**
	 * TODO EXPERIMENTAL If true, use a temp file to provide near-atomic behaviour.
	 * TODO make non-static
	 */
	public static boolean _atomic = true;

	/**
	 * TODO EXPERIMENTAL If true, use a temp file to provide "near-atomic" behaviour.
	 * TODO make non-static.
	 * The up/download is first sent to a temp file, then this is moved into place.
	 */
	public void setAtomic(boolean atomic) {
		this.atomic = atomic;
	}
	
	public void setMkdirTask(boolean onOff) {
		if (onOff) {
			assert mkdirTask != null;
			return;
		}
		mkdirTask = null;
	}
	
	/**
	 * Copy file TO the remote server
	 * @param localFile Can be a directory
	 * @param userAtServer E.g. "alice@fbuildoobar.com" or "alice:password@foovar.com"
	 * @param remotePath E.g. /home/foo/bar.txt
	 */
	public SCPTask(File localFile, String userAtServer, String remotePath) {
		super("scp");		
		op=PUT;
		assert userAtServer != null && userAtServer.length() != 0 : this;
		assert ! userAtServer.startsWith("null@") || userAtServer.endsWith("@null") : this;
		assert localFile.exists() : localFile;
		this.localFile = localFile;
		this.remotePath = remotePath;
		this.userAtServer = userAtServer;		
		
		// Bug with directories ??which we should fix at some point
		if (localFile.isDirectory()) atomic=false;
		
		if (atomic) tempPath = remotePath+".temp";
		
		disableHostKeyChecks();

		String src = localFile.getAbsolutePath();
		// a directory? then send *
		if (localFile.isDirectory()) {
			if (src.endsWith("/")) src += "*";
			else src += "/*";
		}
		addArg(src);
		addArg(userAtServer + ":" + (atomic? tempPath : remotePath));
		String remoteDir = getDirectory(remotePath);
		if ( ! remoteDir.equals("")) {
			mkdirTask = new RemoteTask(userAtServer, "mkdir -p " + remoteDir);
		}

		// Avoid uploading to self
		avoidNoOpUpload(localFile, userAtServer, remotePath);
	}



	/**
	 * Copy file FROM the remote server
	 * @param localFile
	 * @param userAtServer E.g. "alice@foobar.com" or "alice:password@foovar.com"
	 * @param remotePath E.g. /home/foo/bar.txt
	 */
	public SCPTask(String userAtServer, String remotePath, File localFile) {
		super("scp");
		op = GET;
		assert userAtServer != null && userAtServer.length() != 0 : this;
		assert ! userAtServer.startsWith("null@") || userAtServer.endsWith("@null") : this;
		this.localFile = localFile;
		this.userAtServer = userAtServer;

		// Bug with directories ??which we should fix at some point
		if (localFile.isDirectory()) atomic=false;
		
		if (atomic) tempPath = localFile.getAbsolutePath() + ".temp";
		
		disableHostKeyChecks();

		addArg(userAtServer + ":" + remotePath);
		addArg(atomic? tempPath : localFile.getAbsolutePath());
		// Avoid downloading to self
		avoidNoOpUpload(localFile, userAtServer, remotePath);
	}

	/**
	 * Add arguments that switch off paranoid host key checking.
	 * We don't track individual server keys *at the moment*.
	 */
	private void disableHostKeyChecks() {
		// Disable host key checks
		addArg("-o StrictHostKeyChecking=no"); // This seems to cause bad JVM-hangs bugs (Ubuntu Linux 11.10, as seen by DBW, April 2012)
		addArg("-o UserKnownHostsFile=/dev/null");
		// Steven reported still being asked?! (Jan 2013)
	}

	/**
	 * Somewhat (but necessarily) heuristic method to obtain parent directory
	 * of the specified path. This may include user@host: portions etc.
	 * @param path
	 * @return
	 */
	private String getDirectory(String path) {
		if ( ! path.contains("/")) return "";
		return path.substring(0, path.lastIndexOf("/"));
	}

	/**
	 * Check we're not uploading to this server.
	 * @throws IllegalArgumentException
	 * @param _localFile
	 * @param userAtServer
	 * @param remotePath
	 */
	private void avoidNoOpUpload(File _localFile, String userAtServer,
			String remotePath) throws IllegalArgumentException {
		String hostName;
		try {
			hostName = InetAddress.getLocalHost().getHostName();
			String path = hostName+":"+_localFile.getAbsolutePath();
			String rpath = userAtServer+":"+remotePath;
			int i = rpath.indexOf('@');
			if (i!=-1 && i!=rpath.length()-1) rpath = rpath.substring(i+1);
			if (path.equals(rpath)) throw new IllegalArgumentException("Upload to "+rpath+" would copy "+path+" to itself!");
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void doTask() throws Exception {
		// Make the directory
		if (mkdirTask != null) {
			mkdirTask.run();
		}
		if (GET.equals(op)) {
			localFile.getParentFile().mkdirs();
		}
		
		// SCP!
		super.doTask();
		
		// Move?
		if (atomic) {
			if (GET.equals(op)) { 
				FileUtils.move(new File(tempPath), localFile);
			} else {
				RemoteTask mvTask = new RemoteTask(userAtServer, "mv "+tempPath+" "+remotePath);
				mvTask.run();
			}
		}
	}

	
}
