
package com.winterwell.datalog;

import java.io.File;

import com.winterwell.bob.wwjobs.BuildWinterwellProject;

public class BuildDataLog extends BuildWinterwellProject {

	public BuildDataLog() {
		super("winterwell.datalog");
	}	

//	@Override
//	public Collection<? extends BuildTask> getDependencies() {
//		return Arrays.asList(
//				new BuildUtils(), 
//				new BuildWeb(),
//				new BuildMaths(),
//				new BuildWWAppBase()
//				);
//	}
	
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
