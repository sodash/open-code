
package com.winterwell.datalog;

import java.util.List;

import com.winterwell.bob.BuildTask;
import com.winterwell.bob.wwjobs.BuildWinterwellProject;

public class BuildCompressDataLogIndex extends BuildWinterwellProject {

	public BuildCompressDataLogIndex() {
		super(guessProjectDir("winterwell.datalog"), "datalog.compressor");
		setMainClass("com.winterwell.datalog.server.CompressDataLogIndexMain");
	}	

	@Override
	public List<BuildTask> getDependencies() {
		List<BuildTask> deps = super.getDependencies();		
		return deps;
	}	

}
