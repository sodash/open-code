package jobs;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;

import com.winterwell.bob.Bob;
import com.winterwell.bob.BuildTask;
import com.winterwell.bob.tasks.CopyRequiredClassesTask;
import com.winterwell.bob.tasks.JarTask;
import com.winterwell.utils.io.FileUtils;

/**
 * Naturally Bob is built by Bob
 * @author daniel
 *
 */
public class BuildBobRelease extends BuildWinterwellProject {

	public BuildBobRelease() {
		super(new File(FileUtils.getWinterwellDir(), "open-code/winterwell.bob"));
		incSrc = false;
		jarFile = new File(projectDir, "bob.all.jar");
		setMainClass(Bob.class);
	}

	@Override
	public void doTask() throws Exception { 
		super.doTask();
				
		// Jar Task #2 : add utils & web
		for(BuildWinterwellProject bwp : new BuildWinterwellProject[] {
				new BuildUtils(),
				new BuildWeb()
		}) {
			File ubin = bwp.getBinDir();
			JarTask jar = new JarTask(jarFile, ubin);
			jar.setAppend(true);
			jar.run();
			jar.close();
		}
	}

	@Override
	public Collection<? extends BuildTask> getDependencies() {
		return Arrays.asList(new BuildUtils(), new BuildWeb());
	}

}
