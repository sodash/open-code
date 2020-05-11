package com.winterwell.bob;

import java.io.File;

import com.winterwell.utils.Key;
import com.winterwell.utils.Printer;
import com.winterwell.utils.containers.Containers;
import com.winterwell.utils.io.Option;

/**
 * The settings for {@link Bob}. These can be set via command line arguments
 * passed to {@link Bob#main(String[])}, or by using
 * {@link Bob#setSettings(BobSettings)}.
 * 
 * @author Daniel
 */
public class BobSettings {

	public static final Key<Boolean> VERBOSE = new Key<Boolean>("verbose");

	public final static String VERSION_NUMBER = "0.9.26";
	
	@Option(tokens="-cp,-classpath", description="Classpath used for dynamically compiling build scripts. Uses the file1:file2 format of Java")
	// NB: This is not the classpath used for CompileTasks which are part of a build script run.
	public String classpath;
	

	@Option(description="Just print this help message and exit")
	public boolean help;
	
	@Option(tokens = "-ignore", description = "Ignore all exceptions")
	public boolean ignoreAllExceptions;

	@Option(tokens = "-logdir", description = "Directory to write log files to")
	public File logDir = new File("boblog");

	@Option(tokens = "-nolog", description = "Switch off logging")
	public boolean loggingOff;
	
	@Option(tokens = "-q,-quiet")
	public boolean quiet;

	@Option(tokens = "-noskip,-clean", 
			description="Switch off task-history dependency skipping (if on, avoids re-running recent sub-tasks). Kind of equivalent to deleting boblog.")
	public boolean skippingOff;

	@Option(description="TODO sniff the script .java file for Bob settings in javadoc, e.g. @Bob -classpath lib/foo.jar")
	public boolean sniff = true;

	@Option(tokens="-update", description="Download a fresh copy of Bob itself")
	public boolean update;

	@Option(tokens = "-v,-verbose")
	public boolean verbose;

	// @Option(tokens = "-p,-properties", description =
	// "Java properties file to load")
	// public File properties = new File("bob.properties");

	@Override
	public String toString() {
		return "BobSettings"+Printer.toString(Containers.objectAsMap(this));
	}

}
