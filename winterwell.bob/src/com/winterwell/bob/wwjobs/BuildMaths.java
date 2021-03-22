package com.winterwell.bob.wwjobs;

import java.util.List;

import com.winterwell.bob.BuildTask;
import com.winterwell.bob.tasks.MavenDependencyTask;

public class BuildMaths extends BuildWinterwellProject {

	public BuildMaths() {
		super("winterwell.maths");
		setVersion("1.0.0"); // 22 Mar 2021
		incSrc = true;
	}

	@Override
	public List<BuildTask> getDependencies() {
		List deps = super.getDependencies();
		
		// Maven
		MavenDependencyTask mdt = new MavenDependencyTask();
		mdt.setProjectDir(projectDir);
		if (outDir!=null) {
			mdt.setOutputDirectory(outDir);
		}
		mdt.addDependency("net.sf.trove4j", "trove4j", "3.0.3");
		mdt.addDependency("com.google.guava", "guava", "28.2-jre");
		mdt.addDependency("org.apache.commons", "commons-math3", "3.6.1");
		mdt.addDependency("com.googlecode.matrix-toolkits-java", "mtj", "1.0.4");

		deps.add(mdt);
		
		return deps;
	}
	
}
