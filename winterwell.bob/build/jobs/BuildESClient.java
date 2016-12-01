package jobs;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;

import com.winterwell.bob.BuildTask;
import com.winterwell.bob.tasks.CopyTask;
import com.winterwell.utils.io.FileUtils;

public class BuildESClient extends BuildWinterwellProject {

	public BuildESClient() {
		super(new File(FileUtils.getWinterwellDir(), "elasticsearch-java-client"));
	}	

	@Override
	public Collection<? extends BuildTask> getDependencies() {
		return Arrays.asList(new BuildUtils(), new BuildWeb());
	}


}
