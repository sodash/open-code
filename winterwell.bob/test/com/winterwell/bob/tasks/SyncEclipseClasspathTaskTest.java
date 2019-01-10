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
		File projectDir = FileUtils.getWorkingDirectory(); // bob
		File cp = new File("test/test.classpath.after");
		FileUtils.copy(new File("test/test.classpath.before"), cp);
		SyncEclipseClasspathTask sect = new SyncEclipseClasspathTask(projectDir);
		sect.classpathFile = cp;
		
		sect.run();
		
		String after = FileUtils.read(cp);
		Tree<XMLNode> parsed = WebUtils2.parseXmlToTree(after);
		assert after.contains(""); // TODO check preserved jars
		assert after.contains(""); // TODO check added jars
		assert ! after.contains("wibblefruit"); // check removed jars
	}

}
