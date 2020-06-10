package com.winterwell.bob;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.regex.Pattern;

import com.winterwell.bob.tasks.Classpath;
import com.winterwell.bob.tasks.GitBobProjectTask;
import com.winterwell.utils.Dep;
import com.winterwell.utils.Printer;
import com.winterwell.utils.StrUtils;
import com.winterwell.utils.Utils;
import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.utils.containers.Containers;
import com.winterwell.utils.io.CSVReader;
import com.winterwell.utils.io.CSVSpec;
import com.winterwell.utils.io.CSVWriter;
import com.winterwell.utils.io.ConfigBuilder;
import com.winterwell.utils.io.ConfigFactory;
import com.winterwell.utils.io.FileUtils;
import com.winterwell.utils.io.LineReader;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.log.LogFile;
import com.winterwell.utils.threads.TaskRunner;
import com.winterwell.utils.time.Time;
import com.winterwell.utils.time.TimeUtils;
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

	/**
	 * end time for tasks, including other runs loaded in from boblog
	 * Key is Desc-id
	 */
	private static Map<String, Time> time4task;
	/**
	 * end time for tasks, but only within this JVM
	 */
	private static final Set<String> taskThisJVMOnly = new HashSet();

	private static volatile Time runStart;

	public static String LOGTAG = "bob";


	
	Classpath getClasspath() {
		BobConfig _settings = getSingleton().getConfig();
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
			loadTaskHistory();
		}
		// relies on equals()
		String id = buildTask.getDesc().getId();
		Time t = time4task.get(id);
		if (t != null) {
			return t;
		}
		return TimeUtils.WELL_OLD;
	}
	
	public static boolean isRunAlreadyThisJVM(BuildTask buildTask) {
		assert buildTask != null;
		String id = buildTask.getDesc().getId();
		return taskThisJVMOnly.contains(id);
	}

	
	/**
	 * Sets time4task
	 * Repeated calls will do a fresh load from file.
	 * This is to allow info from forked bobs to be loaded in.
	 */
	public static void loadTaskHistory() {
		// load from file
		try {
			ArrayMap<String,Time> t4t = new ArrayMap();
			File csvfile = BobLog.getHistoryFile();
			if (csvfile.exists()) {
				CSVSpec spec = new CSVSpec(',', '"', '#');
				CSVReader r = new CSVReader(csvfile, spec).setNumFields(-1);
				for (String[] row : r) {
					try {
						t4t.put(row[0], new Time(row[1]));
					} catch(Exception ex) {
						Log.e(LOGTAG, ex);
					}
				}
				r.close();
			}			
			time4task = t4t;			
		} catch(Throwable ex) {
			Log.d(LOGTAG, ex);
			if (time4task==null) time4task = new HashMap();
		}		
	}

	public static Bob getSingleton() {
		if (dflt!=null) {
			return dflt;
		}
		// make it
		dflt = new Bob(new BobConfig());
		dflt.init();
		return dflt;
	}

	/**
	 * Take in a list of options and {@link BuildTask} class names
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		Bob bob = null;
		try {
			System.out.println("Bob the Builder   version: "+BobConfig.VERSION_NUMBER+StrUtils.LINEEND);
			
			// Load settings
			ConfigFactory cf = ConfigFactory.get();
			cf.setArgs(args);
			// ...system-wide Bob settings
			ConfigBuilder cb = cf.getConfigBuilder(BobConfig.class);
			File warehouse = GitBobProjectTask.getGitBobDir();
			File bcf = new File(warehouse, "bob.properties");
			if (bcf.isFile()) cb.set(bcf);
			// ...load
			BobConfig _settings = cb.get();
			if ( ! Utils.isBlank(_settings.label)) {
				LOGTAG += "."+_settings.label;
			}
			
			// ??should update be "update" not "-update"??
			// Just update the jar?
			if (_settings.update) {
				doUpdate();
				return;
			}
			// forget some history?
			if ( ! Utils.isBlank(_settings.forget)) {
				doForget(_settings.forget);
				return;
			}
			
			// What to build?
			List<String> argsLeft = cb.getRemainderArgs();
			BobScriptFactory bsf = new BobScriptFactory(FileUtils.getWorkingDirectory());
			
			if (argsLeft.size() == 0) {
				// find a file?
				File buildFile = bsf.findBuildScript(null);
				if (buildFile != null) {
					argsLeft = Arrays.asList(buildFile.toString());
				}
			}
			
			if (_settings.help || argsLeft.size() == 0 || Containers.contains("--help", args)) {
				System.err.println(StrUtils.LINEEND + "Bob the Builder   version: "+BobConfig.VERSION_NUMBER
						+ StrUtils.LINEEND + "---------------"
						+ StrUtils.LINEEND
						+ "Default usage (looks for a BuildX.java file in the builder directory):"+ StrUtils.LINEEND
						+ "	java -jar bob-all.jar"+ StrUtils.LINEEND
						+ StrUtils.LINEEND
						+ "Usage: java -jar bob-all.jar [options] [TargetBuildTasks...]"
						+ StrUtils.LINEEND + cb.getOptionsMessage());
				System.exit(1);
			}		
			Log.d(LOGTAG, "Bob version: "+BobConfig.VERSION_NUMBER+" building "+argsLeft+"...");
	
			// Make Bob
			bob = new Bob(_settings);
			dflt = bob;
			bob.init();
			
			// clean dot?
			if (bob.config.dotFile != null && bob.config.depth==0) {
				BobLog.logDot("\n\ndigraph Tasks"+argsLeft.get(0)+" {\n");
			}
			
			// Build each target					
			for (String clazzName : argsLeft) {
				try {
					Class clazz = bsf.getClass(clazzName);
					Log.i(LOGTAG, "Build script loaded: "+clazz);
					
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
			
			if (bob.config.dotFile != null && bob.config.depth==0) {
				BobLog.logDot("}\n\n");
			}
			
			// report
			if (bob.lastScript instanceof BuildTask) {
				Map success = ((BuildTask) bob.lastScript).getReport();
				if (success!=null && ! success.isEmpty()) {
					System.out.println(StrUtils.LINEEND+Printer.toString(success, StrUtils.LINEEND, ":\t"));
				}
			}
		} catch(Throwable ex) {
			// make sure everything is closed 
			TaskRunner tr = Dep.get(TaskRunner.class);
			tr.shutdownNow();
			// finish with debug output
			System.err.println(Printer.toString(ex, true));
			if (bob!=null) {
				Log.e(LOGTAG, "\n\n	ERROR EXIT for "+bob.lastScript);
				Log.e(LOGTAG, "\n"+ex.toString()+"\n"); // a short error - hopefully a BobBuildException with a stack of tasks
			}
			// send a bleurgh code out
			System.exit(1);
		}
	}

	private static void doForget(String forget) throws IOException {
		time4task = null; // should be null anyway, but just in case - this will ensure a reload

		File csvfile = BobLog.getHistoryFile();
		File csvfile2 = File.createTempFile("bobhistory", ".csv");
		BufferedWriter w = FileUtils.getWriter(csvfile2);
		LineReader lr = new LineReader(csvfile);
		Pattern p = Pattern.compile(forget);
		for (String string : lr) {
			if (p.matcher(string).find()) {
				Log.d(LOGTAG, "forget "+string);
				continue;
			}
			w.write(string); w.write(StrUtils.LINEEND);			
		}
		w.close();
		lr.close();
		FileUtils.move(csvfile2, csvfile);
	}

	private static void doUpdate() {
		// update Bob itself?
		FakeBrowser fb = new FakeBrowser();
		fb.setMaxDownload(50); // 50mb?!
		File tbobJar = fb.getFile("https://www.winterwell.com/software/downloads/bob-all.jar");
		File bobJar = new File(tbobJar.getParentFile(), "bob-all.jar");
		if ( ! tbobJar.equals(bobJar)) {
			FileUtils.move(tbobJar, bobJar);
		}
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

	private void maybeCarryOn(Throwable e) throws RuntimeException {
		if (config.ignoreAllExceptions) {
			Log.report(LOGTAG, "Ignoring: " + e.getMessage(), Level.WARNING);
		} else {
			throw Utils.runtime(e);
		}
	}

	public static void setLastRunDate(BuildTask buildTask) {
		if (time4task==null) {
			loadTaskHistory();
		}
		String id = buildTask.getDesc().getId();
		Time now = new Time();
		time4task.put(id, now);
		taskThisJVMOnly.add(id);
//		assert buildTask.skip() : buildTask;
		// TODO save in a slow thread??
		
		File csvfile = BobLog.getHistoryFile();
		CSVWriter w = new CSVWriter(csvfile, ',', true);
		w.write(id, now, buildTask.toString());		
		w.close(); //flush the edit		
	}

	/**
	 * How many active BuildTasks are there?
	 */
	private final AtomicInteger bobCount = new AtomicInteger();

	private volatile boolean initFlag;

	private BobConfig config;

	private LogFile logfile;

	private Runnable lastScript;

	private Bob(BobConfig settings) {
		this.config = settings;
	}


	public int adjustBobCount(int dn) {
		return bobCount.addAndGet(dn);
	}
	
	/**
	 * @return active tasks
	 */
	public int getBobCount() {
		return bobCount.get();
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
		return new File(config.logDir, name);
	}

	public BobConfig getConfig() {
		return config;
	}

	void init() {
		if (initFlag)
			return;
		initFlag = true;
		
		// ?? how do we want to log stuff??
		logfile = new LogFile(new File("bob.log"));
		
		try {
			config.logDir.mkdirs();
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
		FileUtils.deleteDir(config.logDir);
		config.logDir.mkdir();
	}

	public void setLogging(boolean on) {
		config.loggingOff = !on;
	}

	@Deprecated // normally set by main()
	public void setConfig(BobConfig settings) {
		this.config = settings;
	}

	public void close() {		
		// clean up ops
		TaskRunner tr = Dep.get(TaskRunner.class);
		Log.d(LOGTAG, "close... active-tasks: "+tr.getQueueSize()+" "+tr.getTodo());
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
