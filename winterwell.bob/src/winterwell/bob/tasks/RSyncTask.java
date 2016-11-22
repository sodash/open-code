/**
 *
 */
package winterwell.bob.tasks;

import java.io.File;
import java.io.FileNotFoundException;

import com.winterwell.utils.Utils;

/**
 * RSync a local directory to a remote directory or vice versa
 * 
 * https://developer.apple.com/library/mac/#documentation/Darwin/Reference/ManPages/man1/rsync.1.html
 * @author daniel
 *
 */
public class RSyncTask extends ProcessTask {

	String srcPath;
	String destPath;

	private String rargs = "-rLpzP";
	private final boolean delete;

	/**
	 * -r: recursive
	 * -L: copy the contents of symlinks
	 * -p: preserve permissions
	 * -v: verbose
	 * -z: compress for transmission

	 * @param rargs "-rLpz" by default
	 */
	public void setRsyncArgs(String rargs) {
		this.rargs = rargs;
	}
	

	/**
	 * RSync, using -rLpvz and --exclude .svn as its options
	 * -r: recursive
	 * -L: copy the contents of symlinks
	 * -p: preserve permissions
	 * -v: verbose
	 * -z: compress for transmission
	 *
	 * @param srcPath
	 * 			Use a trailing slash if you want the *contents* of the source directory
	 * 			TODO document the subtleties more
	 * @param destPath
	 *          Uses SSH format, e.g. john@stuff.com:~/wherever
	 * @param delete
	 * 			Use --delete flag to remove extraneous files in the destination
	 */
	public RSyncTask(String srcPath, String destPath, boolean delete) {
		super("rsync");		
		this.srcPath = srcPath;
		if ( ! new File(srcPath).exists()) {
			throw Utils.runtime(new FileNotFoundException(srcPath));
		}
		this.destPath = destPath;
		this.delete = delete;
	}
	
	@Override
	public void doTask() throws Exception {
		// what rsync args?
		addArg(rargs);
		if (delete) addArg("--delete");
		addArg("--exclude .svn");
		// Disable host key checks
		addArg("-e \"ssh -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no\"");
		addArg(srcPath);
		addArg(destPath);
		
		super.doTask();
	}

	@Override
	public String toString() {
		return getClass().getSimpleName()+ ":" + this.srcPath +"->" + this.destPath;
	}

}
