package jobs;

import java.io.File;
import java.util.Date;

import com.winterwell.bob.BuildTask;
import com.winterwell.bob.tasks.JUnitTask;
import com.winterwell.utils.io.FileUtils;

/**
 * Test everything.
 * @author Daniel
 */
public class TestAll extends BuildTask {

	private File codeDir;
	private File logDir;
	private int good;
	private int bad;

	String[] projectPaths = new String[]{
		"winterwell.maths",
		"winterwell.utils",
		"winterwell.nlp",
		"winterwell.web"
	};
	
	@Override
	public void doTask() throws Exception {
		File wwDir = FileUtils.getWinterwellDir();
		codeDir = new File(wwDir,"code");
		logDir = getLogDir(wwDir);
		for(String pp : projectPaths) {
			testProject(pp);
		}
	}

	/**
	 * Where will we put the logs? $WINTERWELL_HOME/buildLogs/latest, that's where.
	 * It should be a symlink to the real build log directory by this point in the
	 * build script.
	 * FIXME: it would be really nice to be able to pass this in as a parameter.
	 * @param wwDir
	 * @return
	 */
	private File getLogDir(File wwDir) {
		Date now = new Date();
		File logRootDir = new File(wwDir, "buildLogs");
		return new File(logRootDir, "latest");
	}

	private void testProject(String projectPath) {
		File projDir = new File(codeDir, projectPath);
		File outputFile = new File(logDir, projectPath.replace("/", "-") + ".html");
		JUnitTask junit = new JUnitTask(
				null,
				new File(projDir,"bin"),
				outputFile);		
		junit.run();		
		good += junit.getSuccessCount();
		bad += junit.getFailureCount();
	}

}
