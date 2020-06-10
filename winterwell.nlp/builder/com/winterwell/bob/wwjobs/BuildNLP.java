package com.winterwell.bob.wwjobs;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import com.winterwell.bob.BuildTask;
import com.winterwell.bob.tasks.CopyRequiredClassesTask;
import com.winterwell.bob.tasks.JarTask;
import com.winterwell.bob.tasks.MavenDependencyTask;
import com.winterwell.utils.IFilter;
import com.winterwell.utils.Printer;
import com.winterwell.utils.io.FileUtils;

public class BuildNLP extends BuildWinterwellProject {

	public BuildNLP() {
		super("winterwell.nlp");
		incSrc = false;
	}

	@Override
	public List<BuildTask> getDependencies() {
		List<BuildTask> deps = super.getDependencies();

		MavenDependencyTask mdt = new MavenDependencyTask();
//		mdt.addDependency("snowball-stemmer", "snowball-stemmer", "0.1.0"); throws an error?!
//		mdt.addDependency("net.didion", "jwnl", "1.4.1"); // error?!
		mdt.addDependency("org.apache.commons", "commons-compress", "1.20");
		mdt.setIncSrc(true); // we like source code
		mdt.setProjectDir(projectDir);
		deps.add(mdt);
		
		return deps;
	}
	
	@Override
	public void doTask() throws Exception {
		super.doTask();
		
		// Also build an ISO639 only one
		File f1 = new File(projectDir, "src/com/winterwell/nlp/languages/ISO639.java");
		List<File> target = Arrays.asList(f1);
		
		File jardir = new File("temp-iso");
		jardir.mkdirs();
		FileUtils.deleteDir(jardir);
		jardir.mkdirs();
		
		CopyRequiredClassesTask req = new CopyRequiredClassesTask(target, jardir);
		req.setFilter(new IFilter<File>() {			
			@Override
			public boolean accept(File x) {
				return x.getName().contains("ISO639");
			}
		});
		req.setIncludeSource(true);
		req.run();

		File lib = new File(FileUtils.getWinterwellDir(), "code/lib");
		File jar = new File(lib, "iso639.jar");

		FileUtils.delete(jar);
		JarTask jarTask = new JarTask(jar, jardir);
		jarTask.run();

		Printer.out(req.getDependencyTree().toString2(0, 30));
	}
	
}
