package com.winterwell.bob;

import java.io.File;
import java.util.Map;

import org.eclipse.jetty.util.ajax.JSON;
import org.junit.Test;

import com.winterwell.utils.io.FileUtils;

public class BobTest {

	@Test
	public void testGetClassString() throws Exception {
		File f = new File(FileUtils.getWinterwellDir(), "jerbil/builder/jerbil/BuildJerbil.java");
		assert f.isFile();
		Class clazz = Bob.getClass(f.getAbsolutePath());
		System.out.println(clazz);
	}
	
	@Test
	public void testLoadHistory() {
		File file = BobLog.getHistoryFile();
		String json = FileUtils.read(file);
		Map jobj = (Map) JSON.parse(json);
	}

}
