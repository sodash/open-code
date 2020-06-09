package com.winterwell.bob;

import java.io.File;

import org.junit.Test;

import com.winterwell.utils.io.FileUtils;

public class BobTest {

	@Test
	public void testLoadHistory() {
		File file = BobLog.getHistoryFile();
		String csv = FileUtils.read(file);
	}

}
