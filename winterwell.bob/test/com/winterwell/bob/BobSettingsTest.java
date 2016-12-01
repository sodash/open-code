package com.winterwell.bob;

import java.io.File;

import junit.framework.TestCase;
import com.winterwell.bob.tasks.CompileTask;

public class BobSettingsTest extends TestCase {

	public void testContinue() {
		Bob bob = Bob.getSingleton();
		BobSettings settings = new BobSettings();
		settings.ignoreAllExceptions = true;
		bob.setSettings(settings);

		// TODO
	}

	public void testSettings() {
		Bob bob = Bob.getSingleton();
		BobSettings settings = new BobSettings();
		settings.logDir = new File("dummy");
		bob.setSettings(settings);

		File logFile = bob.getLogFile(new CompileTask(null, null));
		assert logFile.getPath().startsWith("dummy");
	}
}
