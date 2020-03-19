
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
		mdt.addDependency(postgresql)
		mdt.addDependency(uaparser)
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
