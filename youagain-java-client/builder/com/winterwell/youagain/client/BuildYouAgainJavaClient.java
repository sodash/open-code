
package com.winterwell.youagain.client;

import java.io.File;

import com.winterwell.bob.tasks.MavenDependencyTask;
import com.winterwell.utils.io.FileUtils;

import jobs.BuildWinterwellProject;


public class BuildYouAgainJavaClient extends BuildWinterwellProject {

	public BuildYouAgainJavaClient() {
		super(new File(FileUtils.getWinterwellDir(), "open-code/youagain-java-client"));
		setIncSrc(true);
		setVersion("0.3.0");
	}

	@Override
	public void doTask() throws Exception {
		MavenDependencyTask mdt = new MavenDependencyTask();		
		mdt.addDependency("com.auth0", "java-jwt", "3.4.0");
		mdt.setOutputDirectory(new File(projectDir, "dependencies"));
		mdt.setIncSrc(true);
//		mdt.setForceUpdate(true);
		mdt.run();
		
		super.doTask();
	}

}
