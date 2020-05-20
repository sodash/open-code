package com.winterwell.bob.tasks;

import java.io.File;

import org.junit.Test;

import com.winterwell.bob.TestTask;
import com.winterwell.utils.io.FileUtils;

public class ForkJVMTaskTest {

	@Test
	public void testDoTask() throws Exception {
		ForkJVMTask fork = new ForkJVMTask(TestTask.class);
		fork.setClasspath(new Classpath("bob-all.jar:bin.test"));
		fork.doTask();
	}
	
	@Test
	public void testDoTaskFindScript() throws Exception {
		ForkJVMTask fork = new ForkJVMTask();
		fork.setDir(new File(FileUtils.getWinterwellDir(), "open-code/winterwell.utils"));
//		fork.setClasspath(new Classpath("bob-all.jar"));
		fork.doTask();
	}

}
