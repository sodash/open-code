/**
 *
 */
package winterwell.bob.tasks;


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

import winterwell.bob.BuildTask;
import com.winterwell.utils.TodoException;
import com.winterwell.utils.log.Log;
import com.winterwell.web.LoginDetails;

/**
 * @author daniel
 *
 */
public class FTPTask extends BuildTask {

	private LoginDetails login;
	private File file;
	private FTPClient ftpc;
	private String remotePath;

	/**
	 * FTP send one file
	 * @param user
	 * @param pwd
	 * @param file
	 * @param server
	 * @param remotePath
	 */
	public FTPTask(LoginDetails login, File file, String remotePath)
	{
		op = PUT;
		this.login = login;
		this.file = file;
		this.remotePath = remotePath;
	}

	public void setOp(String op) {
		this.op = op;
	}

	@Override
	protected void doTask() throws Exception {
		ftpc = new FTPClient();
		ftpc.setAutodetectUTF8(true);

		ftpc.connect(login.server);
		Log.d(LOGTAG, "Connected to " + login.server+ " on " + ftpc.getDefaultPort());
        // After connection attempt, you should check the reply code to verify
        // success.
        int reply = ftpc.getReplyCode();
        if ( ! FTPReply.isPositiveCompletion(reply)) {
            throw new IOException("FTP server "+login.server+" refused connection.");
        }

        boolean ok = ftpc.login(login.loginName, login.password);
        if ( ! ok) {
        	Log.d(LOGTAG, "Failed to login to " + login.server+" as "+login.loginName+" w "+login.password); // TODO remove security hole
        	return;
        }
        Log.d(LOGTAG, "Logged in to " + login.server+" as "+login.loginName);

        ftpc.setFileType(FTP.BINARY_FILE_TYPE);
        ftpc.enterLocalPassiveMode();

        if (op==PUT) {
        	Log.d(LOGTAG, "Sending "+file+"...");
            InputStream input = new FileInputStream(file);
            ftpc.storeFile(remotePath, input);
            input.close();
            Log.d(LOGTAG, "...sent "+file);
        } else if (op.equals("list")) {
        	FTPFile[] files = ftpc.listFiles();
        	for (FTPFile ftpFile : files) {
				System.out.println(ftpFile.getName()+"\t"+ftpFile.getRawListing());
			}
        } else {
        	throw new TodoException(op);
        }
	}

	private static final String GET = "get";
	private static final String PUT = "put";

	private String op;

	@Override
	public void close() {
		if (ftpc!=null) {
			try {
				ftpc.logout();
			} catch (IOException e) {
				Log.e(LOGTAG, e);
			}
			try {
				if (ftpc.isConnected()) {
					ftpc.disconnect();
				}
			} catch (IOException e) {
				Log.e(LOGTAG, e);
			}
		}
		super.close();
	}

}
