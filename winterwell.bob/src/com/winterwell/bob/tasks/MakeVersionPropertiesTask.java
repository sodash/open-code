package com.winterwell.bob.tasks;

import java.io.BufferedWriter;
import java.io.File;
import java.util.Map;
import java.util.Properties;

import com.winterwell.bob.BuildTask;
import com.winterwell.utils.StrUtils;
import com.winterwell.utils.containers.Containers;
import com.winterwell.utils.io.FileUtils;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.time.Time;
import com.winterwell.utils.web.WebUtils2;
import com.winterwell.utils.web.XStreamUtils;

public class MakeVersionPropertiesTask extends BuildTask {

	private File configDir;
	private File appDir;
	private Properties props;


	public MakeVersionPropertiesTask() {
		appDir = FileUtils.getWorkingDirectory();
		configDir = new File("config").getAbsoluteFile();
	}
	
	public MakeVersionPropertiesTask setAppDir(File appDir) {
		this.appDir = appDir;
		return this;
	}
	
	public MakeVersionPropertiesTask setConfigDir(File localConfigDir) {
		this.configDir = localConfigDir;
		return this;
	}
	
	@Override
	protected void doTask() throws Exception {
		if ( ! configDir.exists()) {
			Log.i(LOGTAG, "make directory "+configDir);
			configDir.mkdirs();
		}
		// create the version properties
		File creolePropertiesForSite = new File(configDir, "version.properties");
		if (props == null) {
			props = creolePropertiesForSite.exists()? 
						FileUtils.loadProperties(creolePropertiesForSite)
						: new Properties();
		}
		// set the publish time
		props.setProperty("publishDate", new Time().toISOString());
		try {
			// set info on the git branch
			String branch = GitTask.getGitBranch(appDir);
			props.setProperty("branch", branch);
			// ...and commit IDs
			Map<String, Object> info = GitTask.getLastCommitInfo(appDir);
			for(String k : info.keySet()) {
				Object v = info.get(k);
				if (v==null) continue;
				props.setProperty("lastCommit."+k, v.toString());
			}
		} catch(Throwable ex) {
			// oh well;
			Log.d("git.info.error", ex);
		}
		
		// Who did the push?
		try {
			props.setProperty("origin", WebUtils2.hostname());
		} catch(Exception ex) {
			// oh well
		}

		// save
		BufferedWriter w = FileUtils.getWriter(creolePropertiesForSite);
		props.store(w, null);
		FileUtils.close(w);
	}

	public void setProperties(Properties props) {
		this.props = props;
	}

}
