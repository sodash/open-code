package com.winterwell.bob.tasks;

import java.io.File;
import java.io.IOException;

import com.winterwell.bob.BuildTask;
import com.winterwell.utils.io.FileUtils;
import com.winterwell.utils.log.Log;
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

	boolean stashLocalChanges;
	
	@Override
	protected void doTask() throws Exception {
		Log.d(LOGTAG, dir+" < "+this);
		// clone or pull
		if (dir.isDirectory()) {
			// stash?
			if (stashLocalChanges) {
				GitTask gt0 = new GitTask(GitTask.STASH, dir);
				gt0.run();
				gt0.close();
			}
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
		ForkJVMTask childBob = new ForkJVMTask();
		File pd;
		if (projectSubDir!=null) {			
			pd = projectSubDir;
		} else {
			pd = dir;
		}
		childBob.setDir(pd);
		// Do it!
		childBob.run();
	}

	public GitBobProjectTask setSubDir(String subdir) {
		projectSubDir = new File(dir, subdir.toString());
		return this;
	}

	public static File getGitBobDir() {
		return new File(FileUtils.getWinterwellDir(),"bobwarehouse");
	}

}
