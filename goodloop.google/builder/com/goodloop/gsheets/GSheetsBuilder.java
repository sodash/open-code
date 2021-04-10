package com.goodloop.gsheets;
import java.io.File;
import java.util.List;

import org.junit.Test;

import com.winterwell.bob.BuildTask;
import com.winterwell.bob.tasks.MavenDependencyTask;
import com.winterwell.bob.wwjobs.BuildWinterwellProject;
/**
 * See {@link GSheetsClient}
 * @author daniel
 *
 */
public class GSheetsBuilder extends BuildWinterwellProject {

	public GSheetsBuilder() {
		super("goodloop.google");
		setIncSrc(true);
		setVersion("0.1.0"); // 10 Apr 2021
	}
	
	@Override
	public List<BuildTask> getDependencies() {
		List<BuildTask> deps = super.getDependencies();

		MavenDependencyTask mdt = new MavenDependencyTask();
		mdt.addDependency("com.google.api-client:google-api-client:1.31.4");
		mdt.addDependency("com.google.oauth-client:google-oauth-client-jetty:1.31.5");
		mdt.addDependency("com.google.apis:google-api-services-sheets:v4-rev612-1.25.0");
//		mdt.addDependency("com.google.api-client:google-api-client:1.30.4");
//		mdt.addDependency("com.google.oauth-client:google-oauth-client-jetty:1.30.6");
//		mdt.addDependency("com.google.apis:google-api-services-sheets:v4-rev581-1.25.0");
		mdt.setIncSrc(true);
		deps.add(mdt);
		
		return deps;
	}

}
