
package jobs;

import java.io.File;

import com.winterwell.bob.tasks.MavenDependencyTask;
import com.winterwell.utils.io.FileUtils;


public class BuildYouAgainJavaClient extends BuildWinterwellProject {

	public BuildYouAgainJavaClient() {
		super(new File(FileUtils.getWinterwellDir(), "open-code/youagain-java-client"));
		setIncSrc(true);
		setVersion("0.2.1");
	}

	@Override
	public void doTask() throws Exception {
		MavenDependencyTask mdt = new MavenDependencyTask();
		// https://mvnrepository.com/artifact/org.bitbucket.b_c/jose4j
		mdt.addDependency("org.bitbucket.b_c", "jose4j", "0.6.3");
		mdt.setOutputDirectory(new File(projectDir, "dependencies"));
		mdt.setIncSrc(true);
		mdt.setForceUpdate(true);
		mdt.run();
		
		super.doTask();
	}

}
