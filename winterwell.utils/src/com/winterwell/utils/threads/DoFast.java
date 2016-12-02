package com.winterwell.utils.threads;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import com.winterwell.utils.TimeOut;
import com.winterwell.utils.Utils;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.time.Dt;
import com.winterwell.utils.time.TUnit;

/**
 * Convenience class for using TimeOut to do something quickly or not at all.
 * <p>
 * It also offers a more aggressive mode, which uses a separate thread, which can be killed 
 * to ensure the operation stops. This solves the issue with IO operations that don't respond to interrupts.
 * <p>
 * On time-out / exception, {@link #fail(Throwable)} is called -- which will swallow the
 * exception by default! You can test for an exception with {@link #getError()}.
 * 
 * @author daniel
 */
public abstract class DoFast<V> implements Callable<V> {
	private final Dt limit;
	private boolean useNewThread;
	private volatile Throwable ex;
	private boolean rethrow;
	
	/**
	 * If true, this will not swallow exceptions.
	 * @param rethrow
	 */
	public void setRethrow(boolean rethrow) {
		this.rethrow = rethrow;
	}

	public DoFast(Dt limit) {
		this.limit = limit;
		assert limit != null && limit.getValue() > 0 : limit;
	}

	public DoFast(int n, TUnit unit) {
		this(new Dt(n, unit));
	}

	/**
	 * Try to do the task within the time limit.
	 * 
	 * @return
	 */
	public final V call() {
		TimeOut to = null;
		Thread thread = null;
		try {
			to = new TimeOut(limit);
			if (useNewThread) {
				// Run in a new thread -- useful for badly behaved IO
				AtomicBoolean done = new AtomicBoolean();
				AtomicReference<V> ref = new AtomicReference();
				thread = newThread(done, ref);
				thread.start();
				while ( ! done.get()) {
					Utils.sleep(1);
				}
				return ref.get();
			} else {
				// Normal: run in this thread
				return run2();
			}
		} catch (Throwable ex) {
			this.ex = ex;
			return fail(ex);
		} finally {
			if (to != null) {
				to.cancel();
				to = null;
			}
			if (thread!=null) {
				thread.interrupt();
				if (useNewThread) {
					// Really kill it! (but allow some time for a clean shutdown)
					// This is sadly needed for some IO that doesnt respond to interrupt :(
					double max = System.currentTimeMillis() + Math.min(limit.getMillisecs()*0.5, 5000);
					while (thread.isAlive() && System.currentTimeMillis()<max) {
						Utils.sleep(1);
					}
					if (thread.isAlive()) {						
						thread.stop();
					}
				}
				thread = null;
			}
			close();
		}
	}

	private Thread newThread(final AtomicBoolean done, final AtomicReference<V> ref) {
		Thread thread = new Thread(new Runnable(){
			@Override
			public void run() {
				try {
					V val = run2();
					ref.set(val);
					done.set(true);
				} catch (Exception e) {
					ex = e;
					done.set(true);
				}
			}					
		});
		return thread;
	}

	public Throwable getError() {
		return ex;
	}
	
	/**
	 * Called finally for success or fail.
	 */
	protected void close() {
	}

	/**
	 * Do your thing (quickly)
	 */
	protected abstract V run2() throws Exception;

	/**
	 * Called if any exception is thrown (which could be a timeout, or an
	 * exception from run2().
	 * 
	 * @param ex Swallowed by default!
	 * @return This will be returned in place of an actual value from run2()
	 */
	protected V fail(Throwable ex) {
		if (rethrow) {
			throw Utils.runtime(ex);
		}
		Log.report(ex);
		return null;
	}

	public void setUseNewThread(boolean b) {
		useNewThread = b;
	}
}