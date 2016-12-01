package jobs;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;

import com.winterwell.bob.BuildTask;
import com.winterwell.utils.io.FileUtils;

public class BuildMaths extends BuildWinterwellProject {

	public BuildMaths() {
		super(new File(FileUtils.getWinterwellDir(), "code/winterwell.maths"));
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
