package com.winterwell.utils.time;

import com.winterwell.utils.Utils;

import junit.framework.TestCase;

public class StopWatchTest extends TestCase {

	public void testPause() {
		StopWatch stopwatch = new StopWatch();
		Utils.sleep(50);
		assert !stopwatch.isPaused();

		stopwatch.pause();

		assert stopwatch.isPaused();
		Utils.sleep(100);
		long t1 = stopwatch.getTime();
		Utils.sleep(100);
		assert stopwatch.isPaused();
		long t1b = stopwatch.getTime();
		assert t1b == t1;

		stopwatch.start();

		assert !stopwatch.isPaused();
		Utils.sleep(100);
		long t2 = stopwatch.getTime();
		assert Math.abs(t1 - 50) < 10 : t1;
		assert Math.abs(t2 - 150) < 10 : t2;
	}

}
