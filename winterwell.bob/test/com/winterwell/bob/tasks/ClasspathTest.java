package com.winterwell.bob.tasks;

import org.junit.Test;

public class ClasspathTest {

	@Test
	public void testGetSystemClasspath() {
		Classpath cp = Classpath.getSystemClasspath();
		System.out.println(cp);
		assert cp.toString().contains(".jar");
	}

}
