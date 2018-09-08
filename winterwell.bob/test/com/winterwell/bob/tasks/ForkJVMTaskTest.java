package com.winterwell.bob.tasks;

import static org.junit.Assert.*;

import org.junit.Test;

import com.winterwell.bob.BuildTask;
import com.winterwell.bob.TestTask;

public class ForkJVMTaskTest {

	@Test
	public void testDoTask() throws Exception {
		BuildTask target = new TestTask();
		ForkJVMTask fork = new ForkJVMTask(target);
		fork.setClasspath("bob-all.jar:bin.test");
		fork.doTask();
	}

}
