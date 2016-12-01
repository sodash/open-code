package jobs;

import java.io.File;

import com.winterwell.utils.io.FileUtils;

/**
 * Should really be done using Maven. But this is handy for when you haven't got maven.
 * @author daniel
 *
 */
public class BuildJGeoPlanet extends BuildWinterwellProject {

	public BuildJGeoPlanet() {
		super(new File(FileUtils.getWinterwellDir(), "jgeoplanet"));
		incSrc = true;
	}
	
	@Override
	protected File getSrcDir() {
		File d = new File(projectDir, "src/main/java");
		return d;
	}
	
	@Override
	public void doTask() throws Exception {
		super.doTask();
		
		File pinpoint = new File(FileUtils.getWinterwellDir(), "pinpoint/lib");
		if (pinpoint.isDirectory()) {
			FileUtils.copy(jarFile, pinpoint);
		}
	}

}
