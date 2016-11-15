package winterwell.utils.io;

import java.io.IOException;
import java.io.PrintStream;

/**
 * Collect output into a String. It also passes on the output to the underlying
 * PrintStream. E.g. will capture stuff sent to sys-out, but also pass it on as
 * normal. This uses {@link System#setOut(PrintStream)} to capture System.out.
 * Use {@link #toString()} to get the output.
 * <p>
 * By default, this only collects output sent from the thread which created it!
 * <p>
 * Remember to {@link #close()} this object after use.
 * 
 * TODO multi-threaded use is dubious :( Either come up with a better design, or
 * use sparingly
 * 
 * @author daniel
 * 
 */
public class SysOutCollectorStream extends PrintStream {

	private static final ThreadLocal<SysOutCollectorStream> collectors = new ThreadLocal<SysOutCollectorStream>();

	/**
	 * Allows reporting methods to pick up recent sys-out traffic from a thread.
	 * 
	 * @return the SysOutCollectorStream started for this thread, or null if
	 *         there is no active SysOutCollectorStream running.
	 *         <p>
	 *         Warning: does not work properly with nested collectors!
	 */
	public static SysOutCollectorStream getActiveCollector() {
		return collectors.get();
	}

	private final PrintStream base;

	private final StringBuilder buffer = new StringBuilder();

	private boolean closed;

	/**
	 * Avoid memory leak if unclosed.
	 */
	int maxChars = 20000;

	private Thread thread;

	/**
	 * Create and attach to System.out
	 */
	public SysOutCollectorStream() {
		super(System.out);
		base = System.out;
		thread = Thread.currentThread();
		System.setOut(this);
		collectors.set(this);
	}

	/**
	 * Flush, then reset System.out to be as it was when this was created. It is
	 * safe to call this multiple times - only the first call has any effect.
	 */
	@Override
	public synchronized void close() {
		if (closed)
			return;
		flush();
		System.setOut(base);
		closed = true;
		collectors.set(null);
	}

	@Override
	protected void finalize() throws Throwable {
		try {
			if (!closed) {
				close();
				System.out.println(this + " was not closed.");
			}
		} catch (Exception e) {
			// ignore
		}
		super.finalize();
	}

	@Override
	public void flush() {
		super.flush();
	}

	/**
	 * 20,000 by default
	 * 
	 * @param maxChars
	 */
	public void setMaxChars(int maxChars) {
		this.maxChars = maxChars;
	}

	/**
	 * Only collect from this thread. If null, collect from all threads.
	 */
	public void setThread(Thread thread) {
		this.thread = thread;
	}

	/**
	 * @return The collected output
	 */
	@Override
	public String toString() {
		return buffer.toString();
	}

	@Override
	public void write(byte[] b) throws IOException {
		write(b, 0, b.length);
	}

	@Override
	public void write(byte[] buf, int off, int len) {
		if (!closed) {
			// only from the right thread
			if (thread == null || Thread.currentThread() == thread) {
				if (buffer.length() < maxChars) {
					buffer.append(new String(buf, off, len));
				} else {
					// Too big! Close it down.
					buffer.append("\n...");
					close();
				}
			}
		} else {
			// bummer! write through
			// can happen easily enough if one SysOutCollectorStream wraps
			// another
		}
		super.write(buf, off, len);
	}

	@Override
	public void write(int c) {
		assert !closed;
		if (thread == null || Thread.currentThread() == thread) {
			if (buffer.length() < maxChars) {
				buffer.append((char) c);
			} else {
				// Too big! Close it down.
				buffer.append("\n...");
				close();
			}
		}
		super.write(c);
	}
}
