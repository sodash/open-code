package com.winterwell.utils;

import java.io.File;
import java.io.IOException;

import org.junit.Test;

import com.winterwell.utils.io.FileUtils;

public class WrappedExceptionTest {

	@Test
	public void testGetCause1() {
		try {
			FileUtils.copy(new File("foo/bar"), new File("bar/foo"));
		} catch (Throwable ex) {
			WrappedException wex = new WrappedException(ex);
			wex.printStackTrace();
			assert wex.getCause() == ex.getCause() : wex.getCause();
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
			assert s2.contains("FileUtils") : s2;
		}
	}

	@Test
	public void testGetMessage() {
		try {
			FileUtils.copy(new File("foo/bar"), new File("bar/foo"));
		} catch (Throwable ex) {
			{
				WrappedException wex = new WrappedException(ex);
				String s = wex.getMessage();
				assert s.contains("foo");
			}
			{
				WrappedException wex = new WrappedException("Hello", ex);
				String s = wex.getMessage();
				assert s.contains("Hello");
				assert s.contains("foo");
			}
		}
	}


	@Test
	public void testGetCauseDoubleWrapped() {
		try {
			FileUtils.copy(new File("foo/bar"), new File("bar/foo"));
		} catch (Throwable ex) {
			WrappedException wex = new WrappedException(ex);
			assert wex.getCause() instanceof IOException : wex.getCause().getClass();
			
			WrappedException wex2 = new WrappedException("hello", ex);
			assert wex2.getCause() instanceof IOException : wex2.getCause().getClass();
		}
	}

	@Test
	public void testToString2Msg() {
		try {
			FileUtils.copy(new File("foo/bar"), new File("bar/foo"));
		} catch (Throwable ex) {
			String m0 = ex.getMessage();
			WrappedException wex = new WrappedException("This is the top Message", ex);
			String s = wex.toString();
			System.out.println(s);
			String s2 = Printer.toString(wex, true);
			System.out.println(s2);
			assert s2.contains("foo");
			assert s2.contains("FileUtils");
			assert s2.contains("top Mess");
		}
	}

}
