package com.winterwell.bob.tasks;

import static org.junit.Assert.*;

import java.io.File;

import org.junit.Test;

import com.winterwell.utils.containers.Tree;
import com.winterwell.utils.io.FileUtils;
import com.winterwell.utils.web.WebUtils2;
import com.winterwell.utils.web.XMLNode;

public class SyncEclipseClasspathTaskTest {

	@Test
	public void testSyncEclipseClasspathTask() {
		File projectDir = new File("../winterwell.utils");
		assert projectDir.isDirectory() : projectDir.getAbsolutePath();
		// NB: utils cp
		File cp = new File("test/test.classpath.after");
		FileUtils.copy(new File("test/test.classpath.before"), cp);
		SyncEclipseClasspathTask sect = new SyncEclipseClasspathTask(projectDir);
		sect.classpathFile = cp;
		
		sect.run();
		
		String after = FileUtils.read(cp);
		System.out.println(after);
		assert after.contains("xstream.jar"); // check preserved jars
		assert after.contains("dependencies/xpp3_min.jar"); // check added jars
		assert ! after.contains("wibblefruit"); // check removed jars
	}

}
