package jobs;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;

import com.winterwell.bob.BuildTask;
import com.winterwell.bob.tasks.CopyTask;
import com.winterwell.utils.io.FileUtils;

public class BuildStat extends BuildWinterwellProject {

	public BuildStat() {
		super(new File(FileUtils.getWinterwellDir(), "open-code/winterwell.datalog"));
	}	

	@Override
	public Collection<? extends BuildTask> getDependencies() {
		return Arrays.asList(new BuildUtils(), new BuildWeb());
	}


}
