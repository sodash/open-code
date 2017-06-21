package jobs;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import com.winterwell.bob.BuildTask;
import com.winterwell.utils.io.FileUtils;

public class BuildZonefox extends BuildTask {

	@Override
	protected void doTask() throws Exception {
		// Try to find the right directory
		File zfDir = new File(FileUtils.getWinterwellDir(), "zonefox");
		if ( ! zfDir.isDirectory()) {
			zfDir = new File(FileUtils.getWinterwellDir(), "ilab-ww");
		}
		File zflibs = new File(zfDir, "libs");
		assert zflibs.isDirectory() : zflibs;
		// NB: all the below are needed. 
		// But to avoid polluting git with lots of binary versions, I often comment some out.
		List<BuildWinterwellProject> projects = Arrays.asList(				
//				new BuildWeb(),
//				new BuildMaths()
//				new BuildNLP(),
//				new BuildDepot(),
				new BuildDataLog()
//				new BuildBob(),
//				new BuildFlexiGson(),
//				new BuildESClient(),
//				new BuildUtils()
				);
		for (BuildWinterwellProject buildWinterwellProject : projects) {
			buildWinterwellProject.setIncSrc(true);
			buildWinterwellProject.run();
			File jar = buildWinterwellProject.getJar();
			FileUtils.copy(jar, zflibs);
		}
	}

}
