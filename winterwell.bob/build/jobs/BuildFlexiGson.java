package jobs;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;

import com.winterwell.bob.BuildTask;
import com.winterwell.utils.io.FileUtils;

public class BuildFlexiGson extends BuildWinterwellProject {

	public BuildFlexiGson() {
		super(new File(FileUtils.getWinterwellDir(), "flexi-gson"));
		setIncSrc(false);
	}

	@Override
	public Collection<? extends BuildTask> getDependencies() {
		return Arrays.asList(new BuildUtils());
	}
	
	@Override
	public void doTask() throws Exception {
		super.doTask();		
		// copy the file??
	}
}
