package com.winterwell.utils.threads;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.winterwell.utils.TodoException;
import com.winterwell.utils.Utils;
import com.winterwell.utils.log.Log;

/**
 * An executor which will report any exceptions via {@link Log}. You can also
 * access the most recent one (if any) via {@link #getLastException()}
 * <p>
 * Executors will quietly swallow exceptions (though {@link Future}s will throw
 * exceptions). This strikes me as dangerous.
 * 
 * @author daniel
 * 
 * @param <T>
 * @testedby NoisyCallableTest
 */
public class SafeExecutor implements ExecutorService {

	/**
	 * Makes sure that exceptions get reported via {@link Log}
	 * 
	 * @author daniel
	 * 
	 */
	final class NoisyCallable<T> implements Callable<T> {

		private final Callable<T> cbase;

		public NoisyCallable(Callable<T> base) {
			this.cbase = base;
		}

		@Override
		public T call() throws Exception {
			try {
				return cbase.call();
			} catch (Exception e) {
				report(this, e);
				throw e;
			} catch (Throwable e) {
				report(this, e);
				throw Utils.runtime(e);
			}
		}

	}

	/**
	 * Makes sure that exceptions get reported via {@link Log}
	 * 
	 * @author daniel
	 * 
	 */
	final class NoisyRunnable implements Runnable {

		private final Runnable rbase;

		public NoisyRunnable(Runnable base) {
			this.rbase = base;
		}

		@Override
		public void run() throws RuntimeException {
			try {
				rbase.run();
			} catch (Throwable e) {
				report(this, e);
				throw Utils.runtime(e);
			}
		}

	}

	private final ExecutorService base;
	private volatile Throwable ex;

	/**
	 * @return the most recent exception, or null
	 * @see #clearLastException()
	 */
	public Throwable getLastException() {
		return ex;
	}

	public void clearLastException() {
		ex = null;
	}

	public SafeExecutor(ExecutorService base) {
		this.base = base;
	}

	@Override
	public boolean awaitTermination(long timeout, TimeUnit unit)
			throws InterruptedException {
		// TODO do we need a shutdown flag here, & shutdown if necc?
		assert isShutdown();
		return base.awaitTermination(timeout, unit);
	}

	@Override
	public void execute(Runnable command) {
		base.execute(new NoisyRunnable(command));
	}

	@Override
	public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks)
			throws InterruptedException {
		throw new TodoException();
	}

	@Override
	public <T> List<Future<T>> invokeAll(
			Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
			throws InterruptedException {
		throw new TodoException();
	}

	@Override
	public <T> T invokeAny(Collection<? extends Callable<T>> tasks)
			throws InterruptedException, ExecutionException {
		throw new TodoException();
	}

	@Override
	public <T> T invokeAny(Collection<? extends Callable<T>> tasks,
			long timeout, TimeUnit unit) throws InterruptedException,
			ExecutionException, TimeoutException {
		throw new TodoException();
	}

	@Override
	public boolean isShutdown() {
		return base.isShutdown();
	}

	@Override
	public boolean isTerminated() {
		return base.isTerminated();
	}

	/**
	 * Called when a task throws an exception. Reports it to {@link Log}. Can be
	 * over-ridden.
	 * <p>
	 * Note that the {@link Future} object will also still throw an exception.
	 * 
	 * @param runnableOrCallable
	 * @param e
	 *            Never null
	 */
	public void report(Object runnableOrCallable, Throwable e) {
		assert e != null;
		Log.report(e);
		ex = e;
	}

	@Override
	public void shutdown() {
		base.shutdown();
	}

	@Override
	public List<Runnable> shutdownNow() {
		return base.shutdownNow();
	}

	@Override
	public <T> Future<T> submit(Callable<T> task) {
		NoisyCallable<T> safeTask = new NoisyCallable<T>(task);
		return base.submit(safeTask);
	}

	@Override
	public Future<?> submit(Runnable task) {
		return base.submit(new NoisyRunnable(task));
	}

	@Override
	public <T> Future<T> submit(Runnable task, T result) {
		return base.submit(new NoisyRunnable(task), result);
	}

	/**
	 * Convenience: throw an exception if there is one to throw.
	 * 
	 * @throws RuntimeException
	 */
	public void throwIfException() throws RuntimeException {
		if (ex != null) {
			throw Utils.runtime(ex);
		}
	}

	/**
	 * Convenience for {@link #awaitTermination(long, TimeUnit)} with a long timeout and runtime exception
	 */
	public void awaitTermination() {
		try {
			awaitTermination(10, TimeUnit.DAYS);
		} catch (InterruptedException e) {
			throw Utils.runtime(e);
		}
	}

}
