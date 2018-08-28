package com.winterwell.bob.tasks;

import java.io.File;

import org.junit.Test;

public class WinterwellProjectFinderTest {

	@Test
	public void testApply() {
		WinterwellProjectFinder wpf = new WinterwellProjectFinder();
		File utils = wpf.apply("winterwell.utils");
		assert utils.isDirectory();
		assert utils.getName().equals("winterwell.utils");
	}

}
