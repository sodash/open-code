package com.winterwell.datalog;

import org.junit.Test;

import com.winterwell.utils.time.TUnit;

public class RateTest {

	@Test
	public void testIsLessThan() {
		assert new Rate(10, TUnit.DAY).isLessThan(new Rate(5, TUnit.HOUR));
		assert ! new Rate(1, TUnit.DAY).isLessThan(new Rate(5, TUnit.MONTH));
	}
	
	@Test
	public void testIsGreaterThan() {
		assert new Rate(100, TUnit.DAY).isGreaterThan(new Rate(2, TUnit.HOUR));
		assert ! new Rate(1, TUnit.DAY).isGreaterThan(new Rate(1, TUnit.HOUR));
	}

}
