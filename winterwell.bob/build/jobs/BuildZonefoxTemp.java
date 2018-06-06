package jobs;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import com.winterwell.bob.BuildTask;
import com.winterwell.utils.io.FileUtils;

import jobs.BuildDepot;
import jobs.BuildFlexiGson;
import jobs.BuildMaths;
import jobs.BuildUtils;
import jobs.BuildWinterwellProject;

public class BuildZonefoxTemp extends BuildTask {

	@Override
	protected void doTask() throws Exception {
		File zflibs = new File(FileUtils.getWinterwellDir(), "zonefox/libs");
		assert zflibs.isDirectory() : zflibs;
		List<BuildWinterwellProject> projects = Arrays.asList(				
//				new BuildWeb(),
//				new BuildMaths(),
//				new BuildNLP(),
//				new BuildDepot(),
//				new BuildDataLog(),
//				new BuildBob(),
//				new BuildFlexiGson(),
//				new BuildUtils(),
				new BuildESJavaClient(),
				null
				);
		for (BuildWinterwellProject buildWinterwellProject : projects) {
			if (buildWinterwellProject==null) continue;
			buildWinterwellProject.setIncSrc(true);
			buildWinterwellProject.run();
			File jar = buildWinterwellProject.getJar();
			FileUtils.copy(jar, zflibs);
		}
	}

}
