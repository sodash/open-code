package com.winterwell.bob.wwjobs;

import java.util.ArrayList;
import java.util.List;

import com.winterwell.bob.BuildTask;
import com.winterwell.bob.tasks.MavenDependencyTask;

/**
 * Copy pasta from the primary copy which is in Bob's project. 
 * 
 * This copy can be used with bob from the command line.
 * 
 * NB: This file can't be in utils Eclipse source files 'cos Bob isnt (that'd be a circular dependency)
 * @author daniel
 *
 */
public class BuildUtils extends BuildWinterwellProject {

	public BuildUtils() {
		super("winterwell.utils");		
		incSrc = true;				
		setCompile(true);
		setVersion("1.0.7"); // 26 May 2021
	}
	
	@Override
	public List<BuildTask> getDependencies() {
		List<BuildTask> deps = new ArrayList(super.getDependencies());

		// Maven
		MavenDependencyTask mdt = new MavenDependencyTask();
		if (getConfig().clean || getConfig().cleanBefore != null) {
			// NB: recklessly cleaning directories can upset Eclipse
			mdt.setCleanOutputDirectory(true);
		}
		mdt.setProjectDir(projectDir);
		if (outDir!=null) {
			mdt.setOutputDirectory(outDir);
		}
		mdt.addDependency("com.thoughtworks.xstream","xstream", "1.4.15");
		mdt.addDependency("junit","junit","4.12");
		mdt.addDependency("dnsjava","dnsjava","2.1.9"); // Note: not usually used, unless you need DnsUtils
		mdt.setIncSrc(true);
		// DBs -- can we drop these?? If a project needs them they can add
//		mdt.addDependency("org.postgresql", "postgresql", "42.2.13");
//		mdt.addDependency("mysql", "mysql-connector-java", "8.0.19");
//		mdt.addDependency("com.h2database", "h2", "1.4.200");
//		mdt.addDependency("com.jolbox","bonecp","0.8.0.RELEASE"); // NB: includes Guava ans SLF4J
		deps.add(mdt);		
		
		return deps;
	}
		
	@Override
	public void doTask() throws Exception {		
		super.doTask();		
	}

}
