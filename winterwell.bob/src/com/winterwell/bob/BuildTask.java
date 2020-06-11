package com.winterwell.bob;

import java.io.Closeable;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.junit.Test;

import com.winterwell.depot.Desc;
import com.winterwell.depot.IHasDesc;
import com.winterwell.utils.Dep;
import com.winterwell.utils.Printer;
import com.winterwell.utils.StrUtils;
import com.winterwell.utils.TimeOut;
import com.winterwell.utils.Utils;
import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.threads.ATask;
import com.winterwell.utils.threads.ATask.QStatus;
import com.winterwell.utils.threads.TaskRunner;
import com.winterwell.utils.time.Dt;
import com.winterwell.utils.time.TUnit;
import com.winterwell.utils.time.Time;
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
 * java -cp ../winterwell.bob/bob-all.jar BobBuild
 * @author daniel
 * 
 */
public abstract class BuildTask implements Closeable, IHasDesc, Runnable, IBuildTask {

	protected transient Map<String,Object> report = new ArrayMap();

	/**
	 * This will get set at the start of the BuildTask.run()
	 * It is "version stamped" by any fields at that time.
	 *  -- it should be safe against fields modified later in the run.
	 */
	private transient Desc desc;

	@Override
	public Desc getDesc() {
		if (this.desc!=null) return desc;
		Desc _desc = new Desc(getTaskName(), BuildTask.class);
		_desc.setTag("bob");
		try {
			_desc.setVersionStamp(this);
		} catch(Throwable ex) {
			Log.w(LOGTAG, "Reflection based versioning failed: "+ex+". Using unique versioning.");
			_desc.put("vuniq", Utils.getRandom().nextDouble());
		}
		// NB: dont set the desc field until its ready for thread safety
		desc = _desc;
		return desc;
	}
	
	
	protected String getTaskName() {
		return getClass().getSimpleName();
	}


	@Override
	public final int hashCode() {
		return getDesc().hashCode();
	}

	@Override
	public final boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		BuildTask other = (BuildTask) obj;
		return getDesc().equals(other.getDesc());
	}


	protected IErrorHandler errorHandler;

	private transient Level verbosity;
	
	public Level getVerbosity() {
		return verbosity;
	}
	
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
		public void handle(Throwable ex) {
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

	protected BuildTask() {
		setDepth(0);
	}

	@Override
	public List<BuildTask> getDependencies() {
		// What about build tasks from other projects - which aren't on the classpath?
		// Use GitBobProjectTask
		return new ArrayList();
	}
	
	/** 
	 * TODO
	 * @return files this task makes -- and which should be collected up if packaging binaries
	 */
	public List<File> getOutputs() {
		return new ArrayList();
	}
	

	/**
	 * Performs the actual work of the class. 
	 * Mostly you will want to call the wrapper method {@link #run()} instead.
	 * 
	 * @throws Exception
	 */
	protected abstract void doTask() throws Exception;	
	

	private void handleException(Throwable e) {
		if (getConfig().ignoreAllExceptions) {
			Log.d(LOGTAG, "Ignoring: " + e);
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
		throw new BobBuildException(this, e);		
	}

	
	protected Dt maxTime;

	protected transient String LOGTAG;

	private boolean skipDependencies;

	private boolean skipFlag;
	
	/**
	 * if true, the dependencies will NOT be run!
	 * Use-case: for speed in debug runs.
	 * @param skipDependencies
	 * @return 
	 */
	public BuildTask setSkipDependencies(boolean skipDependencies) {
		this.skipDependencies = skipDependencies;
		return this;
	}
	
	public boolean isSkipDependencies() {
		return skipDependencies;
	}
	
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
	 * Call this to run the task via junit!
	 * This will first run the dependencies. 
	 * Then check the last-run date (& possibly quit if the task has already run).
	 * If not, it will call {@link #doTask()}
	 * <p> 
	 * Exceptions are usually thrown.
	 * Though they may be ignored (see {@link BobConfig#ignoreAllExceptions})
	 * or handled by a custom error-handler (see {@link #setErrorHandler(IErrorHandler)}).
	 * 
	 * @param context
	 * @throws RuntimeException
	 *             Exceptions are wrapped with RuntimeException and rethrown,
	 *             unless Bob is set to -ignore
	 */
	@Test
	public final void runViaJUnit() throws RuntimeException {
		// run!
		run();
		
		// also close
		Bob _bob = Bob.getSingleton();
		_bob.built.add(getClass());
		_bob.close();
		
		// report
		Map success = getReport();
		if (success!=null && ! success.isEmpty()) {
			System.out.println(StrUtils.LINEEND+Printer.toString(success, StrUtils.LINEEND, ":\t"));
		}
	}
	
	/**
	 * @return true if run() was called -- but skipped
	 */
	public boolean isSkipFlag() {
		return skipFlag;
	}
	
	/**
	 * Call this to run the task within Bob. 
	 * This does the work of runViaJUnit() without a TaskRunner shutdown.
	 * It can skip repeats.
	 * 
	 * Outside of Bob, you can call {@link #doTask()} instead. 
	 */
	public final void run() {
		// fix desc if it wasn't before
		String id = getDesc().getId();

		// skip repeat/recent runs?
		// ...NB: no skip for the top level task
		int activeTasks = getConfig().depth + getDepth();
//				Bob.getSingleton().getBobCount() + ;
		if (activeTasks!=0) {
			if (skip()) {
				skipFlag = true;
				return;				
			}
		}
		
		// Add an output and error listener
		report("Running " + toString() + " at "
				+ TimeUtils.getTimeStamp() + "...", Level.FINE);		
		bob.adjustBobCount(1);
		TimeOut timeOut = null;
		try {
			if (maxTime!=null) timeOut = new TimeOut(maxTime.getMillisecs());
			// call dependencies?
			setStatus(QStatus.WAITING);
			doDependencies();

			// run			
			setStatus(QStatus.RUNNING);
			doTask();

			// Done
			setStatus(QStatus.DONE);
			reportIssues();
			Bob.setLastRunDate(this);
			return;

		} catch (Throwable e) {
			setStatus(QStatus.ERROR);
			reportIssues();
			// Swallow or rethrow exception depending on settings
			handleException(e);
			Log.w(LOGTAG, "Exit "+this+" with error "+e);
			return;
		} finally {
			if (timeOut!=null) timeOut.cancel();
			// Adjust count
			int bc = bob.adjustBobCount(-1);
			// clean up
			try {
				close();				
			} catch (Throwable e) {
				// Swallow!
				Log.w(LOGTAG, "error in closing: "+e);				
			}
			Log.d(LOGTAG, "...exiting " + toString()+" "+status);
		}
	}
	
	transient volatile QStatus status = QStatus.NOT_SUBMITTED;
	
	protected void setStatus(QStatus status) {
		if (this.status==QStatus.ERROR && status != null) {
			return; // requires explicit clear via null
		}
		this.status = status;
	}
	
	/**
	 * 
	 * @return true if this should not be run eg for repeats
	 * @see #skip(Time) which can be over-ridden
	 */
	public final boolean skip() {				
		// already done this run? This cannot be skipped by -clean
		if (Bob.isRunAlreadyThisJVM(this)) {
			Log.i(LOGTAG, "Skip repeat for dependency: "+getClass().getSimpleName()+" "+getDesc().getId());
			return true;
		}
		// -clean? rerun inspite of any previous runs
		BobConfig settings = getConfig();
		if (settings.clean) {
			return false;
		}
		Time rs = Bob.getRunStart();
		Time lastRun = Bob.getLastRunDate(this);
		if (lastRun==null) {
			return false; // first time, run it
		}
		if (settings.cleanBefore!=null && lastRun.isBeforeOrEqualTo(settings.cleanBefore)) {
			return false; // e.g. a child Bob getting the parent's clean setting
		}
		if (lastRun.isAfterOrEqualTo(rs)) {
			Log.i(LOGTAG, "Skip repeat this run dependency: "+getClass().getSimpleName()+" "+getDesc().getId());
			return true;
		}
		// smart skip?
		Boolean _skip = skipSmart();
		if (_skip!=null) {
			return _skip;
		}
		// So it has been run -- but was it recent enough?
		boolean skip = skip(lastRun);
		if (skip) {
			Log.i(LOGTAG, "Skip recent dependency: "+getClass().getSimpleName()+" "+getDesc().getId());
			return true;
		}
		// do it again
		return false;
	}

	/**
	 * Override to do something.
	 * e.g. "if the output file is missing, return false (always run), otherwise return null (and a time-based skip
	 * might be used)" 
	 * 
	 * @return null for "normal skip behaviour", true for "yes, skip this", false for "no, run this".
	 */
	protected Boolean skipSmart() {
		return null;
	}


	private Dt skipGap;

	/**
	 * @param skipGap null by default, which means "always rerun". 
	 * Set this to allow recent previous runs to count as valid.
	 */
	public BuildTask setSkipGap(Dt skipGap) {
		this.skipGap = skipGap;
		return this;
	}
	
	/**
	 * E.g. "skip if downloaded within a day"
	 * @param lastRun
	 * @return true to skip, false to run. If in doubt, return false.
	 * Note: This will be ignored if -noskip / -clean is set true.
	 * @see #setSkipGap(Dt)
	 */
	protected final boolean skip(Time lastRun) {
		if (skipGap==null) return false;
		if (lastRun==null) return false; // paranoia
		Dt gap = lastRun.dt(new Time());
		Dt hours = gap = gap.convertTo(TUnit.HOUR); // debug
		if (gap.isShorterThan(skipGap)) {
			Log.d(LOGTAG, "skip recent "+this+" - last run "+gap+" < "+skipGap);
			return true;
		}
		Log.d(LOGTAG, "Dont skip "+this+" - prev run "+lastRun+" dt: "+hours+" > "+skipGap);
		return false;
	}


	private void reportIssues() {
		if (issues==null || issues.isEmpty()) return;
		Log.w(LOGTAG, issues.size()+" issues: "
				+StrUtils.ellipsize(Printer.toString(issues, ", "), 280)
				);
	}


	/**
	 * If the task can produce a large number of repetitive ignorable issues, 
	 * then use this to group them. Only a few will be displayed (unless verbose is set)
	 */
	protected transient List<String> issues = new ArrayList();

	protected void addIssue(String msg) {
		if (getConfig().verbose) {
			Log.w(LOGTAG, msg);
			return;
		}
		issues.add(msg);
	}


	protected static BobConfig getConfig() {
		return Bob.getSingleton().getConfig();
	}


	/**
	 * Build the dependencies.
	 * This includes a check to avoid repeat building of the same dependency. 
	 * 
	 * @return
	 * @throws BobBuildException 
	 */
	private boolean doDependencies() throws BobBuildException {
		if (skipDependencies) {
			// was there a change in the dependencies? who knows
			return true;
		}
		// run
		Collection<? extends BuildTask> deps = getDependencies();
		if (deps==null) return true;			
		String a = labelTask(getDesc());	
		for (BuildTask bs : deps) {
			// TODO use getID and getName as [label=]
			String b = labelTask(bs.getDesc());
			BobLog.logDot('"'+a+"\" -> \""+b+"\"\n");			
			bs.setDepth(getDepth()+1);
			// Do it				
			try {
				bs.run();
			} catch (Throwable ex) {
				throw new BobBuildException(bs, ex);
			}
		}
		return true;
	}

	private transient int depth;

	/**
	 * @return 0 for top-level
	 */
	protected int getDepth() {		
		return depth;
	}

	/**
	 * This is Bob build-stack depth -- not a task parameter
	 * @param depth
	 * @return
	 */
	public BuildTask setDepth(int depth) {
		this.depth = depth;
		LOGTAG = Bob.LOGTAG+"."+getConfig().depth+"."+getDepth()+"."+getClass().getSimpleName();
		return this;
	}

	private String labelTask(Desc desc) {
		return desc.getName()+"."+StrUtils.hash(StrUtils.SHORT_ALGORITHM, desc.getId());
	}


	protected void report(String msg, Level level) {
		// skip if we're ignoring these
		if (verbosity!=null && verbosity.intValue() > level.intValue()) return;
		Log.report(LOGTAG, msg, level);
	}
	
	public boolean isVerbose() {
		return (verbosity!=null && verbosity.intValue() >= Level.FINEST.intValue())
				|| getConfig().verbose;
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


	/**
	 * on-success report, which is printed out at the end.
	 * This can contain useful final info, like jar files made or servers updated.
	 * @return
	 */
	public Map getReport() {
		return report;
	}

}
