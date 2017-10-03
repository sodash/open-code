package jobs;

import java.io.File;

import com.winterwell.utils.io.FileUtils;

public class BuildUtils extends BuildWinterwellProject {

	public BuildUtils() {
		super("winterwell.utils");		
		incSrc = true;				
		setCompile(true);
	}
	
	// for testing this build script
//	protected File getBinDir() {
//		return new File(projectDir, "bin2");
//	}
	
	@Override
	public void doTask() throws Exception {
		super.doTask();
		
//		// For coinvent
//		File coinvent = new File(FileUtils.getWinterwellDir(), "coinvent/integration+UI/web/WEB-INF/lib");
//		if (coinvent.isDirectory()) {
//			FileUtils.copy(jarFile, coinvent);
//		}
//		File coinvent2 = new File(FileUtils.getWinterwellDir(), "coinvent/simple-server/web/WEB-INF/lib");
//		if (coinvent2.isDirectory()) {
//			FileUtils.copy(jarFile, coinvent2);
//		}
		
//		For Ivan	
//		File juice = new File(FileUtils.getWinterwellDir(), "juice/libs");
//		if (juice.isDirectory()) {
//			FileUtils.copy(jarFile, juice);
//		}
//		
//		File pinpoint = new File(FileUtils.getWinterwellDir(), "pinpoint/lib");
//		if (pinpoint.isDirectory()) {
//			FileUtils.copy(jarFile, pinpoint);
//		}
	}

}
