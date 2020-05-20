package com.winterwell.bob.tasks;

import java.io.File;
import java.io.IOException;

import org.junit.Test;

import com.winterwell.utils.io.FileUtils;

public class GitBobProjectTaskTest {

//	@Test slow
	public void testClone() throws IOException {
		File tempDir = File.createTempFile("bob", "test");
		FileUtils.delete(tempDir);
		GitBobProjectTask gb = new GitBobProjectTask("git@github.com:sodash/open-code", tempDir);
		gb.setSubDir("winterwell.utils");
		gb.run();
	}

	@Test
	public void testRunWithDeps() throws IOException {
		GitBobProjectTask gbt = WinterwellProjectFinder.getKnownProject("winterwell.web");
		gbt.run();
	}

	@Test
	public void testRunWithDeps_stashOn() throws IOException {
		GitBobProjectTask gbt = WinterwellProjectFinder.getKnownProject("winterwell.web");
		gbt.stashLocalChanges = true;
		gbt.run();
	}
}
