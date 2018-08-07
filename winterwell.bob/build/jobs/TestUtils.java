package jobs;

import java.io.File;

import com.winterwell.bob.BuildTask;
import com.winterwell.bob.tasks.JUnitTask;
import com.winterwell.utils.gui.GuiUtils;

public class TestUtils extends BuildTask {

	@Override
	protected void doTask() throws Exception {
		BuildUtils bu = new BuildUtils();
		File projDir = bu.projectDir;
		File outputFile = new File(projDir, "test-results/"+bu.projectName+".html");
		GuiUtils.setInteractive(false);
		JUnitTask junit = new JUnitTask(
				bu.getSrcDir(),
				bu.getTestBinDir(),
				outputFile);		
		junit.run();		
		int good = junit.getSuccessCount();
		int bad = junit.getFailureCount();
	}

}
