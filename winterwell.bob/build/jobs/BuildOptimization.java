package jobs;

import java.util.Arrays;
import java.util.Collection;

import com.winterwell.bob.BuildTask;

public class BuildOptimization extends BuildWinterwellProject {

	public BuildOptimization() {
		super("winterwell.optimization");
	}

	@Override
	public Collection<? extends BuildTask> getDependencies() {
		return Arrays.asList(new BuildUtils(), new BuildMaths());
	}


}
