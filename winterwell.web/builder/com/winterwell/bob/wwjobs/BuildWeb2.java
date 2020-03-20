package com.winterwell.bob.wwjobs;

import java.util.ArrayList;
import java.util.List;

import com.winterwell.bob.BuildTask;
import com.winterwell.bob.tasks.MavenDependencyTask;
/**
 * A copy of BuildWeb in the bob project itself. This can be used from the command line.
 * @author daniel
 *
 */
public class BuildWeb2 extends BuildWinterwellProject {

	@Override
	public List<BuildTask> getDependencies() {
		ArrayList deps = new ArrayList(super.getDependencies());

		// utils
		deps.add(new BuildUtils());
		
		// maven
		MavenDependencyTask mdt = new MavenDependencyTask();
		String jettyVersion = "9.4.24.v20191120"; // see https://mvnrepository.com/artifact/org.eclipse.jetty/jetty-server
		mdt.addDependency("org.eclipse.jetty", "jetty-server", jettyVersion);
		mdt.addDependency("org.eclipse.jetty","jetty-util",jettyVersion);
		mdt.addDependency("org.eclipse.jetty","jetty-util-ajax",jettyVersion);
		mdt.addDependency("org.eclipse.jetty", "jetty-servlet",jettyVersion);
		
		mdt.addDependency("com.sun.mail", "javax.mail", "1.6.2"); //"1.5.0-b01");
		mdt.addDependency("com.sun.mail", "gimap", "1.6.4"); // used??
//		mdt.addDependency("javax.mail", "javax.mail-api", "1.6.1"); //"1.5.0-b01");
//		mdt.addDependency("javax.mail", "imap", "1.4"); //"1.5.0-b01");
		
		mdt.addDependency("eu.medsea.mimeutil", "mime-util", "1.3"); // latest is "2.1.3" but that seems borked
		mdt.addDependency("org.apache.httpcomponents", "httpclient", "4.5.10");
		mdt.addDependency("org.ccil.cowan.tagsoup", "tagsoup", "1.2.1");
		mdt.addDependency("commons-fileupload","commons-fileupload","1.4");
		mdt.addDependency("commons-io", "commons-io", "2.6");
		// https://mvnrepository.com/artifact/org.apache.commons/commons-lang3
		mdt.addDependency("org.apache.commons","commons-lang3","3.9");
		
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
	public BuildWeb2() {
		super("winterwell.web");
		
//		setCompile(true);
//		setIncSrc(true);
//		setScpToWW(true);
	}	

}
