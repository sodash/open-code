package com.winterwell.utils.log;

/**
 * Listener which collects reports only from a specific thread.
 * 
 * @author daniel
 * 
 */
public abstract class ThreadedListener implements ILogListener {

	private final Thread thread;

	/**
	 * @param thread
	 *            Only listen to reports from this thread. Typically use
	 *            {@link Thread#currentThread()}
	 */
	public ThreadedListener(Thread thread) {
		this.thread = thread;
	}

	@Override
	public final void listen(Report report) {
		if (Thread.currentThread() != thread)
			return;
		listen2(report);
	}

	protected abstract void listen2(Report report);

}
