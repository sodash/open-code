package com.winterwell.utils;

import java.io.File;
import java.io.IOException;

import com.winterwell.utils.FailureException;
import com.winterwell.utils.WrappedException;
import com.winterwell.utils.io.FileUtils;

/**
 * Create a temporary shell script and run it as a {@link Proc}.
 * 
 * TODO support not just Unix!
 * 
 * @author daniel
 * 
 */
public class ShellScript extends Proc {

	private static File getScriptFile() {
		try {
			return File.createTempFile("tmpShell", ".sh");
		} catch (IOException e) {
			throw new WrappedException(e);
		}
	}

	private File shellScript;

	public ShellScript(String unixShellCommand) {
		super(getScriptFile().getAbsolutePath());
		shellScript = new File(getCommand());
		FileUtils.write(shellScript, unixShellCommand);
		// Attempt to clean up
		shellScript.deleteOnExit();
		// Set permissions
		boolean ok = shellScript.setExecutable(true, true);
		if (!ok)
			throw new FailureException(
					"Could not create an executable shell script");
	}

}
