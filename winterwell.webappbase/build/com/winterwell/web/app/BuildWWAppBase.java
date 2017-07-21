package com.winterwell.web.app;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;

import com.winterwell.bob.BuildTask;
import com.winterwell.es.BuildESJavaClient;
import com.winterwell.utils.io.FileUtils;

import jobs.BuildBob;
import jobs.BuildDataLog;
import jobs.BuildFlexiGson;
import jobs.BuildMaths;
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
				new BuildFlexiGson()
				);
	}
	
	public BuildWWAppBase() {
		super(new File(FileUtils.getWinterwellDir(), "open-code/winterwell.webappbase"));
	}

}
