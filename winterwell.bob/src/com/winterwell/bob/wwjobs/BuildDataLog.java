package com.winterwell.bob.wwjobs;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import com.winterwell.bob.BuildTask;

@Deprecated // Use the version in datalog for preference.
// But this is kept so it can be used where we have a cyclic dependency:
// DataLog <-> wwappbase 
public class BuildDataLog extends BuildWinterwellProject {

	public BuildDataLog() {
		super("winterwell.datalog");
	}	

	@Override
	public List<BuildTask> getDependencies() {
		return Arrays.asList(new BuildUtils(), new BuildWeb());
	}
	
	@Override
	public void doTask() throws Exception {	
		super.doTask();
	}

	@Override
	protected File getTestBinDir() {
		return new File(projectDir, "bin.test");
	}

}
