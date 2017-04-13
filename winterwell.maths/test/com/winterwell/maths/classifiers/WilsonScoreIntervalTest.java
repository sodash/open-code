package com.winterwell.maths.classifiers;

import org.junit.Test;

import com.winterwell.utils.containers.Range;

public class WilsonScoreIntervalTest {

	@Test
	public void testSmokeTest() {
		Range range = WilsonScoreInterval.getInterval(0.1, 4/11.0, 10);
		System.out.println(range+" "+(4.0/11));
	}

}
