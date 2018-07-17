package com.winterwell.bob;

import static org.junit.Assert.*;

import java.io.File;

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

}
