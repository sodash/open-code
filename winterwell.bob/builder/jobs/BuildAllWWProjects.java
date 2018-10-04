package jobs;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import com.winterwell.bob.BuildTask;

/**
 * Well, not all. But all the basics.
 * This does not cover ES Java Client, or FlexiGson
 * @author daniel
 *
 */
public class BuildAllWWProjects extends BuildTask {

	@Override
	public Collection<? extends BuildTask> getDependencies() {
		return Arrays.asList(
								
				);
	}
	
	@Override
	protected void doTask() throws Exception {
		List<BuildWinterwellProject> projects = Arrays.asList(
				new BuildUtils(),
				new BuildWeb(),
				new BuildMaths(),
				new BuildNLP(),
				new BuildDepot(),
				new BuildBob()
				);
		for (BuildWinterwellProject bwp : projects) {
			bwp.setScpToWW(true);
			bwp.run();
		}		
	}

}
