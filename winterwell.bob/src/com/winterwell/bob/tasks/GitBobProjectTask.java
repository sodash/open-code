package com.winterwell.bob.tasks;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import com.winterwell.bob.Bob;
import com.winterwell.bob.BuildTask;
import com.winterwell.utils.Printer;
import com.winterwell.utils.Proc;
import com.winterwell.utils.ReflectionUtils;
import com.winterwell.utils.Utils;
import com.winterwell.utils.io.FileUtils;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.time.Dt;
import com.winterwell.utils.time.TUnit;
/**
 * @tested {@link GitBobProjectTaskTest}
 * @author daniel
 *
 */
public class GitBobProjectTask extends BuildTask {

	@Override
	public String toString() {
		return "GitBobProjectTask [gitUrl=" + gitUrl + ", dir=" + dir + ", projectSubDir=" + projectSubDir + "]";
	}

	/**
	 * As per .git/config -- e.g. git@github.com:sodash/open-code
	 */
	String gitUrl;
	

	/**
	 * target (local) directory
	 */
	File dir;
	
	/**
	 * As per .git/config -- e.g. git@github.com:sodash/open-code
	 * target (local) directory
	 */
	public GitBobProjectTask(String gitUrl, File dir) {
		this.gitUrl = gitUrl;
		this.dir = dir;
		// dependencies shouldnt need rebuilding all the time
		setSkipGap(TUnit.DAY.dt);
	}
	
	/**
	 * If the project is a sub-dir of the main repo dir. Usually null
	 */
	File projectSubDir;

	@Override
	protected void doTask() throws Exception {
		Log.d(LOGTAG, dir+" < "+this);
		// clone or pull
		if (dir.isDirectory()) {
			// pull
			GitTask gt = new GitTask(GitTask.PULL, dir);
			gt.run();
			gt.close();
		} else {
			assert ! dir.isFile() : dir;
			// clone
			boolean ok = dir.getAbsoluteFile().mkdirs();
			if ( ! ok) throw new IOException("Could not make directory "+dir);
			GitTask gt = new GitTask(GitTask.CLONE, dir);
			gt.addArg(gitUrl);
			gt.addArg(dir.getAbsolutePath());
			gt.run();
			gt.close();
		}
		
		// build				
		Properties props = System.getProperties();
//		Printer.out(props.keySet());
		String cp = props.getProperty("java.class.path");
		if (Utils.isBlank(cp)) {
			Printer.out(props);
			cp = "bob-all.jar"; // hail mary
			Log.d(LOGTAG, "GUESS cp "+cp);
		} else {
			Log.d(LOGTAG, "Got cp "+cp);
		}
		
//		// FIXME pass on Bob settings like -clean
		// BUT we dont want to rebuild utils n times in one build
		String options = "";
//		if (Bob.getSingleton().getSettings().skippingOff) {
//			options += " -clean";
//		}
		
		// child call to Bob
		ProcessTask proc = null;
		try {
			proc = new ProcessTask("java -cp "+cp+" com.winterwell.bob.Bob"+options, new Dt(15, TUnit.MINUTE).getMillisecs());
	//			Proc proc = new Proc();
			File pd;
			if (projectSubDir!=null) {			
				pd = projectSubDir;
			} else {
				pd = dir;
			}
			proc.setDirectory(projectSubDir);
			proc.setEcho(true);
			Log.i(LOGTAG, "Child-Bob: "+proc.getCommand()+" \r\n[in dir "+pd+"]");
			proc.run();
		} finally {
			long pid = proc.getProcessID();
			if (proc!=null) proc.close();
			Log.d(LOGTAG, "closed process "+pid);
		}
	}

	public GitBobProjectTask setSubDir(String subdir) {
		projectSubDir = new File(dir, subdir.toString());
		return this;
	}

	public static File getGitBobDir() {
		return new File(FileUtils.getWinterwellDir(),"bobwarehouse");
	}

}
