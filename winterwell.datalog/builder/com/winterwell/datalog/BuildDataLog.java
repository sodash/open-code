
package com.winterwell.datalog;

import java.util.List;

import com.winterwell.bob.BuildTask;
import com.winterwell.bob.tasks.MavenDependencyTask;
import com.winterwell.bob.wwjobs.BuildWinterwellProject;

public class BuildDataLog extends BuildWinterwellProject {

	public BuildDataLog() {
		super("winterwell.datalog");
		setVersion("1.1.0"); // 18 Jun 2021
	}	

	@Override
	public List<BuildTask> getDependencies() {
		List<BuildTask> deps = super.getDependencies();
		
		MavenDependencyTask mdt = new MavenDependencyTask();
//		mdt.addDependency("org.postgresql", "postgresql", "42.2.11");
		
		// Browser Agent analysis
//		mdt.addDependency("ua_parser", "ua-parser", "1.3.0"); // causes an error from Maven?? So jar is stored in /lib
		// snakeyaml is needed by ua_parser -- see LGServletTest.testBrowserType()
		// https://mvnrepository.com/artifact/org.yaml/snakeyaml 
		mdt.addDependency("org.yaml", "snakeyaml", "1.26");
		deps.add(mdt);
		
		return deps;
	}	

}
