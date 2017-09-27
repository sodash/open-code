package com.winterwell.bob;

import java.io.Closeable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Collections;
import java.util.logging.Level;

import org.junit.Test;

import com.winterwell.utils.Dep;
import com.winterwell.utils.Printer;
import com.winterwell.utils.TimeOut;
import com.winterwell.utils.Utils;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.threads.ATask;
import com.winterwell.utils.threads.TaskRunner;
import com.winterwell.utils.time.Dt;
import com.winterwell.utils.time.TimeUtils;

/**
 * A task forming part or all of a build process. Subclass this to create build
 * tasks.
 * 
 * <h2>Guidelines for BuildTasks</h2>
 * <p>
 * A BuildTask is just a Java class and you can implement sub-classes however
 * you want.
 * </p>
 * <p>
 * However when sub-classing BuildTask there are a couple of guidelines you
 * should follow:
 * <ul>
 * <li>If a field is used for calculation (rather than storing settings), it
 * should be marked with the <code>transient</code> modifier.</li>
 * <li>Tasks should not perform any actual computation until {@link #doTask()}
 * is called. If you perform computation before then, the state of the world may
 * have changed by the time doTask() is called, which could produce confusing
 * results.</li>
 * </ul>
 * </p>
 * 
 * <h3>hashCode() and transient</h3>
 * <p>
 * Hashcodes are important - they are used to prevent tasks from being rerun.
 * <i>The hashcode is calculated using all of your object's non-transient
 * non-static fields</i>. Therefore be careful to mark fields as
 * <code>transient</code> if they should be excluded from the hashcode. Failure
 * to do so is not catastrophic, but it will lead to tasks being rerun
 * unnecessarily.
 * </p>
 * 
 * <h2>Relationship with JUnit 4</h2>
 * <p>
 * BuildTask uses an @Test annotation to provides a convenient way to run
 * BuildTasks from JUnit-enabled systems such as Eclipse. You can run a
 * BuildTask as a JUnit test (and if an exception is thrown this will show up as
 * a unit-test failure). BuildTask does not actually use JUnit, and JUnit
 * methods should be ignored. Yes this is a bit of a hack, but it works well for
 * me.
 * </p>
 * <p>To run from the command line: see {@link Bob} or bob.sh</p>
 * @author daniel
 * 
 */
public abstract class BuildTask implements Closeable {

	private IErrorHandler errorHandler;

	private Level verbosity;
	
	/**
	 * Allows per-task tweaking of loggin settings.
	 * @param verbosity E.g. Level.WARNING would ignore all Level.INFO reports.
	 */
	public void setVerbosity(Level verbosity) {
		this.verbosity = verbosity;
	}
	
	/**
	 * Use with {@link #setErrorHandler(IErrorHandler)} to swallow
	 * exceptions from this task.
	 */
	public static IErrorHandler IGNORE_EXCEPTIONS = new IErrorHandler() {		
		@Override
		public void handle(Throwable ex) throws Exception {
			System.err.println(ex);
		}
	};
	
	/**
	 * @param handler null by default (= throw exceptions).
	 * Can be set to IGNORE
	 */
	public void setErrorHandler(IErrorHandler handler) {
		this.errorHandler = handler;
	}
	
	/**
	 * Context-key for a File indicating the directory that should be considered
	 * as local.
	 */
	public static final String LOCAL_DIR = "LOCAL_DIR";

	transient Bob bob = Bob.getSingleton();

	transient private int hashcode;

	transient long lastRunDate;

	protected BuildTask() {
		config();
	}

	/**
	 * TODO Load a config class getClass()+"Config" for settings actions
	 * provided via an init method 1st try qualified class name, then simple
	 * name
	 */
	private void config() {
	}

	/**
	 * Performs the actual work of the class. 
	 * Mostly you will want to call the wrapper method {@link #run()} instead.
	 * 
	 * @throws Exception
	 */
	protected abstract void doTask() throws Exception;

	/**
	 * Equals() if same class and all non-transient fields are equals.
	 */
	@Override
	public final boolean equals(Object obj) {
		if (obj.getClass() != getClass()) return false;		
		try {
			return equals2(this, obj, getClass());
		} catch (Exception e) {
			throw Utils.runtime(e);
		}		
	}

	private boolean equals2(Object a, Object b, Class klass) throws Exception {
		// Loop through fields
		Field[] fields = getClass().getDeclaredFields();
		for (Field field : fields) {
			// Ignore transient and static fields
			int mod = field.getModifiers();
			if (Modifier.isTransient(mod) || Modifier.isStatic(mod))
				continue;
			// Get the value
			field.setAccessible(true);
			Object v1 = field.get(a);
			Object v2 = field.get(b);
			if ( ! Utils.equals(v1, v2)) {
				return false;
			}
		}
		// Recurse
		Class sClazz = klass.getSuperclass();
		if (sClazz == null) return true;
		return equals2(a, b, sClazz);
	}

	/**
	 * @return The build tasks this task depends on. Dependencies will be run
	 *         first. Returns an empty list by default - override to specify
	 *         some dependencies. null is also acceptable.
	 */
	public Collection<? extends BuildTask> getDependencies() {
		return Collections.emptyList();
	}

	public final long getLastRunDate() {
		// Lazy setting to make sure getId() has all its vars set by the
		// subclass, if overridden
		if (lastRunDate == 0) {
			lastRunDate = Bob.getLastRunDate(this);
		}
		return lastRunDate;
	}

	/**
	 * @return the objects which will trigger this script to run. E.g. if a file
	 *         has changed since the script was last run. The dependencies also
	 *         count as triggers (there is no need to include them here). null
	 *         indicates that this script should always be run.
	 */
	public Collection<? extends Object> getTriggers() {
		return null;
	}

	private void handleException(Throwable e) {
		if (bob.getSettings().ignoreAllExceptions) {
			System.out.println("Ignoring: " + e);
			return;
		}
		if (errorHandler!=null) {
			try {
				errorHandler.handle(e);
				return;
			} catch (Exception e2) {
				throw Utils.runtime(e2);		
			}
		}
		throw Utils.runtime(e);		
	}

	@Override
	public final int hashCode() {
		if (hashcode != 0)
			return hashcode;
		try {
			hashcode = hashCode2(getClass());
			return hashcode;
		} catch (IllegalAccessException e) {
			throw new IllegalArgumentException(
					"BuildTask requires access to all fields for hashcode calculations. This has failed: "
							+ e.getMessage());
		}
	}

	int hashCode2(Class clazz) throws IllegalAccessException {
		int code = 1;
		int prime = 17;
		// Loop through fields
		Field[] fields = clazz.getDeclaredFields();
		for (Field field : fields) {
			// Ignore transient and static fields
			int mod = field.getModifiers();
			if (Modifier.isTransient(mod) || Modifier.isStatic(mod))
				continue;
			// Get the value
			field.setAccessible(true);
			Object v = field.get(this);
			code = code * prime;
			if (v != null)
				code += v.hashCode();
		}
		// Recurse
		Class sClazz = clazz.getSuperclass();
		if (sClazz == null || BuildTask.class.equals(sClazz))
			return code;
		return 3 * code + hashCode2(sClazz);
	}
	
	Dt maxTime;

	protected String LOGTAG = Bob.LOGTAG+"."+getClass().getSimpleName();
	
	/**
	 * null by default. 
	 * If not null, this sets a timeout on the task (including any dependencies it calls!).
	 * @param maxTime
	 */
	public void setMaxTime(Dt maxTime) {
		this.maxTime = maxTime;
	}
	
	/**
	 * Like run, but it uses the Bob thread pool to run in parallel.
	 */
	public void runInThread() {
		TaskRunner taskRunner = Dep.get(TaskRunner.class);
		ATask atask = new ATask<Object>(getClass().getSimpleName()) {
			@Override
			protected Object run() throws Exception {
				BuildTask.this.run();
				return null;
			}
			
		};
		taskRunner.submitIfAbsent(atask);
	}

	/**
	 * Call this to run the task!
	 * This will first run the dependencies. 
	 * Then check the last-run date (& possibly quit if the task has already run).
	 * If not, it will call {@link #doTask()}
	 * <p> 
	 * Exceptions are usually thrown.
	 * Though they may be ignored (see {@link BobSettings#ignoreAllExceptions})
	 * or handled by a custom error-handler (see {@link #setErrorHandler(IErrorHandler)}).
	 * 
	 * @param context
	 * @throws RuntimeException
	 *             Exceptions are wrapped with RuntimeException and rethrown,
	 *             unless Bob ??.
	 */
	@Test
	public final void run() throws RuntimeException {
		// Add an output and error listener
		if (verbosity!=Level.OFF) bob.addOutputFile(bob.getLogFile(this));
		report("Running " + toString() + " at "
				+ TimeUtils.getTimeStamp() + "...", Level.FINE);
		bob.adjustBobCount(1);
		// Add an extra indent to log messages
		Printer.addIndent("   "); // FIXME something dodgy with the indents; oh
		// well
//		System.out.flush(); // Otherwise new indent is not written for 1st line
//		System.err.flush(); // Otherwise new indent is not written for 1st line
		TimeOut timeOut = null;
		try {
			if (maxTime!=null) timeOut = new TimeOut(maxTime.getMillisecs());
			// call dependencies
			long depLrd = 0;
			// run
			Collection<? extends BuildTask> deps = getDependencies();
			if (deps!=null) {
				for (BuildTask bs : deps) {
					bs.run();
					long lrd = bs.getLastRunDate();
					if (lrd > depLrd)
						depLrd = lrd;
				}
			}
			// Do we need to run?
			if (depLrd < getLastRunDate() && ! triggered()) {
				Log.report(LOGTAG, "skipping " + getClass().getName(), Level.FINE);
				return;
			}

			// run
			doTask();

			// Done
			Bob.setLastRunDate(this);
			return;

		} catch (Throwable e) {
			// Swallow or rethrow exception depending on settings
			handleException(e);
			return;
		} finally {
			if (timeOut!=null) timeOut.cancel();
			// Adjust count
			int bc = bob.adjustBobCount(-1);
			// remove indent
			if (verbosity!=Level.OFF) {
				bob.removeOutputFile(bob.getLogFile(this)); // FIXME what if we have			
				// a nested call?
			}
			Printer.removeIndent("   ");
			// clean up
			try {
				close();
			} catch (Exception e) {
				// Swallow!
				Log.e(LOGTAG, e);
			}
			Log.report(LOGTAG, "...exiting " + toString(), Level.FINE);
			if (bc == 0) {
				bob.close();
				Log.report(LOGTAG, "----- BUILD COMPLETE -----", Level.INFO);
			}
		}
	}

	protected void report(String msg, Level level) {
		// skip if we're ignoring these
		if (verbosity!=null && verbosity.intValue() > level.intValue()) return;
		Log.report(LOGTAG, msg, level);
	}

	/**
	 * Called by run() finally -- after the task has run, or if an exception is thrown.
	 * Override to implement any clean-up operations (e.g. releasing resources).
	 * 
	 * @throws RuntimeException Any exceptions thrown here are swallowed! Why?
	 * Because this is not part of the task, and a failure here does not mean a task fail. 
	 */
	public void close() {		
	}
	

	private boolean triggered() {
		// TODO Auto-generated method stub
		return true;
	}
	
	@Override
	public String toString() {	
		return getClass().getSimpleName();
	}

}
