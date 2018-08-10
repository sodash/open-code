package com.winterwell.bob.tasks;

import org.junit.Test;

public class MavenDependencyTaskTest {

	@Test
	public void testMavenDependencyTask() {
		MavenDependencyTask mdt = new MavenDependencyTask();
		mdt.addDependency("junit", "junit", "4.12");
		mdt.run();
	}

}
