package com.winterwell.bob;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

import com.winterwell.bob.tasks.Classpath;
import com.winterwell.bob.tasks.CompileTask;
import com.winterwell.utils.Dep;
import com.winterwell.utils.FailureException;
import com.winterwell.utils.Printer;
import com.winterwell.utils.ReflectionUtils;
import com.winterwell.utils.StrUtils;
import com.winterwell.utils.Utils;
import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.utils.containers.Containers;
import com.winterwell.utils.containers.Pair;
import com.winterwell.utils.containers.Pair2;
import com.winterwell.utils.io.ConfigBuilder;
import com.winterwell.utils.io.ConfigFactory;
import com.winterwell.utils.io.FileUtils;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.log.LogFile;
import com.winterwell.utils.threads.TaskRunner;
import com.winterwell.utils.time.Time;
import com.winterwell.utils.time.TimeUtils;
import com.winterwell.utils.web.SimpleJson;
import com.winterwell.web.FakeBrowser;

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

	private static Map<String, Time> time4task;

	private static volatile Time runStart;

	public static final String LOGTAG = "bob";

	/**
	 * @throws Exception 
	 */
	static Class getClass(String classOrFileName) throws Exception {
		try {
			return getClass2(classOrFileName);
		} catch(Exception ex) {
			// partial name? try a find
			File buildFile = findBuildScript(classOrFileName);
			if (buildFile!=null && ! buildFile.toString().equals(classOrFileName)) {
				return getClass2(buildFile.toString());
			}
			throw ex;
		}
	}
	
	static Class getClass2(String classOrFileName) throws Exception {
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
			// is it a directory?
			File dir = new File(classOrFileName);
			if (dir.isDirectory()) {
				File found = findBuildScript2(dir, null);
				if (found!=null) {
					Log.d(LOGTAG, "located build-script "+found+" in directory "+dir);
					classOrFileName = found.toString();
				}
			}
			
			Pair2<File, File> klass = compileClass(classOrFileName);
			if (klass != null) {
				// classpath
				List<File> cpfiles = getSingleton().getClasspath().getFiles();
//				classpath = Utils.isEmpty(cp)? null : Containers.
				// dynamically load a class from a file?
				Class clazz = ReflectionUtils.loadClassFromFile(klass.first, klass.second, cpfiles);
				return clazz;
			}
			throw ex;
		}
	}

	/**
	 * From a .java file or fully-qualified classname,
	 * compile it to a .class file 
	 * @param classOrFileName
	 * @return (temp-output-dir, class-file)
	 * @throws Exception
	 */
	private static Pair<File> compileClass(String classOrFileName) throws Exception {
		// TODO can we compile it here and now?? But how would we load it?
		// 1. Look for the .java file		
		String fileName = classOrFileName;
		File f = new File(fileName);
		if ( ! f.isFile()) {
			// classname? turn into a file
			fileName = fileName.replace('.', '/');
			// .java ending
			if (fileName.endsWith("/java")) {
				fileName = fileName.substring(0, fileName.length()-5)+".java";
			}
			if ( ! fileName.endsWith(".java")) {
				fileName = fileName+".java";
			}
			f = new File(fileName);
			if ( f.isDirectory()) {
				throw new IllegalArgumentException(f+" from "+classOrFileName+" should have been handled via find-build-script");
			}
			if ( ! f.isFile()) {
				throw new FileNotFoundException(f+" = "+f.getAbsolutePath()+" from "+classOrFileName);
			}
		}
		
		// sniff package
		String src = FileUtils.read(f);
		String[] fnd = StrUtils.find("package (.+);", src);
		// full classname
		String className = (fnd==null? "" : fnd[1]+".") + new File(FileUtils.getBasename(f)).getName();
		
		File tempDir = FileUtils.createTempDir();
		CompileTask cp = new CompileTask(null, tempDir);
		// classpath
		Classpath claspath = Bob.getSingleton().getClasspath();
		cp.setClasspath(claspath);
		// our .java file to compile
		cp.setSrcFiles(f);
		// ...compile
		cp.doTask();
		File klass = new File(tempDir, className.replace('.', '/')+".class");
		if (klass.isFile()) {
			return new Pair<File>(tempDir, klass);
		}
		throw new FailureException("Bootstrap compile failed for "+classOrFileName+" = "+f);
	}

	private Classpath getClasspath() {
		BobSettings _settings = getSingleton().getSettings();
		Classpath cpfiles;
		if (_settings.classpath!=null && ! _settings.classpath.isEmpty()) {
			cpfiles = new Classpath(_settings.classpath);
			for (File file : cpfiles.getFiles()) {
				if ( ! file.exists()) {
					Log.w(LOGTAG, "Classpath file does not exist: "+file);
				}
			}
		} else {
			cpfiles = new Classpath();
		}
		// add in the Bob files
		Classpath bobcp = Classpath.getSystemClasspath();
		cpfiles.addAll(bobcp.getFiles());
		return cpfiles;
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
		if (time4task==null) {
			time4task = loadTaskHistory();
		}
		// relies on equals()
		String id = buildTask.getDesc().getId();
		Time t = time4task.get(id);
		if (t != null) {
			return t;
		}
		return TimeUtils.WELL_OLD;
	}

	private static Map<String, Time> loadTaskHistory() {
		// load from file
		try {
			File file = getHistoryFile();
			if ( ! file.isFile()) {
				return new HashMap();
			}
			String json = FileUtils.read(file);
			SimpleJson sj = new SimpleJson();
			Map jobj = (Map) sj.fromJson(json);
			ArrayMap<String,Time> t4t = new ArrayMap();
			for(Object id : jobj.keySet()) {
				Object v = jobj.get(id);
				Time time = v instanceof Time? (Time) v : new Time(v.toString());
				t4t.put(id.toString(), time);
			}
			return t4t;			
		} catch(Throwable ex) {
			Log.d(LOGTAG, ex);
			return new HashMap();
		}		
	}

	private static void saveTaskHistory() {
		try {
			File file = getHistoryFile();
			SimpleJson sj = new SimpleJson(); // not our favourite, but Jetty JSON was causing weird breakage
			String json = sj.toJson(time4task);
			FileUtils.write(file, json);
		} catch(Throwable ex) {
			Log.d(LOGTAG, "Warning: saveTaskHistory failed: "+ex);
		}		
	}
	
	static File getHistoryFile() {
		BobSettings _settings = Bob.dflt==null? new BobSettings() : Bob.dflt.settings;
		File file = new File(_settings.logDir, "time4task.json");
		return file;
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
		System.out.println("Bob the Builder   version: "+BobSettings.VERSION_NUMBER+StrUtils.LINEEND);
		
		// Load settings
		ConfigFactory cf = ConfigFactory.get();
		cf.setArgs(args);
		ConfigBuilder cb = cf.getConfigBuilder(BobSettings.class);
		BobSettings _settings = cb.get();
		
		if (_settings.update) {
			doUpdate();
			return;
		}
		
		List<String> argsLeft = cb.getRemainderArgs();
		
		if (argsLeft.size() == 0) {
			// find a file?
			File buildFile = findBuildScript(null);
			if (buildFile != null) {
				argsLeft = Arrays.asList(buildFile.toString());
			}
		}
		
		if (argsLeft.size() == 0 || Containers.contains("--help", args)) {
			System.err.println(StrUtils.LINEEND + "Bob the Builder   version: "+BobSettings.VERSION_NUMBER
					+ StrUtils.LINEEND + "---------------"
					+ StrUtils.LINEEND
					+ "Default usage (looks for a BuildX.java file in the builder directory):"+ StrUtils.LINEEND
					+ "	java -jar bob-all.jar"+ StrUtils.LINEEND
					+ StrUtils.LINEEND
					+ "Usage: java -jar bob-all.jar [options] [TargetBuildTasks...]"
					+ StrUtils.LINEEND + cb.getOptionsMessage());
			System.exit(1);
		}		
		Log.d(LOGTAG, "Bob version: "+BobSettings.VERSION_NUMBER+" building "+argsLeft+"...");

		// Make Bob
		Bob bob = new Bob(_settings);
		dflt = bob;
		bob.init();
				
		// Build each target
		for (String clazzName : argsLeft) {
			try {
				Class clazz = getClass(clazzName);
				bob.build(clazz);
				bob.built.add(clazz);
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
		
		// all done
		bob.close();
		
		// report
		if (bob.lastScript instanceof BuildTask) {
			Map success = ((BuildTask) bob.lastScript).getReport();
			if (success!=null && ! success.isEmpty()) {
				System.out.println(StrUtils.LINEEND+Printer.toString(success, StrUtils.LINEEND, ":\t"));
			}
		}
	}

	private static void doUpdate() {
		// update Bob itself?
		FakeBrowser fb = new FakeBrowser();
		fb.setMaxDownload(50); // 50mb?!
		File bobJar = fb.getFile("https://www.winterwell.com/software/downloads/bob-all.jar");
		System.out.println("Bob jar downloaded to:");
		System.out.println(bobJar);
		// HACK
		File wwbobjar = new File(FileUtils.getWinterwellDir(), "open-code/winterwell.bob/bob-all.jar");
		if (wwbobjar.isFile()) {
			FileUtils.move(wwbobjar, FileUtils.changeType(wwbobjar, ".jar.old"));
			FileUtils.move(bobJar, wwbobjar);
			System.out.println("Bob jar moved to:\n"+wwbobjar);
		}
	}

	private static File findBuildScript(String optionalName) {
		File baseDir = FileUtils.getWorkingDirectory();
		return findBuildScript2(baseDir, optionalName);
	}
	
	/**
	 * ??how best to expose this method
	 * @param projectDir
	 * @param optionalName
	 * @return
	 */
	public static File findBuildScript2(File projectDir, String optionalName) {
		File bdir = new File(projectDir, "builder");
		if ( ! bdir.isDirectory()) {
			return null;
		}
		String namePattern = ".*Build.*\\.java";
		if (optionalName!=null) {
			namePattern = ".*"+optionalName+"\\.java";
		}
		List<File> files = FileUtils.find(bdir, namePattern);
		if (files.isEmpty()) return null;
		if (files.size()==1) {
			Log.w(LOGTAG, "Auto-build: found file "+files.get(0));
			return files.get(0);
		}
		Log.w(LOGTAG, "Auto-build: could not pick between "+files.size()+" tasks in "+bdir);
		Log.w(LOGTAG, Containers.apply(files, FileUtils::getBasename));
		return null;
	}

	private void maybeCarryOn(Throwable e) {
		if (settings.ignoreAllExceptions) {
			Log.report(LOGTAG, "Ignoring: " + e.getMessage(), Level.WARNING);
		} else
			throw Utils.runtime(e);		
	}

	public static void setLastRunDate(BuildTask buildTask) {
		if (time4task==null) {
			time4task = loadTaskHistory();
		}
		String id = buildTask.getDesc().getId();
		time4task.put(id, new Time());
//		assert buildTask.skip() : buildTask;
		// TODO save in a slow thread??
		saveTaskHistory();
	}

	/**
	 * How many active BuildTasks are there?
	 */
	private final AtomicInteger bobCount = new AtomicInteger();

	private volatile boolean initFlag;

	private BobSettings settings;

	private LogFile logfile;

	private Runnable lastScript;

	private Bob(BobSettings settings) {
		this.settings = settings;
	}


	public int adjustBobCount(int dn) {
		return bobCount.addAndGet(dn);
	}

	void build(Class clazz) throws Exception {
		Runnable script = (Runnable) clazz.newInstance();
		lastScript = script;
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
		Log.d(LOGTAG, "close... active-tasks: "+tr.getQueueSize());
		tr.shutdown();
		tr.awaitTermination();
		Log.d(LOGTAG, "...closed");
		
		Log.i(LOGTAG, "----- BUILD COMPLETE: "+built+" -----");
	}
	
	List<Class> built = new ArrayList();

	public static Time getRunStart() {
		if (runStart==null) runStart = new Time();
		return runStart;
	}

}
