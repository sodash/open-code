package com.winterwell.utils.time;

import java.util.Arrays;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

import org.junit.Test;

import winterwell.utils.time.Dt;
import winterwell.utils.time.TUnit;
import winterwell.utils.time.Time;

import com.winterwell.utils.MathUtils;
import com.winterwell.utils.Printer;
import com.winterwell.utils.time.TimeUtils;

public class TimeTest {

	public static void main(String[] args) {
		System.out.println(new Time("1476964316000"));
	}
	
	@Test
	public void testISO8601Format() {
		String iso = new Time().toISOString();
		Time t = new Time(iso);		
	}

	/**
	 * TODO what exactly is this testing?
	 *
	 * @param offset
	 */
	private void diffTriangular(int offset) {
		Time base = new Time(1970, 1, 10, 1, 0, 0);
		Time upper = base.plus(new Dt(23 + offset, TUnit.HOUR));
		Time lower = base.plus(new Dt(-23 + offset, TUnit.HOUR));
		assert base.diff(upper, TUnit.DAY).getValue() < 1 : base.diff(upper,
				TUnit.DAY);
		assert Math.abs(base.diff(lower, TUnit.DAY).getValue()) < 1 : base
				.diff(lower, TUnit.DAY);
		assert lower.diff(upper, TUnit.DAY).getValue() > 1 : lower.diff(upper,
				TUnit.DAY);
	}

	/**
	 * Confirm that setLenient(false) doesnt throw exception if calendar is bad
	 */
	@Test
	public void testBadCalendarBehaviour() {
		GregorianCalendar gc = new GregorianCalendar(2008, 2008, 2008);
		gc.getTimeInMillis(); // This caches the correct time in lenient mode
		gc.setLenient(false);
		gc.getTimeInMillis(); // Which means that this works without throwing an
								// error
		Time t = new Time(gc);
	}

	@Test
	public void testBadDate() {
		try {
			Time t = new Time(2008, 2008, 2008);
			assert false : t;
		} catch (Throwable e) {
			return;
		}
	}

	@Test
	public void testBC() {
		{ // zero
			Time t1 = new Time(0, 1, 1);
			System.out.println(t1);
			// there is no year 0, but 1 BC is the year before 1 AD
			assert t1.getYear() == -1 : t1.getYear();
		}
		{// january -100 AD = -101 BC 'cos no year zero
			Time t1 = new Time(-100, 1, 1);
			System.out.println(t1);
			assert t1.getYear() == -101 : t1.getYear();
		}
		{// january 100 AD
			Time t1 = new Time(100, 1, 1);
			System.out.println(t1);
			assert t1.getYear() == 100 : t1.getYear();
		}
	}

	@Test
	public void testCompareTo() {
		Time t0 = TimeUtils.ANCIENT;
		Time t1 = new Time(1900, 1, 1);
		Time t2 = new Time(2010, 1, 1);
		List<Time> list = Arrays.asList(t1, t2, t0);
		Collections.sort(list);
		assert list.get(0) == t0;
		assert list.get(1) == t1;
		assert list.get(2) == t2;
	}

	@Test
	public void testDaysDiff() {
		Time t1 = new Time(1970, 1, 1, 0, 1, 0);
		Time t2 = new Time(1970, 1, 1, 12, 1, 0);
		Time t3 = new Time(1970, 1, 2, 0, 1, 0);
		Time t4 = new Time(1970, 1, 1, 23, 1, 0);

		assert MathUtils.equalish(t1.diff(t3, TUnit.DAY).getValue(), 1) : t1
				.diff(t3, TUnit.DAY);
		assert MathUtils.equalish(t3.diff(t1, TUnit.DAY).getValue(), -1) : t3
				.diff(t1, TUnit.DAY);
		assert t1.diff(t2, TUnit.DAY).getValue() == 0.5 : t1
				.diff(t2, TUnit.DAY);
		assert t2.diff(t3, TUnit.DAY).getValue() == 0.5 : t2
				.diff(t3, TUnit.DAY);
		assert t1.diff(t4, TUnit.DAY).getValue() < 1;
	}

	/**
	 * Tests as documentation: diff behaves slightly differently at zero because
	 * of int rounding behaviour
	 */
	@Test
	public void testDiffAtZero() {
		diffTriangular(0);
	}

	@Test
	public void testDiffElsewhere() {
		try {
			diffTriangular(24);
			assert false;
		} catch (AssertionError e) {
			return;
		}
	}

	@Test
	public void testEquals() {
		Time t1 = new Time(2008, 12, 30);
		for (int i = 0; i < 100; i++) {
			Time t2 = new Time(2008, 12, 30);
			// assert t1.equals(t2) :
			Printer.out(i + ": " + (t1.longValue() - t2.longValue()));
		}
	}

	@Test
	public void testTime() {
		{
			Time t1 = new Time("01/03/3100");
			assert t1.isAfter(new Time());
		}
		// Time t2 = new Time("Mon 09 May");
		{ // a long-ago timestamp
			Time t = new Time("-62135769600000");
			Printer.out(t);
			assert t.isBefore(new Time());
		}
		{
			Time t2 = new Time(-9223372036832400000L);
			System.out.println(t2);
			assert t2.isBefore(TimeUtils.AD);
			assert t2.isBefore(TimeUtils.ANCIENT);
		}
		{
			Time t1 = new Time("Thu, 3 Oct 2013 15:12:31 +0100");
			assert t1.isBefore(new Time());

		}
	}



	@Test
	public void testTimeZoneInit() {
		TimeZone tz1 = TimeZone.getTimeZone("GMT");
		TimeZone tz2 = TimeZone.getTimeZone("gmt");
		String[] ids = TimeZone.getAvailableIDs();
		assert TimeUtils._GMT_TIMEZONE != null;
	}

	@Test
	public void testToString() {
		System.out.println(new Time());
		System.out.println(new Time(1311341573000L));
	}

	/**
	 * Tests that GMT is being properly used
	 */
	@Test
	public void testWierdness() {
		Time r0 = new Time(2008, 10, 18);
		Printer.out(r0);
		// midnight please
		assert r0.toString().contains("00:00:00");
		GregorianCalendar cal = r0.getCalendar();
		// Printer.out(cal);
		Printer.out(cal.getTime());
		int dom = r0.getDayOfMonth();
		int hr = r0.getHour();
		assert dom == 18 : dom;
		assert hr == 0 : hr;
	}

	@Test
	public void testGetHours() {
		Time a = new Time().plus(30, TUnit.MINUTE);
		Time b = new Time().minus(10, TUnit.HOUR);
		Dt res = b.diff(b, TUnit.HOUR);
		Double hours = (res.getValue());
		System.out.print(Math.floor(hours) + 2);
	}
}
