package com.winterwell.utils.time;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

import org.junit.Test;

import com.winterwell.utils.MathUtils;
import com.winterwell.utils.Printer;

public class TimeTest {
	
	public static void main(String[] args) {
		System.out.println(new Time().minus(1, TUnit.MONTH).getTime());
	}
	
	@Test
	public void testISO8601Format() {
		Time t0 = new Time();
		String iso = t0.toISOString();
		Time t = new Time(iso);
		assert Math.abs(t.getTime() - t0.getTime()) < 1001;
	}
	
	@Test
	public void testISO8601DateOnlyFormat() {
		String iso = new Time(2018, 1, 12, 9, 30, 0).toISOStringDateOnly();
		assert iso.equals("2018-01-12");
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
	public void testISOTime() {		
		for(String s : new String[] {
				"2017-09-13",
				"T10:36:40Z",
				"2017-09-13T10:36:40Z",
				"2017-09-13T10:36:40+0000",
				"2017-09-13T10:36:40+00:00",
				"2017-09-13T10:36:40+0100",
				"2017-09-13T10:36:40+01:00",
				"2017-09-13T10:36:40+01",
				new Time().toISOString(),
				new Date().toString(),
				new Time().toString()
		}) {
			System.out.println("\n"+s);
			try {
				Time time = new Time(s);
				System.out.println("	Time:	"+time);
			} catch(Exception ex) {
				System.out.println("	Time:	"+ex);
			}
			try {
				Date time = new Date(s);
				System.out.println("	Date:	"+time);
			} catch(Exception ex) {
				System.out.println("	Date:	"+ex);
			}
			try {
				Instant odt = Instant.parse(s);
				long oes = odt.toEpochMilli();
				System.out.println("	Instant:	"+new Time(oes));
			} catch(Exception ex) {
				System.out.println("	Instant:	"+ex);
			}
			try {
				OffsetDateTime odt = OffsetDateTime.parse(s);
				long oes = odt.toEpochSecond()*1000;
				System.out.println("	OffsetDateTime:	"+new Time(oes));
			} catch(Exception ex) {
				System.out.println("	OffsetDateTime:	"+ex);
			}
			try {
				ZonedDateTime odt = ZonedDateTime.parse(s);
				long oes = odt.toEpochSecond()*1000;
				System.out.println("	ZonedDateTime:	"+new Time(oes));
			} catch(Exception ex) {
				System.out.println("	ZonedDateTime:	"+ex);
			}
			try {
				TemporalAccessor j8 = DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(s);
				Instant instant = Instant.from(j8);
				long oes = instant.toEpochMilli();
				System.out.println("	DateTimeFormatter:	"+new Time(oes));
			} catch(Exception ex) {
				System.out.println("	DateTimeFormatter:	"+ex);
			}
			try {
				TemporalAccessor j8 = DateTimeFormatter.ISO_DATE.parse(s);				
				Instant instant = Instant.from(j8);
				long oes = instant.toEpochMilli();
				System.out.println("	DateTimeFormatter ISO Date:	"+new Time(oes));
			} catch(Exception ex) {
				System.out.println("	DateTimeFormatter ISO Date:	"+ex);
			}
			try {
				TemporalAccessor j8 = DateTimeFormatter.ISO_OFFSET_DATE.parse(s);
				Instant instant = Instant.from(j8);
				long oes = instant.toEpochMilli();
				System.out.println("	DateTimeFormatter ISO Offset Date:	"+new Time(oes));
			} catch(Exception ex) {
				System.out.println("	DateTimeFormatter ISO Offset Date:	"+ex);
			}
//			try {
//				SimpleDateFormat sdf = new SimpleDateFormat(Time.iso8601inferZone);
//				Date parsed = sdf.parse(s);				
//				System.out.println("SimpleDateFormat infer:	"+new Time(parsed));
//			} catch(Exception ex) {
//				System.out.println("SimpleDateFormat infer:	"+ex);
//			}
			try {
				SimpleDateFormat sdf = new SimpleDateFormat(Time.iso8601Z);
				Date parsed = sdf.parse(s);				
				System.out.println("	SimpleDateFormat Z:	"+new Time(parsed));
			} catch(Exception ex) {
				System.out.println("	SimpleDateFormat Z:	"+ex);
			}
			try {
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-ddX");
				Date parsed = sdf.parse(s);				
				System.out.println("	SimpleDateFormat X:	"+new Time(parsed));
			} catch(Exception ex) {
				System.out.println("	SimpleDateFormat X:	"+ex);
			}
			try {
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
				Date parsed = sdf.parse(s);				
				System.out.println("	SimpleDateFormat date:	"+new Time(parsed));
			} catch(Exception ex) {
				System.out.println("	SimpleDateFormat date:	"+ex);
			}
//			try {
//				Calendar p = javax.xml.bind.DatatypeConverter.parseDateTime(s);				
//				System.out.println("	jaxb:	"+new Time(p));
//			} catch(Exception ex) {
//				System.out.println("	jaxb:	"+ex);
//			}			
		}
	}
	
	@Test
	public void testTime() {				
		{
			Time now = new Time();
			String iso = now.toISOString();
			Time t1 = new Time(iso);
			assert Math.abs(t1.diff(now)) < 1000 : t1.diff(now);
		}
		{
			Time now = new Time();
			String s = now.toString();
			Time t1 = new Time(s);
			assert Math.abs(t1.diff(now)) < 1000 : t1.diff(now);
		}
		{
			Time sep = new Time("2017-09-13");
			assert sep.toISOString().startsWith("2017-09-13");
		}
		{	// a 1 hour offset
			Time t0 = new Time("2017-09-13T10:36:40+01:00");
			Time t1 = new Time("2017-09-13T10:36:40Z");
			assert Math.abs(t1.diff(t0)) == TUnit.HOUR.getMillisecs() : t1.diff(t0);
		}
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
