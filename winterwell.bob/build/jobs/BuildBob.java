package jobs;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import com.winterwell.bob.Bob;
import com.winterwell.bob.BuildTask;
import com.winterwell.bob.tasks.BigJarTask;
import com.winterwell.bob.tasks.EclipseClasspath;
import com.winterwell.bob.tasks.JarTask;
import com.winterwell.utils.io.FileUtils;

/**
 * Naturally Bob is built by Bob
 * @author daniel
 *
 */
public class BuildBob extends BuildWinterwellProject {

	public BuildBob() {
		super(new File(FileUtils.getWinterwellDir(), "open-code/winterwell.bob"), "bob");
		incSrc = true;
		setMainClass(Bob.class);
	}

	@Override
	public void doTask() throws Exception { 
		super.doTask();
		
		File ujar = new BuildUtils().getJar();
		FileUtils.copy(ujar, projectDir);
		
		// Jar Task #2 : add utils etc
		List<File> jars = FileUtils.find(new File(projectDir, "dependencies"), ".*\\.jar");
		for(BuildTask bt : getDependencies()) {
			if (bt instanceof BuildWinterwellProject) {
				jars.add(((BuildWinterwellProject) bt).getJar());	
			}
		}
				
		jars.add(0, getJar());
		
//		EclipseClasspath ec = new EclipseClasspath(projectDir);
//		Set<File> libs = ec.getCollectedLibs();		
		
//		jars.addAll(libs);
		File fatjar = new File("bob-all.jar");
		BigJarTask jt = new BigJarTask(fatjar, jars);
		jt.setManifestProperty(jt.MANIFEST_MAIN_CLASS, mainClass);
		jt.run();
	}

	@Override
	public Collection<? extends BuildTask> getDependencies() {
		return Arrays.asList(new BuildUtils(), new BuildWeb());
	}

}
