package com.winterwell.utils.time;

import com.winterwell.utils.time.StopWatch;

import junit.framework.TestCase;
import com.winterwell.utils.Utils;

public class StopWatchTest extends TestCase {

	public void testPause() {
		StopWatch stopwatch = new StopWatch();
		Utils.sleep(500);
		assert !stopwatch.isPaused();

		stopwatch.pause();

		assert stopwatch.isPaused();
		Utils.sleep(1000);
		long t1 = stopwatch.getTime();
		Utils.sleep(1000);
		assert stopwatch.isPaused();
		long t1b = stopwatch.getTime();
		assert t1b == t1;

		stopwatch.start();

		assert !stopwatch.isPaused();
		Utils.sleep(1000);
		long t2 = stopwatch.getTime();
		assert Math.abs(t1 - 500) < 100 : t1;
		assert Math.abs(t2 - 1500) < 100 : t2;
	}

}
