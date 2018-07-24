package jobs;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

import com.winterwell.bob.BuildTask;
import com.winterwell.bob.tasks.MavenDependencyTask;
import com.winterwell.utils.io.FileUtils;

public class BuildWeb extends BuildWinterwellProject {

	@Override
	public Collection<? extends BuildTask> getDependencies() {
		ArrayList deps = new ArrayList(super.getDependencies());

		// utils
		deps.add(new BuildUtils());
		
		// maven
		MavenDependencyTask mdt = new MavenDependencyTask();
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
		incSrc=true;	
		setScpToWW(false);
	}
	
	@Override
	public void doTask() throws Exception {		
		super.doTask();				
	}

}
