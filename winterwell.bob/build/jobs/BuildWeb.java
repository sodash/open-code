package jobs;

import java.io.File;

import com.winterwell.bob.tasks.MavenDependencyTask;
import com.winterwell.utils.io.FileUtils;

public class BuildWeb extends BuildWinterwellProject {

	/**
	 * Build winterwell.web
	 */
	public BuildWeb() {
		super("winterwell.web");
		incSrc=true;	
		setScpToWW(false);
	}
	
	@Override
	public void doTask() throws Exception {
		
		MavenDependencyTask mdt = new MavenDependencyTask();
		mdt.setProjectDir(projectDir);
		if (outDir!=null) mdt.setOutputDirectory(outDir);
		mdt.run();
		
		super.doTask();
				
//		File juice = new File(FileUtils.getWinterwellDir(), "juice/libs");
//		if (juice.isDirectory()) {
//			FileUtils.copy(jarFile, juice);
//		}
	}

}
