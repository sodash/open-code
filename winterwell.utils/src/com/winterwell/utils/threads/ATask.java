package com.winterwell.utils.threads;

import java.io.Closeable;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;

import com.winterwell.datalog.DataLog;
import com.winterwell.utils.ReflectionUtils;
import com.winterwell.utils.StrUtils;
import com.winterwell.utils.TimeOut;
import com.winterwell.utils.Utils;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.time.Dt;
import com.winterwell.utils.time.Time;
import com.winterwell.utils.time.TimeUtils;

/**
 * Works with {@link TaskRunner} to give a more robust version of Runnable/Callable.
 * Provides {@link #cancel()}. When run, sets the running thread's name for
 * debugging info.
 * 
 * @author daniel
 * 
 * @param <V>
 *            return type
 */
public abstract class ATask<V> implements Callable<V>, IProgress, Closeable {

	/**
	 * The enum ordering follows the lifecycle, so you can use &lt; to compare
	 * states. E.g.
	 * <code>while(status.ordinal() < QStatus.STOPPING.ordinal()) {do stuff}</code>
	 */
	public static enum QStatus {
		/** Initial putting-the-task-together state. */
		NOT_SUBMITTED,
		/** Start requested. */
		WAITING,
		/** Doing It's Thang! */
		RUNNING,
		/** Stop requested but not yet processed */
		STOPPING,
		/** Stopped: properly */
		DONE,
		/** Stopped with error */
		ERROR,
		/** Stopped: user cancelled */
		CANCELLED;

		/**
		 * @return true for the finished states of DONE, ERROR and CANCELLED.<br>
		 *         false for everything earlier (including STOPPING).
		 */
		public boolean isFinished() {
			return ordinal() >= DONE.ordinal();
		}
	}

	/**
	 * Time the task stopped running. null if not run yet.
	 */
	private Time end;

	/**
	 * Time the task stopped running. null if not run yet.
	 */
	public Time getEnd() {
		return end;
	}
	
	/**
	 * If >0, we will set a TimeOut
	 */
	private long maxTime;

	private final String name;

	private V output;

	transient TaskRunner runner;

	/**
	 * Short stacktrace from where this task was created.
	 * Useful in debugging.
	 */
	private String callStack;
	
	/**
	 * Short stacktrace from where this task was created.
	 * Useful in debugging.
	 */
	public String getCallStack() {
		return callStack;
	}
	
	/**
	 * Time the task started running. null if not started yet.
	 */
	private Time start;

	private volatile QStatus status = QStatus.NOT_SUBMITTED;

	private transient Thread thread;

	public ATask() {
		this(null);
	}

	/**
	 * 
	 * @param name
	 *            This will be used as the thread name whilst running.
	 */
	public ATask(String name) {
		this.name = name;
		callStack = StrUtils.join(ReflectionUtils.getSomeStack(6), "\n");
	}

	/** Normally null. Used if maxTime is set. */
	private TimeOut timeOut;

	private Time qtime;

	private Throwable error;

	/**
	 * @return time spent in the queue. Never null (falls back to zero)
	 */
	public Dt getQueueTime() {
		if (qtime == null)
			return TimeUtils.NO_TIME_AT_ALL;
		return qtime.dt(start == null ? new Time() : start);
	}

	/**
	 * Wraps {@link #run()} with timing & thread-naming. Finally calls
	 * {@link #close()}
	 */
	@Override
	public final V call() throws Exception {
		try {
			if (status == QStatus.CANCELLED)
				throw new CancellationException();
			start = new Time();
			// save runtime stats
			if (runner != null && runner.stats) {
				Dt dt = getQueueTime();
				DataLog.mean(dt.getMillisecs(), "TaskRunner_qdt",
						runner.getName(), getClass().getSimpleName());
			}
			// set a timer running
			// - can lead to InterruptedExceptions
			if (maxTime > 0) {
				// TODO if we had an ExecutorService, we could use DoFast which can kill stuck threads.
				timeOut = new TimeOut(maxTime);
			}
			// assert runner != null;
			status = QStatus.RUNNING;
			thread = Thread.currentThread();
			// Set the thread name (but keep it short)
			// This will always be reset to Done: or Error: by the end of the
			// method call
			thread.setName(StrUtils.ellipsize(name == null ? toString() : name,
					32));
			// if (captureStdOut) {
			// sysOut = new SysOutCollectorStream();
			// }

			// run!
			output = run();
			
			status = QStatus.DONE;
			thread.setName(StrUtils.ellipsize("Done: " + thread.getName(), 32));
			return output;
		} catch (Throwable e) {
			status = QStatus.ERROR;
			setError(e);
			if (runner != null) {
				runner.report(this, e);
			}
			thread.setName(StrUtils.ellipsize("Error: " + thread.getName(), 32));
			// There's not much point throwing an exception from within an
			// executor
			// - but it's useful in debugging.
			throw Utils.runtime(e);
		} finally {
			end = new Time();
			close();
		}
	}

	/**
	 * @deprecated This is normally managed by #call()
	 * @param error
	 */
	public void setError(Throwable error) {
		this.error = error;
	}
	
	/**
	 * Only set on status=ERROR!
	 * @return error or null
	 */
	public Throwable getError() {
		return error;
	}

	private volatile boolean closedFlag;
	
	/**
	 * Stop the time-out (if set), cancel if needed, other sub-class clean-up ops, & let the
	 * TaskRunner know. Repeated calls have no effect.
	 */
	public final void close() {
		if (closedFlag) return;
		closedFlag = true;
		// status -- should already be cancelled|error|done. 
		// But this is a public method, so could be called out of turn.
		if (this.status == null || this.status.ordinal() < QStatus.DONE.ordinal()) {
			cancel();
			return;
		}
		
		if (timeOut != null) {
			timeOut.cancel();
		}
		try {
			// other clean-up ops
			close2();
		} catch (Throwable ex) {
			if (runner != null)
				runner.report(this, ex);
			else
				Log.report(ex);
		}
		TaskRunner _runner = runner;
		if (_runner != null) {
			_runner.done(this);
		}
		// drop references
		runner = null;
		thread = null;
	}

	/**
	 * Cancel this task - it will be skipped over instead of running. Should
	 * only be called on tasks which have status == WAITING (or status ==
	 * CANCELLED in which case it does nothing).
	 * <p>
	 * The effects of cancelling a running task are undefined. They depend on
	 * {@link #cancel2_running()} (a pretty aggressive stop-it by default).
	 * 
	 * @throws IllegalStateException
	 */
	public void cancel() throws IllegalStateException {
		try {			
			switch (status) {
			case CANCELLED:
				return; // no-op
			case WAITING:
				break;
			case NOT_SUBMITTED:
				break;
			case DONE:
				return; // no-op
			case RUNNING:
				// what to do?
				cancel2_running();
			}
			if (runner != null) {
				runner.done(this);
			}
		} finally {
			status = QStatus.CANCELLED;
			close();
		}
	}

	/**
	 * Forcibly stop the task, interrupting the thread if need be.
	 * 
	 * @throws IllegalStateException
	 */
	// ?? should we have a pleaseStop flag & wait politely?
	protected void cancel2_running() throws IllegalStateException {
		// the finally clause should take care of setting thread=null
		int cnt = 0;
		if (thread == null)
			return;
		thread.interrupt();
		Utils.sleep(50);
		while (thread != null) {
			Utils.sleep(200);
			try {
				// This should trigger an exception in call(), leading
				// to close() being called, which will null-out thread
				thread.interrupt();
				cnt++;
				if (cnt == 5) {
					// No joy? then be more aggressive
					thread.stop();
					break;
				}
			} catch (NullPointerException ex) {
				// ignore: it's a non-buggy race condition on clearing thread
			}
		}
	}

	/**
	 * Implement a task-specific version of equals() and hashCode() if you want
	 * to avoid duplicate tasks.
	 */
	@Override
	public boolean equals(Object obj) {
		return super.equals(obj);
	}

	/**
	 * Get intermediate results. Override to do anything - the default returns
	 * null.
	 * 
	 * @return
	 * @see #getProgress()
	 */
	public V getIntermediateOutput() {
		return null;
	}

	public String getName() {
		return name;
	}

	/**
	 * Only valid if status == DONE
	 * 
	 * @return
	 */
	public V getOutput() {
		assert status == QStatus.DONE : status;
		return output;
	}

	@Override
	public double[] getProgress() {
		return null;
	}

	/**
	 * @return time spent actually running. Never null (falls back to zero)
	 */
	public Dt getRunningTime() {
		if (start == null)
			return TimeUtils.NO_TIME_AT_ALL;
		return start.dt(end == null ? new Time() : end);
	}

	public Time getStart() {
		return start;
	}
	
	public QStatus getStatus() {
		return status;
	}

	/**
	 * Implement a task-specific version of equals() and hashCode() if you want
	 * to avoid duplicate tasks.
	 */
	@Override
	public int hashCode() {
		return super.hashCode();
	}

	/**
	 * TODO interrupt the running thread
	 * 
	 * @throws IllegalStateException
	 */
	public void interrupt() throws IllegalStateException {
		if (status == QStatus.CANCELLED)
			return; // no-op
		if (status != QStatus.RUNNING)
			throw new IllegalStateException(status + " " + this);
		// Test: what does this do? inc to the queue
		thread.interrupt();
	}

	/**
	 * Do whatever it is this task does!
	 * 
	 * @return
	 * @throws Exception
	 * @deprecated Don't call this directly! Call {@link #call()} instead.
	 */
	protected abstract V run() throws Exception;

	/**
	 * Do task-sepcific clean-up operations. Does nothing by default -- override
	 * to use.
	 */
	protected void close2() {

	}

	/**
	 * @param null for no timeout. This will use a {@link TimeOut} to interrupt
	 *        the thread.
	 */
	public void setMaxTime(Dt maxTime) {
		this.maxTime = maxTime == null ? 0 : maxTime.getMillisecs();
	}

	void setTaskRunner(TaskRunner runner) {
		assert this.runner == null : "Task " + this
				+ " already assigned to runner " + this.runner;
		this.runner = runner;
		status = QStatus.WAITING;
		qtime = new Time();
	}

	@Override
	public String toString() {
		if (name != null)
			return getClass().getName() + "[" + name + "]";
		// have a predictable name for TaskRunnerWithStats
		return getClass().getName();
	}

	/**
	 * Convenience for: this task has finished, one way or another, and isnt moving.
	 * @return true for done|error|cancelled
	 */
	public boolean isFinished() {
		return status == QStatus.DONE || status == QStatus.ERROR || status == QStatus.CANCELLED;
	}

}
