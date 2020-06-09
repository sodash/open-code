package com.winterwell.bob;

import java.io.File;

import org.junit.Test;

import com.winterwell.bob.tasks.WinterwellProjectFinder;
import com.winterwell.utils.containers.Pair;
import com.winterwell.utils.io.FileUtils;

public class BobScriptFactoryTest {

	@Test
	public void testCompileClass() throws Exception {
		File baseDir = new WinterwellProjectFinder().apply("winterwell.datalog");
		Pair<File> compd = new BobScriptFactory(baseDir).getClass3_compileClass(baseDir, "/home/daniel/winterwell/open-code/winterwell.datalog/builder/com/winterwell/datalog/BuildDataLog.java");
		System.out.println(compd);
	}
	
	@Test
	public void testGetClassString() throws Exception {
		File f = new File(FileUtils.getWinterwellDir(), "jerbil/builder/jerbil/BuildJerbil.java");
		assert f.isFile();
		File pdir = new WinterwellProjectFinder().apply("jerbil");
		Class clazz = new BobScriptFactory(pdir).getClass(f.getAbsolutePath());
		System.out.println(clazz);
	}
	
}
