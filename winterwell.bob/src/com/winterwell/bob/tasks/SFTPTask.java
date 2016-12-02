/**
 *
 */
package com.winterwell.bob.tasks;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import com.winterwell.bob.BuildTask;
import com.winterwell.utils.log.Log;
import com.winterwell.web.LoginDetails;

/**
 * SFTP is a pain. Probably the easiest way is to shell out to lftp.
 * Or we could pull in http://www.jcraft.com/jsch/
 * I've enquired whether we could use SCP instead.
 * http://stackoverflow.com/questions/5386482/how-to-run-the-sftp-command-with-a-password-from-bash-script
 *
 * @testedby SFTPTaskTest
 * @author Joe Halliwell <joe@winterwell.com>
 *
 */
public class SFTPTask extends BuildTask {

	private LoginDetails login;
	private File file;
	private String remotePath;
	private final static String LOGTAG = "SFTP";
	private boolean copyToRemote;

	/**
	 * FTP send one file
	 * @param user
	 * @param pwd
	 * @param file
	 * @param server
	 * @param remotePath
	 */
	public SFTPTask(LoginDetails login, File file, String remotePath) {
		this(login, file, remotePath, true);
	}
	
	/**
	 * FTP transfer file
	 * @param login
	 * @param file
	 * @param remotePath
	 * @param copyToRemote: if True the file will be sent to the remotePath, otherwise the remotePath will be copied to the file
	 */
	public SFTPTask(LoginDetails login, File file, String remotePath, boolean copyToRemote) {
		this.login = login;
		this.file = file.getAbsoluteFile();

		if (remotePath == null) remotePath = file.getName();
		this.remotePath = remotePath;
		assert remotePath.endsWith(file.getName()) : "Remote path should be a file";
		
		this.copyToRemote = copyToRemote;
	}
	
	@Override
	protected void doTask() throws Exception {
		Session session = null;
        Channel channel = null;
        ChannelSftp channelSftp = null;
		try {
            JSch jsch = new JSch();
            Log.i(LOGTAG, "Connecting to " + login);
            session = jsch.getSession(login.loginName, login.server, login.port == 0 ? 22 : login.port);
            session.setPassword(login.password);
            java.util.Properties config = new java.util.Properties();
            config.put("StrictHostKeyChecking", "no");
            // Use ssh keys if no password set
            if (login.password == null) {
            	config.put("PreferredAuthentications", "publickey");
            	jsch.setKnownHosts("~/.ssh/known_hosts");
            	jsch.addIdentity("~/.ssh/id_rsa");
            }
            
            session.setConfig(config);
            session.connect();
            Log.d(LOGTAG, "Host connected.");
            channel = session.openChannel("sftp");
            channel.connect();
            Log.d(LOGTAG, "sftp channel opened and connected.");
            channelSftp = (ChannelSftp) channel;
            
            if (copyToRemote) put(channelSftp, file, remotePath);
            else get(channelSftp, file, remotePath);
        } catch (Exception ex) {
             Log.e(LOGTAG, ex);
             throw ex;
        }
        finally{
            channelSftp.exit();
            Log.d(LOGTAG, "sftp Channel exited.");
            channel.disconnect();
            Log.d(LOGTAG, "Channel disconnected.");
            session.disconnect();
            Log.d(LOGTAG, "Host Session disconnected.");
        }
	}
	
	private void put(ChannelSftp channelSftp, File file, String remotePath) throws FileNotFoundException, SftpException {
		Log.d(LOGTAG, "Transferring to " + remotePath);
        channelSftp.put(new FileInputStream(file), remotePath);
        Log.i(LOGTAG, "File transfered successfully to host.");
	}
	
	private void get(ChannelSftp channelSftp, File file, String remotePath) throws SftpException, IOException {
		Log.d(LOGTAG, "Transferring from " + remotePath);
		InputStream is = channelSftp.get(remotePath);
		BufferedInputStream bis = new BufferedInputStream(is);
		
		OutputStream os = new FileOutputStream(file);
		BufferedOutputStream bos = new BufferedOutputStream(os);
		int read = 0;
		while ((read = bis.read()) > 0) {
			bos.write(read);
		}
		bos.close();
		Log.i(LOGTAG, "File transfered successfully from host.");
	}
}
