package com.winterwell.web.app;

import java.io.File;
import java.util.List;

import com.winterwell.bob.BuildTask;
import com.winterwell.bob.tasks.MavenDependencyTask;
import com.winterwell.bob.wwjobs.BuildWinterwellProject;
import com.winterwell.utils.io.FileUtils;

public class BuildWWAppBase extends BuildWinterwellProject {

	@Override
	public List<BuildTask> getDependencies() {
		List<BuildTask> deps = super.getDependencies();

//		// Lombok - not used anymore for robust builds :'(
//		MavenDependencyTask mdt = new MavenDependencyTask();
//		mdt.addDependency("org.projectlombok:lombok:1.18.10");
//		deps.add(mdt);
		
		return deps;
	}
	
	public BuildWWAppBase() {
		super("winterwell.webappbase");
		setIncSrc(true);
	}
	

}
