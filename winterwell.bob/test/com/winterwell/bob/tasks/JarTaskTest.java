package com.winterwell.bob.tasks;

import java.io.File;

import junit.framework.TestCase;

public class JarTaskTest extends TestCase {

	/**
	 * These jar files are giving "invalid archive" messages when examined through the Windows file explorer
	 * TODO check in Linux
	 */
	public void testWindowsWierdness() {
		File dir = new File("bin");
		File jarFile = new File("testing/test.jar");
		JarTask jt = new JarTask(jarFile, dir);
		jt.run();
	}
}
