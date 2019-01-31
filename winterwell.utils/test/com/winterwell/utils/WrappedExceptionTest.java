package com.winterwell.utils;

import java.io.File;

import org.junit.Test;

import com.winterwell.utils.io.FileUtils;

public class WrappedExceptionTest {

	@Test
	public void testGetCause() {
		try {
			FileUtils.copy(new File("foo/bar"), new File("bar/foo"));
		} catch (Throwable ex) {
			WrappedException wex = new WrappedException(ex);
			assert wex.getCause() == ex : wex.getCause();
		}
	}

	@Test
	public void testToString() {
		try {
			// This throws a WrappedException since the file doesn't exist
			FileUtils.copy(new File("foo/bar"), new File("bar/foo"));
		} catch (Throwable ex) {
			String s2 = Printer.toString(ex, true);
			assert s2.contains("foo");
			assert s2.contains("FileUtils");
		}
	}

}
