package jobs;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;

import com.winterwell.bob.Bob;
import com.winterwell.bob.BuildTask;
import com.winterwell.bob.tasks.CompileTask;
import com.winterwell.bob.tasks.JarTask;
import com.winterwell.utils.io.FileUtils;

/**
 * Naturally Bob is built by Bob
 * @author daniel
 *
 */
public class BuildBob extends BuildWinterwellProject {

	public BuildBob() {
		super(new File(FileUtils.getWinterwellDir(), "open-code/winterwell.bob"));
		incSrc = true;
		jarFile = new File(projectDir, "bob.jar");
	}

	@Override
	public void doTask() throws Exception { 
		super.doTask();
		
//		// Jar Task #2 : add utils
//		// Merge in utils TODO select which bits!
//		jar = new JarTask(bobjar, new File(FileUtils.getWinterwellDir(), "code/winterwell.utils/bin"));
//		jar.setAppend(true);
//		jar.run();
		
//		// copy into code/lib
//		File lib = new File(FileUtils.getWinterwellDir(), "code/lib");
//		lib.mkdir();
//		FileUtils.copy(bobjar, lib);
	}

	@Override
	public Collection<? extends BuildTask> getDependencies() {
		return Arrays.asList(new BuildUtils());
	}

}
