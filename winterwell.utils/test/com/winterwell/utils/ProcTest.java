package com.winterwell.utils;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import com.winterwell.utils.io.FileUtils;

/**
 * @tested {@link Proc}
 * @author daniel
 * 
 */
public class ProcTest {

	@Test
	public void testBashEncode() {
		{
			String raw = "http://foo.com/a.html?a=1&b=a%40com";
			String s = raw; //Proc.bashEncodeStrong(raw);
			Proc p = new Proc(Arrays.asList("echo", s));
			p.start();
			p.waitFor(1000);
			String out = p.getOutput().trim();
			assert out.equals(raw) : out;
		}
		{
			String raw = "Here's my test";
			String s = raw; //Proc.bashEncodeStrong("Here's my test");
			Proc p = new Proc(Arrays.asList("echo", s));
			p.start();
			p.waitFor(1000);
			String out = p.getOutput().trim();
			assert out.equals("Here's my test") : out;
		}
	}
	
	/**
	 * This intermittently causes an error! Have added a tiny delay to waitFor
	 * which seems to fix things
	 */
	@Test
	public void testBug() {
		Proc p = new Proc("ls "
				+ new File(FileUtils.getWinterwellDir(), "code"));
		p.run();
		p.waitFor();
		String output = p.getOutput();
		// assert output.contains("Revision") : output;
		p.close();
	}

	@Test
	public void testParse() throws InterruptedException {
		// Unix
		Proc p = new Proc("");
		List<String> parsed = p.parse("test with   \"quoted stuff\"		ok");
		assert Arrays.equals(new String[] { "test", "with", "quoted stuff",
				"ok" }, parsed.toArray(StrUtils.ARRAY)) : parsed;
		p.close();
	}

	@Test
	public void testProcess() throws InterruptedException {
		// Unix
		Proc p = new Proc("ls -la");
		p.run();
		p.waitFor();
		String out = p.getOutput();
		String err = p.getError();
		String ps = p.toString();
		p.close();
		String ps2 = p.toString();
		assert err.length() == 0;
		assert out.length() != 0;
		assert out.contains("..") : out;
	}

	@Test
	public void testProcessEcho() throws InterruptedException {
		// check sys-out to see if that works
		// Unix
		Proc p = new Proc("ls -la");
		p.setEcho(true);
		p.run();
		p.waitFor();
		p.close();
	}

	/**
	 * This appears to show that there isn't a problem with process... see
	 * {@link Proc#closeStreams}
	 */
	@Test
	public void testProcessVsOpenFilesIssue() throws IOException {
		for (int i = 0; i < 10000; i++) {
			System.out.println("Launching " + i);
			ProcessBuilder a = new ProcessBuilder("ls");
			java.lang.Process p = a.start();
			p.destroy();
		}
	}

	@Test
	public void testSetDirectory() throws InterruptedException {
		// Unix
		Proc p = new Proc("pwd");
		p.setDirectory(new File("src"));
		p.run();
		p.waitFor();
		String out = p.getOutput().trim();
		String err = p.getError();
		p.close();
		assert out.endsWith("/src");
//		System.out.println(out);
	}

	@Test
	public void testWaitFor() throws InterruptedException {
		{ // good
			Proc p = new Proc("ls");
			p.run();
			p.waitFor(2000);
			String out = p.getOutput();
			assert out.length() != 0 : p.getError();
			p.close();
		}
		{ // bad (will not exit)
			Proc p = new Proc("grep waitForInput");
			p.run();
			try {
				p.waitFor(1000);
				// NB: should not finish
				assert false : p.getOutput();
			} catch (Exception e) {
				// yeh :)
				System.out.println(p.getOutput());
			} finally {
				p.close();
			}
		}
	}
}
