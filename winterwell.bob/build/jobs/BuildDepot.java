package jobs;

import java.util.Arrays;
import java.util.Collection;

import com.winterwell.bob.BuildTask;

public class BuildDepot extends BuildWinterwellProject {

	public BuildDepot() {
		super("winterwell.depot");
	}	

	@Override
	public Collection<? extends BuildTask> getDependencies() {
		return Arrays.asList(new BuildUtils(), new BuildWeb());
	}


}
