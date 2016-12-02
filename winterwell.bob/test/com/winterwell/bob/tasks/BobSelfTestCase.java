package com.winterwell.bob.tasks;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import com.winterwell.utils.io.FileUtils;

import junit.framework.TestCase;

/**
 * Superclass for testcases which act on the Bob source itself.
 * Contains convenience methods to eg set the classpath properly.
 * @author miles
 *
 */
public abstract class BobSelfTestCase extends TestCase {

	public BobSelfTestCase() {
		super();
	}

	public BobSelfTestCase(String name) {
		super(name);
	}

	protected List<File> getBobClasspath() {
		return Arrays.asList(
				getPathToJar("winterwell.utils/winterwell.utils.jar"),
				getPathToJar("middleware/junit4_4.3.1/junit.jar"),
				getPathToJar("middleware/javamail/mail.jar"));
	}

	private File getPathToJar(String jarname) {
		return new File(FileUtils.getWinterwellDir(), "code/" + jarname).getAbsoluteFile();
	}

}