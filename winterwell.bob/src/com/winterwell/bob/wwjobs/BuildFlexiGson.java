package com.winterwell.bob.wwjobs;

import java.util.Arrays;
import java.util.List;

import com.winterwell.bob.BuildTask;

public class BuildFlexiGson extends BuildWinterwellProject {

	public BuildFlexiGson() {
		super("flexi-gson");
	}

	@Override
	public List<BuildTask> getDependencies() {
		return Arrays.asList(new BuildUtils());
	}
	
}
