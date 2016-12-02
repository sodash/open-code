/**
 *
 */
package com.winterwell.utils.time;

import junit.framework.TestCase;

/**
 * @author miles
 * 
 */
public class TimeUtilsToStringTest extends TestCase {

	private String getString(int number, TUnit unit) {
		return TimeUtils.toString(new Dt(number, unit));
	}

	public void testHugelyDifferentUnits() {
		assertEquals("1 week",
				getString(7 * 24 * 60 * 60 * 1000 + 1, TUnit.MILLISECOND));
	}

	public void testMinimum() {
		String actual = TimeUtils
				.toString(new Dt(75, TUnit.MINUTE), TUnit.HOUR);
		assertEquals("1 hour", actual);
	}

	public void testMinimumLessThan() {
		String actual = TimeUtils
				.toString(new Dt(15, TUnit.MINUTE), TUnit.HOUR);
		assertEquals("less than one hour", actual);
	}

	public void testMinus() {
		String actual = getString(-75, TUnit.SECOND);
		// assertEquals("1 minute and 15 seconds", actual);
		assertEquals("-1 minute", actual);
	}

	public void testMixedUnits() {
		assertEquals("2 days", getString(36, TUnit.HOUR));
		assertEquals("42 minutes", getString(2521, TUnit.SECOND));
	}

	public void testSingleUnit() {
		assertEquals("3 days", getString(3, TUnit.DAY));
		assertEquals("2 hours", getString(2, TUnit.HOUR));
		assertEquals("1 second", getString(1, TUnit.SECOND));
	}

	public void testTime() {
		Time t = new Time().minus(new Dt(7, TUnit.HOUR));
		String actual = TimeUtils.toString(t);
		assertEquals("7 hours ago", actual);
	}

	public void testTimeMinimum() {
		Time t = new Time().minus(new Dt(65, TUnit.SECOND));
		String actual = TimeUtils.toString(t, TUnit.MINUTE);
		// Nowadays we also round up...
		// assertEquals("less than one minute ago", actual);
		assertEquals("1 minute ago", actual);
	}
}
