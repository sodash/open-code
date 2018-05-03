
package jobs;

import java.io.File;

import com.winterwell.bob.tasks.MavenDependencyTask;
import com.winterwell.utils.io.FileUtils;


public class BuildYouAgainJavaClient extends BuildWinterwellProject {

	public BuildYouAgainJavaClient() {
		super(new File(FileUtils.getWinterwellDir(), "open-code/youagain-java-client"));
		setIncSrc(true);
		setVersion("0.2.2");
	}

	@Override
	public void doTask() throws Exception {
		MavenDependencyTask mdt = new MavenDependencyTask();
		// https://mvnrepository.com/artifact/org.bitbucket.b_c/jose4j
		// For some reason Jose4J 0.6.3 doesn't seem to like our JWTs - so we're sticking with known-working 0.5.2 for now
		//mdt.addDependency("org.bitbucket.b_c", "jose4j", "0.6.3");
		mdt.addDependency("org.bitbucket.b_c", "jose4j", "0.5.2");
		mdt.setOutputDirectory(new File(projectDir, "dependencies"));
		mdt.setIncSrc(true);
		mdt.setForceUpdate(true);
		mdt.run();
		
		super.doTask();
	}

}
