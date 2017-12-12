package com.winterwell.bob;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

import com.winterwell.utils.Dep;
import com.winterwell.utils.StrUtils;
import com.winterwell.utils.Utils;
import com.winterwell.utils.io.ArgsParser;
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
 * java -classpath bob.jar BobBuild MyBuildScript 
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

	private static final Bob dflt = new Bob();

	private static Map<BuildTask, Time> time4task = new HashMap<>();

	private static volatile Time runStart;

	public final static String VERSION_NUMBER = "0.01.01";

	public static final String LOGTAG = "bob";

	/**
	 * @throws ClassNotFoundException 
	 */
	private static Class getClass(String className) throws ClassNotFoundException {
		// Strip endings if they were used
		if (className.endsWith(".java")) {
			className = className.substring(0, className.length()
					- ".java".length());
		}
		if (className.endsWith(".class")) {
			className = className.substring(0, className.length()
					- ".class".length());
		}
		try {
			Class<?> clazz = Class.forName(className);
			return clazz;
		} catch(ClassNotFoundException ex) {
			// TODO can we compile it here and now??
			throw ex;
		}
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
		dflt.init();
		return dflt;
	}

	/**
	 * Take in a list of options and {@link BuildTask} class names
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		Bob bob = getSingleton();
		// // Load settings
		ArgsParser.getConfig(bob.settings, args, null, null);
		try {
			if (args.length == 0)
				throw new IOException();
		} catch (IOException e) {
			System.err.println(StrUtils.LINEEND + "Bob the Builder"
					+ StrUtils.LINEEND + "---------------"
					+ StrUtils.LINEEND
					+ "Usage: java -jar bob.jar [-cp CLASSPATH] [options] TargetBuildTasks..."
					+ StrUtils.LINEEND + new com.winterwell.utils.io.ArgsParser(bob.settings).getOptionsMessage());
			System.exit(1);
		}
		// Build each target
		for (String clazzName : args) {
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

//	Set<File> outputFiles = new HashSet<File>();

	private BobSettings settings = new BobSettings();

private LogFile logfile;

//	PrintStream sharedErrorStream;
//
//	PrintStream sharedOutputStream;
//
//	private PrintStream sysErr;
//
//	private PrintStream sysOut;

	private Bob() {
	}

	@Deprecated
	public void addOutputFile(File file) {
		// Add to shared output
//		outputFiles.add(file);
	}

	public int adjustBobCount(int dn) {
		return bobCount.addAndGet(dn);
	}

	public void build(Class clazz) throws Exception {
		BuildTask script = (BuildTask) clazz.newInstance();
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
		
		// for dispose() to restore
//		sysOut = System.out;
//		sysErr = System.err;
//		// set wrapped out and err
//		sharedOutputStream = new IndentedStream(new SharedOutputStream(this,
//				System.out));
//		sharedErrorStream = new IndentedStream(new SharedOutputStream(this,
//				System.err));
//		System.setOut(sharedOutputStream);
//		System.setErr(sharedErrorStream);

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

	@Deprecated
	public void removeOutputFile(File file) {
//		sharedOutputStream.flush();
//		sharedErrorStream.flush();
//		outputFiles.remove(file);
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

///**
// * Add indents to the output messages.
// * 
// * @author daniel
// * 
// */
//class IndentedStream extends PrintStream {
//
//	/**
//	 * Avoid writing multiple indents due to multiple flushes.
//	 */
//	private boolean empty = true;
//
//	public IndentedStream(OutputStream out) {
//		super(out);
//	}
//
//	@Override
//	public void flush() {
//		super.flush();
//		if (!empty) {
//			String indent = Environment.get().get(Printer.INDENT);
//			try {
//				write(indent.getBytes());
//			} catch (IOException e) {
//				setError();
//			}
//			empty = true;
//		}
//	}
//
//	@Override
//	public void write(byte[] b) throws IOException {
//		super.write(b);
//		if (b.length != 0)
//			empty = false;
//	}
//
//	@Override
//	public void write(byte[] b, int off, int len) {
//		super.write(b, off, len);
//		if (len != 0)
//			empty = false;
//	}
//
//	@Override
//	public void write(int b) {
//		super.write(b);
//		empty = false;
//	}
//
//}

///**
// * Send output to normal output + files.
// * 
// * @author daniel
// * 
// */
//class SharedOutputStream extends PrintStream {
//	Bob bob;
//	ByteArrayOutputStream buffer = new ByteArrayOutputStream();
//
//	public SharedOutputStream(Bob bob, OutputStream out) {
//		super(out, true);
//		this.bob = bob;
//	}
//
//	// StringBuilder buffer = new StringBuilder();
//	@Override
//	public void flush() {
//		super.flush();
//		byte[] b = buffer.toByteArray();
//		int len = b.length; // buffer.getSize();
//		buffer = new ByteArrayOutputStream();
//		// Save to files
//		for (File f : bob.outputFiles) {
//			try {
//				// open as append
//				FileOutputStream fout = new FileOutputStream(f, true);
//				fout.write(b, 0, len);
//				fout.close();
//			} catch (IOException e) {
//				// Oh dear - don't print to System.out as that could create a
//				// loop
//				PrintStream dump = new PrintStream(out);
//				e.printStackTrace(dump);
//				dump.flush();
//			}
//		}
//	}
//
//	@Override
//	public void write(byte[] b) throws IOException {
//		super.write(b);
//		if (bob.getSettings().loggingOff)
//			return;
//		buffer.write(b);
//	}
//
//	@Override
//	public void write(byte[] buf, int off, int len) {
//		super.write(buf, off, len);
//		if (bob.getSettings().loggingOff)
//			return;
//		buffer.write(buf, off, len);
//	}
//
//	@Override
//	public void write(int c) {
//		super.write(c);
//		if (bob.getSettings().loggingOff)
//			return;
//		buffer.write(c);
//	}
//
//}

///**
// * TODO
// * 
// * @author daniel
// * 
// */
//class SpecialClassLoader extends ClassLoader {
//	// TODO public Class loadClass(String name) {
//	// Class<?> clazz = null;
//	// // Full name
//	// try {
//	// clazz = Class.forName(name);
//	// return clazz;
//	// } catch (ClassNotFoundException ex) {
//	// // oh well
//	// }
//	// // Short name
//	// int i = name.lastIndexOf('.');
//	// String shortName = name;
//	// if (i!=-1) {
//	// shortName = name.substring(i+1);
//	// try {
//	// clazz = Class.forName(shortName);
//	// return clazz;
//	// } catch (ClassNotFoundException ex) {
//	// // oh well
//	// }
//	// }
//	// // Local Java file?
//	// File jf = new File(shortName+".java");
//	// clazz = compileAndLoadClass(jf);
//	// if (clazz != null) return clazz;
//	//
//	// return null;
//	// }
//
//	// /**
//	// *
//	// * @param javaFile
//	// * @return Class object created from javaFile, or null
//	// * Note: As a special case, attempting to create {@link CompileTask}Config
//	// always returns null.
//	// */
//	// private Class<?> compileAndLoadClass(File javaFile) {
//	// if ( ! javaFile.exists()) return null;
//	// // Special case: prevent loops
//	// if (javaFile.getName().endsWith(CompileTask.class.getSimpleName()+
//	// "Config.java")) {
//	// return null;
//	// }
//	// // Temp output dir
//	// File tempOut;
//	// try {
//	// tempOut = File.createTempFile("bob", "dir");
//	// } catch (IOException e) {
//	// throw new WrappedException(e);
//	// }
//	// FileUtils.delete(tempOut);
//	// boolean ok = tempOut.mkdirs();
//	// if ( ! ok) throw new WrappedException("Could not create "+tempOut);
//	// // Compile
//	// CompileTask comp = new CompileTask(Arrays.asList(javaFile), tempOut);
//	// comp.run();
//	// ClassLoader loader = new SpecialClassLoader(tempOut);
//	// String cName = javaFile.getName().substring(0,
//	// javaFile.getName().length()-5);
//	// try {
//	// Class<?> clazz = loader.loadClass(cName);
//	// return clazz;
//	// } catch (ClassNotFoundException e) {
//	// return null;
//	// }
//	// }
//
//	private final File binDir;
//
//	public SpecialClassLoader(File tempOut) {
//		binDir = tempOut;
//	}
//
//	@Override
//	protected URL findResource(String name) {
//		File f = new File(binDir, name);
//		try {
//			return f.toURI().toURL();
//		} catch (MalformedURLException e) {
//			return null;
//		}
//	}
//
//}
