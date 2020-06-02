
package com.winterwell.datalog;

import java.io.File;
import java.util.List;

import com.winterwell.bob.BuildTask;
import com.winterwell.bob.tasks.MavenDependencyTask;
import com.winterwell.bob.wwjobs.BuildWinterwellProject;

public class BuildDataLog extends BuildWinterwellProject {

	public BuildDataLog() {
		super("winterwell.datalog");
	}	

	@Override
	public List<BuildTask> getDependencies() {
		List<BuildTask> deps = super.getDependencies();
		
		MavenDependencyTask mdt = new MavenDependencyTask();
//		mdt.addDependency("org.postgresql", "postgresql", "42.2.11");
//		mdt.addDependency("ua_parser", "ua-parser", "1.3.0"); // causes an error from Maven??
		// https://mvnrepository.com/artifact/org.yaml/snakeyaml
		mdt.addDependency("org.yaml", "snakeyaml", "1.26");

		deps.add(mdt);
		
		return deps;
	}
	
	@Override
	public void doTask() throws Exception {	
		super.doTask();
		doTest();
			
	}

	@Override
	protected File getTestBinDir() {
		return new File(projectDir, "bin.test");
	}

}
