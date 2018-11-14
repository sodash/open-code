package com.winterwell.bob.tasks;

import static org.junit.Assert.*;

import org.junit.Test;

public class ClasspathTest {

	@Test
	public void testGetSystemClasspath() {
		Classpath cp = Classpath.getSystemClasspath();
		System.out.println(cp);
		assert cp.toString().contains(".jar");
	}

}
