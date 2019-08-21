package com.winterwell.bob.wwjobs;

import java.util.ArrayList;
import java.util.List;

import com.winterwell.bob.BuildTask;
import com.winterwell.bob.tasks.MavenDependencyTask;

public class BuildUtils extends BuildWinterwellProject {

	public BuildUtils() {
		super("winterwell.utils");		
		incSrc = true;				
		setCompile(true);
	}
	
	@Override
	public List<BuildTask> getDependencies() {
		List<BuildTask> deps = new ArrayList(super.getDependencies());

		// Maven
		MavenDependencyTask mdt = new MavenDependencyTask();
		mdt.setCleanOutputDirectory(true);
		mdt.setProjectDir(projectDir);
		if (outDir!=null) {
			mdt.setOutputDirectory(outDir);
		}
		mdt.addDependency("com.thoughtworks.xstream","xstream", "1.4.10");
		mdt.addDependency("junit","junit","4.12");
		mdt.addDependency("dnsjava","dnsjava","2.1.9"); // Note: not usually used, unless you need DnsUtils
//		mdt.addDependency("com.jolbox","bonecp","0.8.0.RELEASE"); // NB: includes Guava ans SLF4J
		deps.add(mdt);		
		
		return deps;
	}
		
	@Override
	public void doTask() throws Exception {		
		super.doTask();		
	}

}
