package com.goodloop.gsheets;
import org.junit.Test;

import com.winterwell.bob.BuildTask;
import com.winterwell.bob.tasks.MavenDependencyTask;
/**
 * See {@link GSheetsClient}
 * @author daniel
 *
 */
public class GSheetsBuilder extends BuildTask {

	@Override
	protected void doTask() throws Exception {
		MavenDependencyTask mdt = new MavenDependencyTask();
		mdt.addDependency("com.google.api-client:google-api-client:1.30.4");
		mdt.addDependency("com.google.oauth-client:google-oauth-client-jetty:1.30.6");
		mdt.addDependency("com.google.apis:google-api-services-sheets:v4-rev581-1.25.0");
		mdt.setIncSrc(true);
		mdt.run();
	}

}
