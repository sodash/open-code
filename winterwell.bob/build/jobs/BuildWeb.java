package jobs;

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
		mdt.addDependency("org.eclipse.jetty", "jetty-server", "9.4.8.v20171121");
		mdt.addDependency("org.eclipse.jetty","jetty-util","9.4.8.v20171121");
		mdt.addDependency("org.eclipse.jetty","jetty-util-ajax","9.4.8.v20171121");
		mdt.addDependency("org.eclipse.jetty", "jetty-servlet", "9.4.8.v20171121");
		mdt.addDependency("javax.mail", "mail", "1.5.0-b01");
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
