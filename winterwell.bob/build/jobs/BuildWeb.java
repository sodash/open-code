package jobs;

import java.io.File;

import com.winterwell.utils.io.FileUtils;

public class BuildWeb extends BuildWinterwellProject {

	/**
	 * Build winterwell.web
	 */
	public BuildWeb() {
		super("winterwell.web");
		incSrc=true;	
	}
	
	@Override
	public void doTask() throws Exception {
		super.doTask();
		
		// For coinvent
		File coinvent = new File(FileUtils.getWinterwellDir(), "coinvent/integration+UI/web/WEB-INF/lib");
		if (coinvent.isDirectory()) {
			FileUtils.copy(jarFile, coinvent);
		}
		File coinvent2 = new File(FileUtils.getWinterwellDir(), "coinvent/simple-server/web/WEB-INF/lib");
		if (coinvent2.isDirectory()) {
			FileUtils.copy(jarFile, coinvent2);
		}
		
//		File juice = new File(FileUtils.getWinterwellDir(), "juice/libs");
//		if (juice.isDirectory()) {
//			FileUtils.copy(jarFile, juice);
//		}
	}

}
