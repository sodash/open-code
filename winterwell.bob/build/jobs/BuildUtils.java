package jobs;

import java.io.File;

import com.winterwell.bob.tasks.MavenDependencyTask;
import com.winterwell.utils.io.FileUtils;

public class BuildUtils extends BuildWinterwellProject {

	public BuildUtils() {
		super("winterwell.utils");		
		incSrc = true;				
		setCompile(true);
	}
		
	@Override
	public void doTask() throws Exception {
		MavenDependencyTask mdt = new MavenDependencyTask();
		mdt.setProjectDir(projectDir);
		mdt.run();
		
		super.doTask();				
	}

}
