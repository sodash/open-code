package com.winterwell.web.app;

import java.util.List;

import com.winterwell.bob.BuildTask;
import com.winterwell.bob.wwjobs.BuildWinterwellProject;

public class BuildWWAppBase extends BuildWinterwellProject {

	
	public BuildWWAppBase() {
		super("winterwell.webappbase");
		setIncSrc(true);
	}
	

}
