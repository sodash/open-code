package com.winterwell.bob.tasks;

import java.io.File;

/**
 * Run Ant using an external process call. You must have Ant installed and
 * configured.
 * 
 * @author daniel
 */
public class AntTask extends ProcessTask {

	private final File script;
	private final String[] targets;

	/**
	 * Run Ant using an external process call. You must have Ant installed and
	 * configured.
	 */
	public AntTask(File script) {
		super("ant");
		this.script = script;
		targets = new String[0];
		if (!(script.isFile() && script.getName().endsWith(".xml"))
				&& !script.isDirectory()) {
			throw new IllegalArgumentException(script
					+ " is not a .xml file or a directory!");
		}
	}

	public AntTask(File script, String... targets) {
		super("ant");
		this.script = script;
		this.targets = targets;
	}

	/**
	 * <pre>
	 * -diagnostics           print information that might be helpful to
	 * 	                         diagnose or report problems.
	 * 	  -quiet, -q             be extra quiet
	 * 	  -verbose, -v           be extra verbose
	 * 	  -debug, -d             print debugging information
	 * 	  -emacs, -e             produce logging information without adornments
	 * 	  -lib &lt;path&gt;            specifies a path to search for jars and classes
	 * 	  -logfile &lt;file&gt;        use given file for log
	 * 	    -l     &lt;file&gt;                ''
	 * 	  -logger &lt;classname&gt;    the class which is to perform logging
	 * 	  -listener &lt;classname&gt;  add an instance of class as a project listener
	 * 	  -noinput               do not allow interactive input
	 * 	  -D&lt;property&gt;=&lt;value&gt;   use value for given property
	 * 	  -keep-going, -k        execute all targets that do not depend
	 * 	                         on failed target(s)
	 * 	  -propertyfile &lt;name&gt;   load all properties from file with -D
	 * 	                         properties taking precedence
	 * 	  -inputhandler &lt;class&gt;  the class which will handle input requests
	 * 	  -find &lt;file&gt;           (s)earch for buildfile towards the root of
	 * 	    -s  &lt;file&gt;           the filesystem and use it
	 * 	  -nice  number          A niceness value for the main thread:
	 * 	                         1 (lowest) to 10 (highest); 5 is the default
	 * 	  -nouserlib             Run ant without using the jar files from
	 * 	                         ${user.home}/.ant/lib
	 * 	  -noclasspath           Run ant without using CLASSPATH
	 * 	  -autoproxy             Java1.5+: use the OS proxy settings
	 * 	  -main &lt;class&gt;          override Ant's normal entry point
	 * </pre>
	 */
	@Override
	public void addArg(String option) {
		args.add(option);
	}

	@Override
	public void doTask() throws Exception {
		if (script.isFile()) {
			addArg("-buildfile " + script.getAbsolutePath());
		}
		for (String t : targets) {
			addArg(t);
		}
		if (script.isDirectory()) {
			setDirectory(script);
		}
		super.doTask();
	}

}
