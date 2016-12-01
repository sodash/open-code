package jobs;

import java.util.Arrays;
import java.util.Collection;

import com.winterwell.bob.BuildTask;

public class BuildAllWWProjects extends BuildTask {

	@Override
	public Collection<? extends BuildTask> getDependencies() {
		return Arrays.asList(
				new BuildUtils(),
				new BuildWeb(),
				new BuildMaths(),
				new BuildNLP(),
				new BuildDepot(),
				new BuildBob()
				);
	}
	
	@Override
	protected void doTask() throws Exception {	
	}

}
