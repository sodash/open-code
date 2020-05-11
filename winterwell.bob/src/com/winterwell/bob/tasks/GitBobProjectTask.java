package com.winterwell.bob.tasks;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

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

	/**
	 * As per .git/config -- e.g. git@github.com:sodash/open-code
	 */
	String gitUrl;
	
	File dir;
	
	public GitBobProjectTask(String gitUrl, File dir) {
		this.gitUrl = gitUrl;
		this.dir = dir;
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
		}
		Proc proc = new Proc("java -cp "+cp+" com.winterwell.bob.Bob");
		if (projectSubDir!=null) {
			proc.setDirectory(projectSubDir);
		} else {
			proc.setDirectory(dir);
		}
		proc.setEcho(true);
		proc.start();
		proc.waitFor(new Dt(15, TUnit.MINUTE));
		Log.d(LOGTAG, proc.getError());
		Log.d(LOGTAG, proc.getOutput());
	}

	public GitBobProjectTask setSubDir(String subdir) {
		projectSubDir = new File(dir, subdir.toString());
		return this;
	}

}
