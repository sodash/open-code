package com.winterwell.utils.log;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

import com.winterwell.utils.Printer;

/**
 * A progress counter. Periodically outputs a message to Log.
 * 
 * Status: is this worthwhile??
 * 
 * @author Dan
 * 
 */
public final class Counter {
	private final AtomicInteger cnt = new AtomicInteger();
	private String desc = "	{0}";
	private final int period;

	public Counter(int period) {
		this.period = period;
	}

	public int get() {
		return cnt.get();
	}

	public void inc() {
		int v = cnt.incrementAndGet();
		if (v % period == 0) {
			Log.report(Printer.format(desc, v), Level.INFO);
			// Utils.sleep(1000); // This was to stop Joe's laptop overheating
		}
	}

	/**
	 * @param desc
	 *            Set the formatting string as used by {@link Printer}. The
	 *            default is "	{0}" Must include {0} to pick up the number!
	 */
	public void setDescription(String desc) {
		this.desc = desc;
	}

}
