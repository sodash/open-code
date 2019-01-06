package com.winterwell.bob.wwjobs;

import java.util.Arrays;
import java.util.List;

import com.winterwell.bob.BuildTask;

public class BuildOptimization extends BuildWinterwellProject {

	public BuildOptimization() {
		super("winterwell.optimization");
	}

	@Override
	public List<BuildTask> getDependencies() {
		return Arrays.asList(new BuildUtils(), new BuildMaths());
	}


}
