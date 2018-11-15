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
//		setScpToWW(true);
		makeFatJar = true;
		
		// Manually set the version
		String v = Bob.VERSION_NUMBER;
		setVersion(v);
	}

	@Override
	public void doTask() throws Exception { 
		super.doTask();
		
		// also make "winterwell.bob.jar" for other builds to find (e.g. BuildJerbil)
		File bobjar = getJar();
		FileUtils.copy(bobjar, new File(projectDir, "winterwell.bob.jar"));
		
		// bob-all.jar -- This is what you want to run Bob
		if (makeFatJar) {
			File fatJar = doFatJar();

			if (scpToWW) {
				String remoteJar = "/home/winterwell/public-software/"+fatJar.getName();
				SCPTask scp = new SCPTask(fatJar, "winterwell@winterwell.com",				
						remoteJar);
				// this is online at: https://www.winterwell.com/software/downloads
				scp.setMkdirTask(false);
				scp.run();
		//			scp.runInThread(); no, wait for it to finish
				report.put("scp to remote", "winterwell.com:"+remoteJar);
			}
		}
	}

	@Override
	public Collection<? extends BuildTask> getDependencies() {
		return Arrays.asList(new BuildUtils(), new BuildWeb());
	}

}
