package jobs;

import java.io.File;

import com.winterwell.bob.tasks.MavenDependencyTask;
import com.winterwell.utils.io.FileUtils;
import com.winterwell.utils.log.Log;

public class BuildUtils extends BuildWinterwellProject {

	public BuildUtils() {
		super("winterwell.utils");		
		incSrc = true;				
		setCompile(true);
	}
		
	@Override
	public void doTask() throws Exception {
		// get the jars utils needs
		try {
			MavenDependencyTask mdt = new MavenDependencyTask();
			mdt.setProjectDir(projectDir);
			mdt.run();
		} catch(Throwable ex) {
			Log.e(LOGTAG, ex);
			// oh well?!
		}
		
		super.doTask();				
	}

}
