package com.winterwell.bob;

import java.io.File;
import java.io.IOException;

import com.winterwell.utils.Key;
import com.winterwell.utils.Printer;
import com.winterwell.utils.Utils;
import com.winterwell.utils.containers.Containers;
import com.winterwell.utils.io.FileUtils;
import com.winterwell.utils.io.Option;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.time.Time;

/**
 * The settings for {@link Bob}. These can be set via command line arguments
 * passed to {@link Bob#main(String[])}, or by using
 * {@link Bob#setConfig(BobSettings)}.
 * 
 * @author Daniel
 */
public class BobConfig {

	public static final Key<Boolean> VERBOSE = new Key<Boolean>("verbose");

	public final static String VERSION_NUMBER = "1.2.8"; // July 06 2021
	
	@Option(tokens="-cp,-classpath", description="Classpath used for dynamically compiling build scripts. Uses the file1:file2 format of Java")
	// NB: This is not the classpath used for CompileTasks which are part of a build script run.
	public String classpath;
	
	@Option(description = "A name for a build run, to help tell them apart in logs")
	public String label = Utils.getRandomString(4);

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

	@Option(tokens = "-noskip,-clean,-skippingOff", 
			description="Switch off task-history dependency skipping (if on, avoids re-running recent sub-tasks). Kind of equivalent to deleting boblog.")
	public boolean clean;
	
	@Option(description="Clean anything before the given time (this is also used by clean for child Bobs).")
	public Time cleanBefore;

	@Option(description="Usually unset. Limit how deep the recursion can go. E.g. `-clean -maxDepth 1` will do immeadiate dependencies but no deeper (useful for running a local maven download only).")
	public int maxDepth;
	
	@Option(description="Usually unset. Remove tasks from bobhistory.csv which match this regex pattern, to avoid skipping that task, then exit (note: bobhistory.csv is shared between projects on the computer).")
	public String forget;	
	
	@Option(description="TODO sniff the script .java file for Bob settings in javadoc, e.g. @Bob -classpath lib/foo.jar")
	public boolean sniff = true;

	@Option(tokens="-update", description="Download a fresh copy of Bob itself, then exit.")
	public boolean update;

	@Option(tokens = "-v,-verbose")
	public boolean verbose;
	
	@Option(description="Set by Bob when making recursive child Bob. 0 for top-level")
	public int depth;

	// NB: non an option 'cos its (currently) a system-wide setting 
	File bobwarehouse;
	
	@Option(description="Save call dependency graph in dot format. Normally in bobwarehouse.")
	public File dotFile;

	public BobConfig() {
		try {
			bobwarehouse = new File(FileUtils.getWinterwellDir(),"bobwarehouse");
		} catch(Exception ex) {
			try {
				File tf = File.createTempFile("blah", ".temp");
				File tempDir = tf.getParentFile();
				bobwarehouse = new File(tempDir, "bobwarehouse");
				Log.w("BobConfig", "Using temp dir "+bobwarehouse+". It would be better to set WINTERWELL_HOME");
			} catch (IOException e) {
				Log.e("BobConfig", "No winterwell or temp dir - Cannot make a reliable bobwarehouse. Using .bob as a fallback!");
				bobwarehouse = new File(".bob", "bobwarehouse");
			}
		}
		dotFile = new File(bobwarehouse, "calls.dot");		
	}
	
	

	/**
	 * 
	 * @return winterwell/bobwarehouse or temp/bobwarehouse
	 */
	public File getGitBobDir() {
		return bobwarehouse;
	}
	
	// @Option(tokens = "-p,-properties", description =
	// "Java properties file to load")
	// public File properties = new File("bob.properties");

	@Override
	public String toString() {
		return "BobSettings"+Printer.toString(Containers.objectAsMap(this));
	}

}
