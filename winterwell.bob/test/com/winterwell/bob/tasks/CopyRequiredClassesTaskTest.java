package com.winterwell.bob.tasks;

import java.io.File;

import org.junit.Test;

import com.winterwell.utils.Printer;

import com.winterwell.utils.containers.Tree;
import com.winterwell.utils.io.FileUtils;

/**
 * @tested {@link CopyRequiredClassesTask}
 * @author daniel
 *
 */
public class CopyRequiredClassesTaskTest {


	@Test
	public void testComments() {
		{
			String clean = CopyRequiredClassesTask.JAVA_COMMENT.matcher(
					"\t/** Hello */ x*y").replaceAll("");
			assert clean.equals("\t x*y") : clean;
		}
		{
			String clean = CopyRequiredClassesTask.JAVA_COMMENT.matcher(
					"\t/** Hello\n\t* What's up?\n\t*/\nx*y").replaceAll("");
			assert clean.equals("\t\nx*y") : clean;
		}
		{
			String clean = CopyRequiredClassesTask.JAVA_COMMENT2.matcher(
					"a// b\n//\nc").replaceAll("");
			assert clean.equals("a\n\nc") : clean;
		}	
	}
	
	@Test
	public void testExampleFile() {
		
		File tempOut = FileUtils.createTempDir();
		CopyRequiredClassesTask copyReqTask = new CopyRequiredClassesTask(FileUtils.getWorkingDirectory(), 
				tempOut);
		
		File fu = new File(FileUtils.getWinterwellDir(), "code/winterwell.utils/src/winterwell/utils/io/FileUtils.java");
		assert fu.exists();
		Tree deps = new Tree();
		copyReqTask.doTask2_copyInClasses(fu, deps);
		Printer.out(deps.toString2(0, 30));
		// check output... looks OK at present
		FileUtils.deleteDir(tempOut);
	}
	
	@Test
	public void testDoTask() {
		// let's see what Bob needs
		System.out.println("WORKINGDIR 0>" + FileUtils.getWorkingDirectory());
		File srcDir = new File("src");
		File classDir = new File("temp-bin");
		if (classDir.exists()) FileUtils.deleteDir(classDir);
		classDir.mkdirs();
		
		CopyRequiredClassesTask copyReqTask = new CopyRequiredClassesTask(srcDir, classDir);
		copyReqTask.run();
		
		assert classDir.list().length > 0;
		File utils = FileUtils.find(classDir, ".*Utils\\.class").get(0);
		assert utils != null;
	}

}
