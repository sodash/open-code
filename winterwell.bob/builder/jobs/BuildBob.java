package jobs;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;

import com.winterwell.bob.Bob;
import com.winterwell.bob.BuildTask;
import com.winterwell.bob.tasks.SCPTask;
import com.winterwell.bob.tasks.WinterwellProjectFinder;
import com.winterwell.bob.wwjobs.BuildUtils;
import com.winterwell.bob.wwjobs.BuildWeb;
import com.winterwell.bob.wwjobs.BuildWinterwellProject;
import com.winterwell.utils.io.FileUtils;

/**
 * Naturally Bob is built by Bob.
 * 
 * You can get the latest version of Bob from:
 * https://www.winterwell.com/software/downloads/bob-all.jar
 * 
 * @author daniel
 *
 */
public class BuildBob extends BuildWinterwellProject {

	public BuildBob() {
		super(new WinterwellProjectFinder().apply("winterwell.bob"), "bob");
		incSrc = true;
		setMainClass(Bob.class);
		
		// uncomment if releasing Bob
//		setScpToWW(true);
//		setMakeFatJar(true);
		
		// Manually set the version
		String v = Bob.VERSION_NUMBER;
		setVersion(v);
	}

	@Override
	public void doTask() throws Exception { 
		super.doTask();
		
		// needed??
		// also make "winterwell.bob.jar" for other builds to find (e.g. BuildJerbil)
		File bobjar = getJar();
		FileUtils.copy(bobjar, new File(projectDir, "winterwell.bob.jar"));
		
		// bob-all.jar is what you want to run Bob
	}

	@Override
	public Collection<? extends BuildTask> getDependencies() {
		return Arrays.asList(new BuildUtils(), new BuildWeb());
	}

}
