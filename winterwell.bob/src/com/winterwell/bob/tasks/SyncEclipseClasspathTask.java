package com.winterwell.bob.tasks;

import java.io.File;
import java.util.HashSet;

import com.winterwell.bob.BuildTask;
import com.winterwell.utils.containers.Tree;
import com.winterwell.utils.io.FileUtils;
import com.winterwell.utils.web.WebUtils2;
import com.winterwell.utils.web.XMLNode;

/**
 * status: SKETCH
 * Add/remove to .classpath so that the dependencies/x.jar match what's in the dependencies folder.
 * 
 * Use this after {@link MavenDependencyTask} as a convenient way to maintain the classpath.
 * 
 * @testedby {@link SyncEclipseClasspathTaskTest}
 * @author daniel
 *
 */
public class SyncEclipseClasspathTask extends BuildTask {

	File projectDir;
	File depsDir;
	File classpathFile;
	
	public SyncEclipseClasspathTask(File projectDir) {
		this.projectDir = projectDir;
		depsDir = new File(projectDir, "dependencies");
		classpathFile = new File(projectDir, ".classpath");
	}
	
	@Override
	protected void doTask() throws Exception {
		String[] ajars = depsDir.list();
		HashSet jars = new HashSet();
		for (String j : ajars) {
			if (j.endsWith(".jar")) jars.add(j);
		}
		System.out.println(jars);
		
		String xml = FileUtils.read(classpathFile);
		Tree<XMLNode> tree = WebUtils2.parseXmlToTree(xml);
		for (XMLNode node : tree) {
			System.out.println(node);
		}
		
		// output
	}

}
