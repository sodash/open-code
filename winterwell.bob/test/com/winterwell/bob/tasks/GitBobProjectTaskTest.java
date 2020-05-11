package com.winterwell.bob.tasks;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;

import org.junit.Test;

import com.winterwell.utils.Utils;
import com.winterwell.utils.io.FileUtils;

public class GitBobProjectTaskTest {

	@Test
	public void testClone() throws IOException {
		File tempDir = File.createTempFile("bob", "test");
		FileUtils.delete(tempDir);
		GitBobProjectTask gb = new GitBobProjectTask("git@github.com:sodash/open-code", tempDir);
		gb.setSubDir(new File("winterwell.utils"));
		gb.run();
	}

}
