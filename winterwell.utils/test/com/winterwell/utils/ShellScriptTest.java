package com.winterwell.utils;

import java.io.File;

import com.winterwell.utils.ShellScript;

import junit.framework.TestCase;
import com.winterwell.utils.io.FileUtils;

public class ShellScriptTest extends TestCase {

	public void testUnixShellProcess() {
		File f = new File("testOutput.txt");
		ShellScript p = new ShellScript("ls > " + f.getAbsolutePath());
		p.run();
		p.waitFor();
		String out = FileUtils.read(f);
		assert out.length() > 0 : out;
		System.out.println(out);
		FileUtils.delete(f);
	}

}
