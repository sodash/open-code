package com.winterwell.bob.tasks;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;

import com.winterwell.utils.web.WebUtils2;

public class JUnitTaskTest {

	@Test
	public void testDoTask() throws Exception {
//		boolean interactive = GuiUtils.isInteractive();
//		GuiUtils.setInteractive(false);
		File srcDir = new File("test");
		Collection<File> classPath = Arrays.asList(new File("bin.test"));
		File outputFile = new File("bin.test/JunitTaskTest.html");
		JUnitTask jut = new JUnitTask(srcDir, classPath, outputFile);
//		jut.setExceptionOnTestFailure(true);
		jut.doTask();
		
//		GuiUtils.setInteractive(interactive);
		WebUtils2.display(outputFile);
	}

}
