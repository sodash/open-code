
package com.winterwell.youagain.client;

import java.io.File;

import com.winterwell.utils.io.FileUtils;

import jobs.BuildWinterwellProject;


public class BuildYouAgainJavaClient extends BuildWinterwellProject {

	public BuildYouAgainJavaClient() {
		super(new File(FileUtils.getWinterwellDir(), "open-code/youagain-java-client"));
		setIncSrc(true);
		setVersion("0.2.0");
	}

	@Override
	public void doTask() throws Exception {
		super.doTask();
	}

}
