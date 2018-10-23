package com.winterwell.bob.wwjobs;

import java.util.ArrayList;
import java.util.Collection;

import com.winterwell.bob.BuildTask;
import com.winterwell.bob.tasks.MavenDependencyTask;

public class BuildWeb extends BuildWinterwellProject {

	@Override
	public Collection<? extends BuildTask> getDependencies() {
		ArrayList deps = new ArrayList(super.getDependencies());

		// utils
		deps.add(new BuildUtils());
		
		// maven
		MavenDependencyTask mdt = new MavenDependencyTask();
		String jettyVersion = "9.4.12.RC2"; // see https://mvnrepository.com/artifact/org.eclipse.jetty/jetty-server
		mdt.addDependency("org.eclipse.jetty", "jetty-server", jettyVersion);
		mdt.addDependency("org.eclipse.jetty","jetty-util",jettyVersion);
		mdt.addDependency("org.eclipse.jetty","jetty-util-ajax",jettyVersion);
		mdt.addDependency("org.eclipse.jetty", "jetty-servlet",jettyVersion);
		
		mdt.addDependency("com.sun.mail", "javax.mail", "1.6.1"); //"1.5.0-b01");
		mdt.addDependency("com.sun.mail", "gimap", "1.6.1");
//		mdt.addDependency("javax.mail", "javax.mail-api", "1.6.1"); //"1.5.0-b01");
//		mdt.addDependency("javax.mail", "imap", "1.4"); //"1.5.0-b01");
		mdt.setIncSrc(true); // we like source code
//		mdt.setForceUpdate(true);
		mdt.setProjectDir(projectDir);
		if (outDir!=null) {
			mdt.setOutputDirectory(outDir);
		}
		deps.add(mdt);
		
		return deps;
	}
	
	/**
	 * Build winterwell.web
	 */
	public BuildWeb() {
		super("winterwell.web");
		
		setCompile(true);
		setIncSrc(true);
		setScpToWW(false);
	}
	
	@Override
	public void doTask() throws Exception {		
		super.doTask();				
	}

}
