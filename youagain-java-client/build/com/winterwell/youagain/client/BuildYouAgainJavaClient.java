
package com.winterwell.youagain.client;

import java.io.File;
import java.util.Set;
import java.util.logging.Level;

import org.junit.Test;

import com.winterwell.bob.tasks.CopyTask;
import com.winterwell.bob.tasks.EclipseClasspath;
import com.winterwell.bob.tasks.JarTask;
import com.winterwell.bob.tasks.RSyncTask;
import com.winterwell.bob.tasks.SCPTask;

import com.winterwell.utils.io.FileUtils;

import jobs.BuildBob;
import jobs.BuildDepot;
import jobs.BuildMaths;
import jobs.BuildDataLog;
import jobs.BuildUtils;
import jobs.BuildWeb;
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
