/**
 *
 */
package com.winterwell.utils.threads;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.winterwell.datalog.DataLog;
import com.winterwell.utils.NotUniqueException;
import com.winterwell.utils.Utils;
import com.winterwell.utils.io.FileUtils;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.time.Dt;
import com.winterwell.utils.time.StopWatch;

/**
 * Run tasks in an offline asynchronous manner. Allows some management of the
 * queue.
 * <p>
 * Intended usage: for handling relatively few relatively slow tasks.
 * <p>
 * This _almost_ implements ExecutorService -- but it doesn't for clarity.
 * ExecutorService offers lots of (basically interchangeable) ways to run a
 * task, while TaskRunner offers 2 more focused methods: {@link #submit(ATask)}
 * and {@link #trySubmit(ATask)}
 * 
 * @author daniel
 * @testedby  TaskRunnerTest}
 * @see SafeExecutor
 */
public class TaskRunner {

	/**
	 * Hint: Usually follow this with {@link #awaitTermination()}
	 */
	public void shutdown() {
		Log.i("TaskRunner." + getName(), "shutdown");
		exec.shutdown();
	}

	boolean stats;

	/** default for arbitrary tasks */
	private static TaskRunner dflt;
	/**
	 * default for high-priority tasks -- which must be fast!
	 */
	private static TaskRunner highPriority;

	/**
	 * how many tasks to keep around in done
	 */
	private static final int HISTORY = 6;

	/**
	 * Switch on (or off) Stat tracking. Uses TaskRunner_todo and TaskRunner_dt
	 * 
	 * @param stats
	 * @return this
	 */
	public TaskRunner setStats(boolean stats) {
		this.stats = stats;
		// check that we have datalog on the classpath
		if (stats)
			DataLog.get("TaskRunner_todo");
		return this;
	}

	private static final int NUM_THREADS_FOR_DEFAULT = 6;

	public static TaskRunner getDefault() {
		if (dflt == null) {
			dflt = new TaskRunner(NUM_THREADS_FOR_DEFAULT);
		}
		return dflt;
	}

	/**
	 * This is a default task-runner for fast high-priority tasks. E.g. tasks
	 * which cause the user-interface to lag. Do NOT submit slow tasks!
	 */
	public static TaskRunner getFastDefault() {
		if (highPriority == null) {
			highPriority = new TaskRunner(NUM_THREADS_FOR_DEFAULT);
		}
		return highPriority;
	}

	public static void setDefault(TaskRunner dflt) {
		TaskRunner.dflt = dflt;
	}

	/**
	 * Includes cancelled and error tasks
	 */
	private final Queue<WeakReference<ATask>> done = new ConcurrentLinkedQueue<WeakReference<ATask>>();

	private final ExecutorService exec;

	/**
	 * Includes running tasks
	 */
	private final ConcurrentLinkedQueue<ATask> todo = new ConcurrentLinkedQueue<ATask>();

	/**
	 * Create a 2-thread TaskRunner
	 */
	public TaskRunner() {
		this(2);
	}

	private String name = "?";

	/**
	 * 
	 * @param name
	 * @return this
	 */
	public TaskRunner setName(String name) {
		assert !Utils.isBlank(name) : "[" + name + "]";
		this.name = name;
		return this;
	}

	/**
	 * 
	 * @return never blank. default:"?"
	 */
	public final String getName() {
		return name;
	}

	/**
	 * A TaskRunner which uses the current thread
	 * 
	 * @param thisThread
	 *            Must be true
	 */
	public TaskRunner(boolean thisThread) {
		assert thisThread;
		exec = new ThisThreadExecutorService();
	}

	public TaskRunner(int numThreads) {
		assert numThreads > 0;
		exec = Executors.newFixedThreadPool(numThreads);
	}
	

	/**
	 * Convenience for {@link #awaitTermination(long, TimeUnit)} with a long timeout.
	 */
	public void awaitTermination() {
		try {
			exec.awaitTermination(10, TimeUnit.DAYS);
		} catch (InterruptedException e) {
			throw Utils.runtime(e);
		}
	}


	/**
	 * Clean up any references to task. Called by the task on success, failure
	 * or cancellation.
	 * 
	 * @param task
	 */
	protected synchronized void done(ATask task) {
		// Log.d(BUGHUNT_TAG, "Finished " + task);

		todo.remove(task);
		done.add(new WeakReference(task));
		if (done.size() >= HISTORY) {
			done.remove();
		}

		// save runtime stats
		if (stats) {
			Dt dt = task.getRunningTime();
			DataLog.mean(dt.getMillisecs(), "TaskRunner_dt", name, task.getClass()
					.getSimpleName());
		}
	}

	/**
	 * Most recent finished tasks. Includes cancelled tasks.
	 * <p>
	 * The done list uses weak-references, so there are no guarantees that it
	 * will hold a task for any length of time.
	 */
	public Collection<ATask> getDone() {
		// copy out the (not-null) references
		List dones = new ArrayList(done.size());
		for (WeakReference<ATask> ref : done) {
			ATask at = ref.get();
			if (at != null)
				dones.add(at);
		}
		return dones;
	}

	/**
	 * 
	 * @return number of pending *and* currently-running tasks
	 */
	public int getQueueSize() {
		return todo.size();
	}

	/**
	 * pending and currently-running tasks
	 * 
	 * @return This is a concurrent list, so it can be edited _with_ side-effects!
	 */
	public Collection<ATask> getTodo() {
		return todo;
	}

	/**
	 * Is the task running or queued. Task equality is determined by equals() in
	 * the usual way.
	 * 
	 * @param task
	 * @return true if in {@link #todo}
	 */
	public boolean hasTask(ATask task) {
		return todo.contains(task);
	}

	/**
	 * Called when a task throws an exception. Reports it to {@link Log}. Can be
	 * over-ridden.
	 * <p>
	 * Note that the {@link Future} object will also still throw an exception.
	 * 
	 * @param noisyRunnable
	 * @param e
	 */
	public void report(Object runnableOrCallable, Throwable e) {
		Log.report(e);
	}

	/**
	 * Submit a task for processing.
	 * 
	 * NB: unless equals/hashcode are overridden, an equivalent task is an
	 * identical one. TODO: Possibly introduce a trySubmit() that always
	 * succeeds
	 * 
	 * @param task
	 * @return
	 * @throws NotUniqueException
	 *             if an equivalent task is already running or queued.
	 */
	public synchronized Future submit(ATask task) throws NotUniqueException {
		if (todo.contains(task)) {
			throw new NotUniqueException(
					"An equivalent task is already running/queued");
		}
		task.setTaskRunner(this);
		todo.add(task);
		Future f = exec.submit(task);

		// Stats
		if (stats) {
			DataLog.set(todo.size(), "TaskRunner_todo", name);
		}

		return f;
	}


	/**
     * Attempts to stop all actively executing tasks, halts the
     * processing of waiting tasks, and returns a list of the tasks
     * that were awaiting execution.
     *
     * <p>This method does not wait for actively executing tasks to
     * terminate.
     *
     * <p>There are no guarantees beyond best-effort attempts to stop
     * processing actively executing tasks.  For example, typical
     * implementations will cancel via {@link Thread#interrupt}, so any
     * task that fails to respond to interrupts may never terminate.
     *
     * @return list of tasks that never commenced execution
     */
	public List<Runnable> shutdownNow() {
		return exec.shutdownNow();
	}

	File dump;

	public File getDump() {
		return dump;
	}

	public TaskRunner setDump(File dump) {
		this.dump = dump;
		assert dump == null || !dump.isDirectory() : dump;
		return this;
	}

	public synchronized int flushToDisk(Dt cleanClose) {
		assert dump != null;
		shutdown();
		try {
			exec.awaitTermination(cleanClose.getMillisecs(),
					TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			Log.w("TaskRunner." + getName(), e);
		}
		// copy into a simple list for safe serialisation -- of course, we still
		// rely on the ATasks being nice-to-serialise
		ArrayList _todo = new ArrayList(getTodo());
		Log.i("TaskRunner." + getName(), "Save " + _todo.size() + " to " + dump);
		dump.getParentFile().mkdirs();
		FileUtils.save(_todo, dump);
		return _todo.size();
	}

	public boolean isShutdown() {
		return exec.isShutdown();
	}

	public synchronized void load() {
		if (!dump.isFile()) {
			Log.i("TaskRunner." + getName(), "Nothing to load.");
			return;
		}
		Log.i("TaskRunner." + getName(), "Load from " + dump + "...");
		List<ATask> _todo = FileUtils.load(dump);
		for (ATask aTask : _todo) {
			submitIfAbsent(aTask);
		}
		Log.i("TaskRunner." + getName(), "Loaded " + _todo.size()
				+ " from dump");
	}

	/**
	 * 
	 * @param matchMe
	 * @return a pending or currently running or done task that equals() matchMe, if there is one, or null
	 */
	public ATask getTaskMatching(ATask matchMe) {
		assert matchMe != null; // Probably a bug
		// running?
		for (ATask aTask : todo) {
			if (matchMe.equals(aTask)) {
				return aTask;
			}
		}
		// done?
		for (ATask aTask : getDone()) {
			if (matchMe.equals(aTask)) {
				return aTask;
			}
		}
		return null;
	}

	/**
	 * Convenience for {@link #submit(ATask)} which doesn't throw
	 * {@link NotUniqueException}s
	 * 
	 * @param task
	 * @return true if the task was submitted, false if an equivalent task was
	 *         already in the queue.
	 */
	public boolean submitIfAbsent(ATask task) {
		try {
			submit(task);
			return true;
		} catch(NotUniqueException ex) {
			// ignore
			return false;
		}
	}

	/**
	 * Remove a task from `todo` and `done`. Does NOT cancel or modify the task itself.
	 * @param task
	 * @return true if task was removed
	 */
	public boolean forget(ATask task) {
		if (task==null) return false;
		boolean rm = todo.remove(task);
		for (WeakReference<ATask> ref : done) {
			ATask at = ref.get();
			if (task.equals(at)) {
				ref.clear();
				done.remove(ref);
				rm = true;
				break;
			}
		}
		return rm;
	}
	

}


/**
 * Run in the current thread (useful for debugging).
 * @author daniel
 */
class ThisThreadExecutorService extends AbstractExecutorService {
	
	AtomicInteger running = new AtomicInteger();

	@Override
	public boolean awaitTermination(long timeout, TimeUnit unit)
			throws InterruptedException {
		timeout = unit.toMillis(timeout);
		StopWatch sw = new StopWatch();
		while (running.get() > 0) {
			if (sw.getTime() >= timeout)
				return false;
		}
		return true;
	}

	@Override
	public void execute(Runnable command) {
		running.incrementAndGet();
		command.run();
		running.decrementAndGet();
	}

	@Override
	public boolean isShutdown() {
		return false;
	}

	@Override
	public boolean isTerminated() {
		return false;
	}

	@Override
	public void shutdown() {
	}

	@Override
	public List<Runnable> shutdownNow() {
		return Collections.emptyList();
	}
}
