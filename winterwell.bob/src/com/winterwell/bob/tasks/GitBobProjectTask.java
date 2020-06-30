package com.winterwell.bob.tasks;

import java.io.File;
import java.io.IOException;

import com.winterwell.bob.BuildTask;
import com.winterwell.bob.wwjobs.BuildHacks;
import com.winterwell.utils.io.FileUtils;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.time.TUnit;
import com.winterwell.web.app.KServerType;
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
		resetLocalChanges = BuildHacks.getServerType() != KServerType.LOCAL;
	}
	
	/**
	 * If the project is a sub-dir of the main repo dir. Usually null
	 */
	File projectSubDir;

	boolean stashLocalChanges;
	/**
	 * This is set true for non-local. It helps ensure the git pull will work.
	 * It does delete local edits!
	 */
	boolean resetLocalChanges;
	
	@Override
	protected void doTask() throws Exception {
		Log.d(LOGTAG, dir+" < "+this);
		// clone or pull
		if (dir.isDirectory()) {
			// stash?
			if (stashLocalChanges) {
				GitTask gt0 = new GitTask(GitTask.STASH, dir);
				gt0.setDepth(getDepth()+1);				
				gt0.run();
				gt0.close();
			}
			// reset first? a harder version of stash!
			if (resetLocalChanges) {
				Log.d(LOGTAG, "git reset --hard (because not a local dev box)");
				GitTask gr = new GitTask(GitTask.RESET, dir);
				gr.addArg("--hard FETCH_HEAD");
				gr.run();
				gr.close();
			}
			// pull
			GitTask gt = new GitTask(GitTask.PULL, dir);
			gt.setDepth(getDepth()+1);
			gt.run();
			gt.close();						
		} else {
			assert ! dir.isFile() : dir;
			// clone
			boolean ok = dir.getAbsoluteFile().mkdirs();
			if ( ! ok) throw new IOException("Could not make directory "+dir);
			GitTask gt = new GitTask(GitTask.CLONE, dir);
			gt.setDepth(getDepth()+1);
			gt.addArg("--depth 1");
			gt.addArg(gitUrl);
			gt.addArg(dir.getAbsolutePath());
			gt.run();
			gt.close();
		}
		
		// build				
		ForkJVMTask childBob = new ForkJVMTask();
		childBob.setDepth(getDepth()+1);
		File pd;
		if (projectSubDir!=null) {			
			pd = projectSubDir;
		} else {
			pd = dir;
		}
		childBob.setDir(pd);
		// Do it!
		childBob.run();
		childBob.close();
	}

	public GitBobProjectTask setSubDir(String subdir) {
		projectSubDir = new File(dir, subdir.toString());
		return this;
	}

}
