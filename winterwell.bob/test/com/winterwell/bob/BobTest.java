package com.winterwell.bob;

import java.io.File;
import java.util.Map;

import org.eclipse.jetty.util.ajax.JSON;
import org.junit.Test;

import com.winterwell.bob.tasks.WinterwellProjectFinder;
import com.winterwell.utils.containers.Pair;
import com.winterwell.utils.io.FileUtils;

public class BobTest {

	@Test
	public void testLoadHistory() {
		File file = BobLog.getHistoryFile();
		String csv = FileUtils.read(file);
	}

}
