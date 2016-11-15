package com.winterwell.utils.time;

import com.winterwell.utils.time.StopWatchTest;

import winterwell.utils.StrUtils;
import winterwell.utils.time.Dt;
import winterwell.utils.time.TUnit;

/**
 * Simple timer utility. Can be paused and restarted.
 * 
 * @author daniel
 * @testedby {@link StopWatchTest}
 */
public final class StopWatch {

	/**
	 * Amount of time which has been used up by pauses.
	 */
	private long paused;
	/**
	 * Start time of the current pause, or 0 if not paused
	 */
	private long pauseStart;
	private long start = System.currentTimeMillis();

	/**
	 * Create and start a StopWatch
	 */
	public StopWatch() {
		//
	}

	/**
	 * @return The time in milliseconds since this stopwatch was started, minus
	 *         any pauses.
	 */
	public long getTime() {
		if (isPaused())
			return pauseStart - start - paused;
		return System.currentTimeMillis() - start - paused;
	}

	public boolean isPaused() {
		return pauseStart != 0;
	}

	/**
	 * Pause the stopwatch. Does nothing if already paused.
	 * 
	 * @return this for convenience when doing
	 *         <code>new StopWatch().pause()</code>
	 */
	public StopWatch pause() {
		if (isPaused())
			return this;
		pauseStart = System.currentTimeMillis();
		return this;
	}
	
	/**
	 * Identical to {@link #pause()}! Provided to meet expectations.
	 */
	public void stop() {
		pause();
	}

	public void print() {
		System.out.println(toString());
	}

	public void print(String pretext) {
		System.out.println(pretext + toString());
	}

	/**
	 * Restart the stopwatch after a pause. Does nothing if the stopwatch is not
	 * paused. Note that stopwatches are started automatically when constructed.
	 * Does not reset the timer (just create a new one to do that).
	 */
	public void start() {
		// adjust pause time
		if (!isPaused())
			return;
		paused += (System.currentTimeMillis() - pauseStart);
		pauseStart = 0;
	}

	/**
	 * @return The time in seconds since this stopwatch was started, minus any
	 *         pauses.
	 */
	@Override
	public String toString() {
		long dt = getTime();
		// seconds, and less precision please
		return StrUtils.toNSigFigs(dt / 1000.0, 4) + " seconds";
	}

	public Dt getDt() {
		return TimeUtils.fixUnits(new Dt(getTime(), TUnit.MILLISECOND));
	}

}
