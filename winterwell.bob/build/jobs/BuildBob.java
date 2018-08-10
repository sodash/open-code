package jobs;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;

import com.winterwell.bob.Bob;
import com.winterwell.bob.BuildTask;
import com.winterwell.bob.tasks.WinterwellProjectFinder;
import com.winterwell.utils.io.FileUtils;

/**
 * Naturally Bob is built by Bob
 * @author daniel
 *
 */
public class BuildBob extends BuildWinterwellProject {

	public BuildBob() {
		super(new WinterwellProjectFinder().apply("winterwell.bob"), "bob");
		incSrc = true;
		setMainClass(Bob.class);
	}

	@Override
	public void doTask() throws Exception { 
		super.doTask();
		
		// also make "winterwell.bob.jar" for other builds to find (e.g. BuildJerbil)
		File bobjar = getJar();
		FileUtils.copy(bobjar, new File(projectDir, "winterwell.bob.jar"));
		
		doFatJar();
	}

	@Override
	public Collection<? extends BuildTask> getDependencies() {
		return Arrays.asList(new BuildUtils(), new BuildWeb());
	}

}
