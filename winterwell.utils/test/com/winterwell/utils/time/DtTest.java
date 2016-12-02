/**
 * 
 */
package com.winterwell.utils.time;

import com.winterwell.utils.MathUtils;
import com.winterwell.utils.Printer;

import junit.framework.TestCase;

/**
 * @author Joe Halliwell <joe@winterwell.com>
 * 
 */
public class DtTest extends TestCase {

	public void testScratch() {
		System.out.println(TimeUtils.toString(new Dt(18000000, TUnit.MILLISECOND)));
	}

	

	public void testToString() {
		System.out.println(new Dt(1, TUnit.HOUR));
		System.out.println(TUnit.DAY.dt);
		System.out.println(new Dt(1, TUnit.WEEK));
		System.out.println(new Dt(2, TUnit.WEEK));
	}
	
	
	public void testDivide() {
		assert MathUtils
				.equalish(60.0, TUnit.MINUTE.dt.divide(TUnit.SECOND.dt));
		assert TUnit.YEAR.dt.divide(TUnit.MONTH.dt) == 12;
		assert new Dt(1.5, TUnit.YEAR).divide(TUnit.MONTH.dt) == 18;
		assert TUnit.YEAR.dt.divide(new Dt(3, TUnit.MONTH)) == 4;
		assert TUnit.YEAR.dt.divide(TUnit.WEEK.dt) == 52;
	}

	public void testZero() {
		Printer.out(TimeUtils.NO_TIME_AT_ALL);
	}

	public void testOutOfBounds() {
		Time start = new Time(0);
		Time before = start.minus(1, TUnit.YEAR);
		assert before.isBefore(start);
	}
}
