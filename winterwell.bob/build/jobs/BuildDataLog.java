package jobs;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;

import com.winterwell.bob.BuildTask;

public class BuildDataLog extends BuildWinterwellProject {

	public BuildDataLog() {
		super("winterwell.datalog");
	}	

	@Override
	public Collection<? extends BuildTask> getDependencies() {
		return Arrays.asList(new BuildUtils(), new BuildWeb());
	}
	
	@Override
	public void doTask() throws Exception {	
		super.doTask();
		doTest();
			
	}

	@Override
	protected File getTestBinDir() {
		return new File(projectDir, "bin.test");
	}

}
