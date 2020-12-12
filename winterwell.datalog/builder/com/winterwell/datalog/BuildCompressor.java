
package com.winterwell.datalog;

import com.winterwell.bob.tasks.WinterwellProjectFinder;
import com.winterwell.bob.wwjobs.BuildWinterwellProject;

public class BuildCompressor extends BuildWinterwellProject {

	public static void main(String[] args) throws Exception {
		BuildCompressor b = new BuildCompressor();
		b.doTask();
	}
	
	public BuildCompressor() {
		super(new WinterwellProjectFinder().apply("winterwell.datalog"), "datalog.compressor");
		setMainClass("com.winterwell.datalog.server.CompressDataLogIndexMain");
	}	

}
