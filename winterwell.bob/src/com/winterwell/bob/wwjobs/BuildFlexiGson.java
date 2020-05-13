package com.winterwell.bob.wwjobs;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import com.winterwell.bob.BuildTask;
import com.winterwell.utils.io.FileUtils;

public class BuildFlexiGson extends BuildWinterwellProject {

	public BuildFlexiGson() {
		super(new File(FileUtils.getWinterwellDir(), "flexi-gson"));
//		setIncSrc(true);
//		setScpToWW(true);
	}

	@Override
	public List<BuildTask> getDependencies() {
		return Arrays.asList(new BuildUtils());
	}
	
}
