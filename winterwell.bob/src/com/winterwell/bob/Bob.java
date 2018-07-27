package com.winterwell.bob;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

import com.winterwell.bob.tasks.CompileTask;
import com.winterwell.utils.Dep;
import com.winterwell.utils.FailureException;
import com.winterwell.utils.Printer;
import com.winterwell.utils.ReflectionUtils;
import com.winterwell.utils.StrUtils;
import com.winterwell.utils.Utils;
import com.winterwell.utils.containers.Containers;
import com.winterwell.utils.containers.Pair;
import com.winterwell.utils.containers.Pair2;
import com.winterwell.utils.io.ArgsParser;
import com.winterwell.utils.io.ConfigBuilder;
import com.winterwell.utils.io.ConfigFactory;
import com.winterwell.utils.io.ConfigFactoryTest;
import com.winterwell.utils.io.FileUtils;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.log.LogFile;
import com.winterwell.utils.threads.TaskRunner;
import com.winterwell.utils.time.Time;
import com.winterwell.utils.time.TimeUtils;

/**
 * Bob the Builder, a Java based build utility.
 * 
 * <h2>Usage</h2> 
 * Example:
 * <code>
 * java -classpath bob-all.jar MyBuildScript 
 * </code>
 * <p>
 * The Bob class is for command line usage. For programmatic
 * usage, see {@link BuildTask}.
 * </p>
 * 
 * <h2>Options</h2> TODO copy from usage message
 * 
 * <h2>Logging output</h2> System.out and System.err are captured and directed
 * into multiple log files - as well as to their normal output. By default the
 * log files are stored in a directory called boblog, and are named after the
 * class generating them. boblog files contain all the output from that task and
 * any tasks it called.
 * 
 * <p>
 * Logging can be switched off using the -nolog option. The boblog director can
 * be set using the -logdir option TODO a class loader that spits out helpful
 * error messages when certain classes are not found.
 * 
 * <h2>Open questions</h2> Bob is in an early stage of development. Things that
 * are yet to be decided:<br>
 * - How to handle logging and console printing.<br>
 * - Shell scripts for launching Bob.<br>
 * - What tasks to bundle in the jar.<br>
 * 
 * @author daniel
 * 
 */
public class Bob {

	private static Bob dflt;

	private static Map<Runnable, Time> time4task = new HashMap<>();

	private static volatile Time runStart;

	public final static String VERSION_NUMBER = "0.01.01";

	public static final String LOGTAG = "bob";

	/**
	 * @throws Exception 
	 */
	static Class getClass(String classOrFileName) throws Exception {
		String className = classOrFileName;
		// Strip endings if they were used
		if (classOrFileName.endsWith(".java")) {
			className = classOrFileName.substring(0, classOrFileName.length() - 5);
		}
		if (classOrFileName.endsWith(".class")) {
			className = classOrFileName.substring(0, classOrFileName.length() - 6);
		}
		try {
			Class<?> clazz = Class.forName(className);
			return clazz;
		} catch(ClassNotFoundException ex) {
			Pair2<File, File> klass = compileClass(classOrFileName);
			if (klass != null) {
				List<File> cpfiles = getSingleton().getSettings().getClasspathFiles();
//				classpath = Utils.isEmpty(cp)? null : Containers.
				// dynamically load a class from a file?
				Class clazz = ReflectionUtils.loadClassFromFile(klass.first, klass.second, cpfiles);
				return clazz;
			}
			throw ex;
		}
	}

	private static Pair<File> compileClass(String classOrFileName) throws Exception {
		// TODO can we compile it here and now?? But how would we load it?
		String fileName = classOrFileName;
		if ( ! classOrFileName.endsWith(".java")) fileName = classOrFileName+".java";
		File f = new File(fileName);
		// sniff package
		String src = FileUtils.read(f);
		String[] fnd = StrUtils.find("package (.+);", src);
		String cn = (fnd==null? "" : fnd[1]+".") + new File(FileUtils.getBasename(f)).getName();
		
		File tempDir = FileUtils.createTempDir();
		CompileTask cp = new CompileTask(null, tempDir);
		// classpath??
//		Map<String, String> env = System.getenv();
//		ClassLoader cl = ClassLoader.getSystemClassLoader();
		BobSettings _settings = getSingleton().getSettings();
		if (_settings.classpath!=null && ! _settings.classpath.isEmpty()) {
			Collection<File> cpfiles = Containers.apply(_settings.classpath, File::new);
			for (File file : cpfiles) {
				if ( ! file.exists()) {
					Log.w(LOGTAG, "Classpath file does not exist: "+file);
				}
			}
			// add in the Bob files
			String jcp = System.getProperty("java.class.path");
			if (jcp != null) {
				String[] jcps = jcp.split(":");
				for (String j : jcps) {
					cpfiles.add(new File(j));
				}
			}
			cp.setClasspath(cpfiles);
		}
		cp.setSrcFiles(f);
		cp.doTask();
		File klass = new File(tempDir, cn.replace('.', '/')+".class");
		if (klass.isFile()) {
			return new Pair<File>(tempDir, klass);
		}
		throw new FailureException("Bootstrap compile failed for "+classOrFileName+" = "+f);
	}

	private static void classNotFoundMessage(Throwable e) {
		System.out.println(e);
		// ?? better detection and error messages
		System.out
				.println("----------"
						+ StrUtils.LINEEND
						+ "This error can be caused by classpath settings. "
						+ "Are you using Ant, JUnit or email? These tasks require extra libraries on the classpath."
						+ StrUtils.LINEEND + "----------");
	}

	public static Time getLastRunDate(BuildTask buildTask) {
		assert buildTask != null;
		// relies on equals()
		Time t = time4task.get(buildTask);
		if (t != null)
			return t;
//		// FIXME BU != BU :(
//		for(BuildTask huh : time4task.keySet()) {
//			assert ! huh.equals(buildTask);
//		}
		// // From file?
		// File f = getFile(buildTask);
		// if (f != null && f.exists()) {
		// return f.lastModified();
		// }
		return TimeUtils.WELL_OLD;
	}

	public static Bob getSingleton() {
		if (dflt!=null) {
			return dflt;
		}
		// make it
		dflt = new Bob(new BobSettings());
		dflt.init();
		return dflt;
	}

	/**
	 * Take in a list of options and {@link BuildTask} class names
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		// Load settings
		ConfigFactory cf = ConfigFactory.get();
		cf.setArgs(args);
		ConfigBuilder cb = cf.getConfigBuilder(BobSettings.class);
		BobSettings _settings = cb.get();
		// Make Bob
		Bob bob = new Bob(_settings);
		dflt = bob;
		bob.init();
		
		List<String> argsLeft = cb.getRemainderArgs();
		
		if (argsLeft.size() == 0) {
			// find a file?
			File buildFile = findBuildScript();
			if (buildFile != null) {
				argsLeft = Arrays.asList(buildFile.toString());
			}						
		}
		
		if (argsLeft.size() == 0 || "--help".equals(args[0])) {			
			System.err.println(StrUtils.LINEEND + "Bob the Builder"
					+ StrUtils.LINEEND + "---------------"
					+ StrUtils.LINEEND
					+ "Usage: java -jar bob-all.jar [-cp CLASSPATH] [options] [TargetBuildTasks...]"
					+ StrUtils.LINEEND + new com.winterwell.utils.io.ArgsParser(bob.settings).getOptionsMessage());
			System.exit(1);
		}
		
		// Build each target
		for (String clazzName : argsLeft) {
			try {
				Class clazz = getClass(clazzName);
				bob.build(clazz);
			} catch (ClassNotFoundException e) {
				classNotFoundMessage(e);
				bob.maybeCarryOn(e);
			} catch (NoClassDefFoundError e) {
				classNotFoundMessage(e);
				bob.maybeCarryOn(e);
			} catch (Exception e) {		
				bob.maybeCarryOn(e);
			}
		}
	}

	private static File findBuildScript() {
		File baseDir = FileUtils.getWorkingDirectory();
		File bdir = new File(baseDir, "builder");
		if ( ! bdir.isDirectory()) {
			return null;
		}
		List<File> files = FileUtils.find(bdir, ".*Build.*\\.java");
		if (files.isEmpty()) return null;
		if (files.size()==1) {
			Log.w(LOGTAG, "Auto-build: found file "+files.get(0));
			return files.get(0);
		}
		Log.w(LOGTAG, "Auto-build: could not pick between files "+files);
		return null;
	}

	private void maybeCarryOn(Throwable e) {
		if (settings.ignoreAllExceptions) {
			Log.report(LOGTAG, "Ignoring: " + e.getMessage(), Level.WARNING);
		} else
			throw Utils.runtime(e);		
	}

	public static void setLastRunDate(BuildTask buildTask) {
		time4task.put(buildTask, new Time());
	}

	/**
	 * How many active BuildTasks are there?
	 */
	private final AtomicInteger bobCount = new AtomicInteger();

	private volatile boolean initFlag;

	private BobSettings settings;

	private LogFile logfile;

	private Bob(BobSettings settings) {
		this.settings = settings;
	}


	public int adjustBobCount(int dn) {
		return bobCount.addAndGet(dn);
	}

	public void build(Class clazz) throws Exception {
		Runnable script = (Runnable) clazz.newInstance();
		// Run it
		script.run();
		// Done
	}

	/**
	 * Restore System.out and System.err to normal.
	 */
	public void dispose() {
//		System.setOut(sysOut);
//		System.setErr(sysErr);
	}

	/**
	 * one file per class
	 * 
	 * @param buildTask
	 * @return
	 */
	File getLogFile(BuildTask buildTask) {
		init();
		// int hc = buildTask.hashCode();
		// String dash = hc > 0 ? "_" : ""; // if there is no -, just for
		// neatness
		// // of names
		String name = buildTask.getClass().getSimpleName()
		// + dash + hc
				+ ".log";
		return new File(settings.logDir, name);
	}

	public BobSettings getSettings() {
		return settings;
	}

	void init() {
		if (initFlag)
			return;
		initFlag = true;
		
		// ?? how do we want to log stuff??
		logfile = new LogFile(new File("bob.log"));
		
		try {
			settings.logDir.mkdirs();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		initTaskRunner();
	}

	private void initTaskRunner() {
		if ( ! Dep.has(TaskRunner.class)) {
			Dep.set(TaskRunner.class, new TaskRunner(10)); // TODO config num threads
		}
	}

	/**
	 * Delete all the run data.
	 */
	void resetAll() {
		init();
		FileUtils.deleteDir(settings.logDir);
		settings.logDir.mkdir();
	}

	public void setLogging(boolean on) {
		settings.loggingOff = !on;
	}

	@Deprecated // normally set by main()
	public void setSettings(BobSettings settings) {
		this.settings = settings;
	}

	public void close() {
		// clean up ops
		TaskRunner tr = Dep.get(TaskRunner.class);
		tr.shutdown();
	}

	public static Time getRunStart() {
		if (runStart==null) runStart = new Time();
		return runStart;
	}

}
