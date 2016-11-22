package com.winterwell.utils;

import java.io.File;

import org.junit.Test;

import com.winterwell.utils.Printer;

import com.winterwell.utils.io.FileUtils;

public class WrappedExceptionTest {

	@Test
	public void testGetCause() {
		try {
			FileUtils.copy(new File("foo/bar"), new File("bar/foo"));
		} catch (Throwable ex) {
			WrappedException wex = new WrappedException(ex);
			wex.printStackTrace();
			assert wex.getCause() == ex : wex.getCause();
		}
	}

	@Test
	public void testToString() {
		try {
			FileUtils.copy(new File("foo/bar"), new File("bar/foo"));
		} catch (Throwable ex) {
			WrappedException wex = new WrappedException(ex);
			String s = wex.toString();
			System.out.println(s);
			String s2 = Printer.toString(wex, true);
			System.out.println(s2);
			assert s2.contains("foo");
			assert s2.contains("FileUtils");
		}
	}

}
