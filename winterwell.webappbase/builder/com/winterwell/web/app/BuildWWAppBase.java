package com.winterwell.web.app;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;

import com.winterwell.bob.BuildTask;
import com.winterwell.es.BuildESJavaClient;
import com.winterwell.utils.io.FileUtils;
import com.winterwell.youagain.client.BuildYouAgainJavaClient;

import jobs.BuildBob;
import jobs.BuildFlexiGson;
import jobs.BuildUtils;
import jobs.BuildWeb;
import jobs.BuildWinterwellProject;

public class BuildWWAppBase extends BuildWinterwellProject {

	@Override
	public Collection<? extends BuildTask> getDependencies() {
		return Arrays.asList(
				new BuildUtils(),
				new BuildBob(),
				new BuildWeb(),
				new BuildESJavaClient(),
				new BuildFlexiGson(),
				new BuildYouAgainJavaClient()
				);
	}
	
	public BuildWWAppBase() {
		super(new File(FileUtils.getWinterwellDir(), "open-code/winterwell.webappbase"));
	}

}
