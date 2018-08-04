package jobs;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.winterwell.bob.BuildTask;
import com.winterwell.bob.tasks.MavenDependencyTask;
import com.winterwell.utils.io.FileUtils;
import com.winterwell.utils.log.Log;

public class BuildUtils extends BuildWinterwellProject {

	public BuildUtils() {
		super("winterwell.utils");		
		incSrc = true;				
		setCompile(true);
	}
	
	@Override
	public Collection<? extends BuildTask> getDependencies() {
		List<BuildTask> deps = new ArrayList(super.getDependencies());

		// Maven
		MavenDependencyTask mdt = new MavenDependencyTask();
		mdt.setProjectDir(projectDir);
		if (outDir!=null) {
			mdt.setOutputDirectory(outDir);
		}
		mdt.addDependency("com.thoughtworks.xstream","xstream", "1.4.10");
		mdt.addDependency("junit","junit","4.12");
		mdt.addDependency("com.jolbox","bonecp","0.8.0.RELEASE");
		deps.add(mdt);
		
		return deps;
	}
		
	@Override
	public void doTask() throws Exception {		
		super.doTask();				
	}

}
