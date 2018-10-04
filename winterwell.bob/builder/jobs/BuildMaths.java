package jobs;

import java.util.Arrays;
import java.util.Collection;

import com.winterwell.bob.BuildTask;

public class BuildMaths extends BuildWinterwellProject {

	public BuildMaths() {
		super("winterwell.maths");
		incSrc = false;
	}

	@Override
	public Collection<? extends BuildTask> getDependencies() {
		return Arrays.asList(new BuildUtils());
	}
	
	@Override
	public void doTask() throws Exception {
		super.doTask();		
	}
}
