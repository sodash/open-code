package com.winterwell.utils.log;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
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
public final class Counter extends Number {
	private static final long serialVersionUID = 1L;
	private final AtomicLong cnt = new AtomicLong();
	private String desc = "... {0}";
	private final int period;

	public Counter() {
		this(1000);
	}
	
	public Counter(int period) {
		this.period = period;
	}

	public long get() {
		return cnt.get();
	}

	public long incrementAndGet() {
		long v = cnt.incrementAndGet();
		if (v % period == 0) {
			Log.i(Printer.format(desc, v));
		}
		return v;
	}
		
	public void inc() {
		incrementAndGet();
	}

	/**
	 * @param desc
	 *            Set the formatting string as used by {@link Printer}. The
	 *            default is "	{0}" Must include {0} to pick up the number!
	 */
	public void setDescription(String desc) {
		this.desc = desc;
	}

	@Override
	public int intValue() {
		return cnt.intValue();
	}

	@Override
	public long longValue() {
		return cnt.longValue();
	}

	@Override
	public float floatValue() {
		return cnt.floatValue();
	}

	@Override
	public double doubleValue() {
		return cnt.doubleValue();
	}

}
