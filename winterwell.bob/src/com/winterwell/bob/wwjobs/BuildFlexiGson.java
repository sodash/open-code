package com.winterwell.bob.wwjobs;

import java.util.Arrays;
import java.util.List;

import com.winterwell.bob.BuildTask;
import com.winterwell.web.app.KServerType;

public class BuildFlexiGson extends BuildWinterwellProject {

	public BuildFlexiGson() {
		super("flexi-gson");

		// HACK edit if releasing
		if (BuildHacks.getServerType()==KServerType.LOCAL) {
			setScpToWW(true);
		}
	}

	@Override
	public List<BuildTask> getDependencies() {
		return Arrays.asList(new BuildUtils());
	}
	
}
