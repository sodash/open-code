package jobs;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;

import com.winterwell.bob.BuildTask;
import com.winterwell.bob.tasks.MavenDependencyTask;
import com.winterwell.utils.io.FileUtils;

public class BuildDataLog extends BuildWinterwellProject {

	public BuildDataLog() {
		super("winterwell.datalog");
	}	

	@Override
	public void doTask() throws Exception {
		
		MavenDependencyTask mdt = new MavenDependencyTask();
		mdt.addDependency("com.hazelcast","hazelcast", "3.10.2");
		mdt.setOutputDirectory(new File(projectDir, "tmp-lib"));
		mdt.run();
		
		super.doTask();
	}
	
	@Override
	public Collection<? extends BuildTask> getDependencies() {
		return Arrays.asList(new BuildUtils(), new BuildWeb());
	}


}
