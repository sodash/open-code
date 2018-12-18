package com.winterwell.web.app;

import java.io.File;
import java.util.Collection;

import com.winterwell.bob.BuildTask;
import com.winterwell.bob.wwjobs.BuildWinterwellProject;
import com.winterwell.utils.io.FileUtils;

public class BuildWWAppBase extends BuildWinterwellProject {

	@Override
	public Collection<? extends BuildTask> getDependencies() {
		return super.getDependencies();
//		return Arrays.asList(
//				new BuildUtils(),
//				new BuildBob().setMakeFatJar(false),
//				new BuildWeb(),
//				new BuildFlexiGson(),
////				new BuildYouAgainJavaClient()
//				new WWDependencyTask("youagain-java-client", "com.winterwell.youagain.client.BuildYouAgainJavaClient"),
////				new BuildESJavaClient(),
//				new WWDependencyTask("elasticsearch-java-client", "com.winterwell.es.BuildESJavaClient")
//				);
	}
	
	public BuildWWAppBase() {
		super(new File(FileUtils.getWinterwellDir(), "open-code/winterwell.webappbase"));
		setIncSrc(true);
//		setScpToWW(true);
	}
	

}
