package com.winterwell.bob;

import java.io.File;

import com.winterwell.bob.tasks.CompileTask;

import junit.framework.TestCase;

public class BobSettingsTest extends TestCase {

	public void testContinue() {
		Bob bob = Bob.getSingleton();
		BobConfig settings = new BobConfig();
		settings.ignoreAllExceptions = true;
		bob.setConfig(settings);

		// TODO
	}

	public void testSettings() {
		Bob bob = Bob.getSingleton();
		BobConfig settings = new BobConfig();
		settings.logDir = new File("dummy");
		bob.setConfig(settings);

		File logFile = bob.getLogFile(new CompileTask(null, null));
		assert logFile.getPath().startsWith("dummy");
	}
}
