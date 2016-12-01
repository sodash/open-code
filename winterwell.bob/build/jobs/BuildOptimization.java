package jobs;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;

import com.winterwell.bob.BuildTask;
import com.winterwell.utils.io.FileUtils;

public class BuildOptimization extends BuildWinterwellProject {

	public BuildOptimization() {
		super(new File(FileUtils.getWinterwellDir(), "open-code/winterwell.optimization"));
	}

	@Override
	public Collection<? extends BuildTask> getDependencies() {
		return Arrays.asList(new BuildUtils(), new BuildMaths());
	}


}
