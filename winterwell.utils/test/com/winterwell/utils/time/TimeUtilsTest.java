package com.winterwell.utils.time;

import static org.junit.Assert.assertEquals;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

import org.junit.Test;

import com.winterwell.utils.Printer;

import winterwell.utils.containers.Containers;
import winterwell.utils.time.Dt;
import winterwell.utils.time.TUnit;
import winterwell.utils.time.Time;

import com.winterwell.utils.containers.Pair;
import com.winterwell.utils.time.TimeUtils;

public class TimeUtilsTest {

	@Test
	public void testScratch() {
		SimpleDateFormat sdf = new SimpleDateFormat();
		String out = sdf.format(new Time().getDate());
	}
	
	/**
	 * Does setting the timezone alter the reported hour and day-of-week?
	 * Yes it does.
	 * @throws ParseException
	 */
	@Test
	public void testChangingTimezone() throws ParseException {
		// 2am UK 2nd Jan
		Time t = new Time(2014,1,2, 2,0,0);
		TimeZone tz = TimeZone.getTimeZone("EST");
		GregorianCalendar cal = t.getCalendar();
		
		int hr0 = cal.get(Calendar.HOUR_OF_DAY);
		int day0 = cal.get(Calendar.DAY_OF_WEEK);
		System.out.println(hr0+" "+day0);
		
		cal.setTimeZone(tz);
		
		int hr = cal.get(Calendar.HOUR_OF_DAY);
		int day = cal.get(Calendar.DAY_OF_WEEK);
		System.out.println(hr+" "+day);
		
		assert hr != hr0;
		assert day != day0;
	}
	
	
	/**
	 * See AReport's use of timezone
	 * @throws ParseException
	 */
	@Test
	public void testNamedTimezone() throws ParseException {
		{	// GMT
			TimeZone tz = TimeZone.getTimeZone("GMT");
			assert "GMT".equals(tz.getID());
		}
		{	// UK in summer
			TimeZone tz = TimeZone.getTimeZone("GB");
			System.out.println(tz.getDisplayName());
			System.out.println(tz.getID());
			assert tz.getDSTSavings() == TUnit.HOUR.getMillisecs();
			assert tz.useDaylightTime();
			
			SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy hh:mm");
			Date date = sdf.parse("01/07/2014 00:00");
			Time midnightGMT = new Time(2014,7,1,0,0,0);
			System.out.println(midnightGMT);
			int off = tz.getOffset(midnightGMT.getTime());
			System.out.println(off);
			assert off != 0;
			Time t = midnightGMT.plus(off, TUnit.MILLISECOND);
			System.out.println(t);
			assert t.getHour() == 1;
		}
	}	
	
	@Test
	public void testTimezoneToDiff() {
		{	Time t = new Time("1/1/2014 0:00 GMT+3");
			System.out.println(t);
			System.out.println(t.getHour());
			assert t.getHour() == 24 - 3;
		}
		{
			Time t = new Time("1/1/2014 0:00 EST");
			System.out.println(t);
			System.out.println(t.getHour());
			assert t.getHour() == 5;
		}
	}
	
	@Test
	public void testFirst() {
		List<Time> times = Arrays.asList(TimeUtils.ANCIENT,
				new Time(1900, 3, 3), new Time(2001, 2, 2),
				new Time(3001, 1, 1));
		Time f = TimeUtils.first(times);
		assert f.equals(TimeUtils.ANCIENT);
	}

	@Test
	public void testLast() {
		List<Time> times = Arrays.asList(TimeUtils.ANCIENT,
				new Time(1900, 3, 3), new Time(2001, 2, 2),
				new Time(3001, 1, 1));
		Time f = TimeUtils.last(times);
		assert f.equals(new Time(3001, 1, 1)) : f;
	}
	
	@Test
	public void parseExperimental_testLastSunday() {
		{
			Time f = TimeUtils.parseExperimental("last Sunday");
			assert f.format("EEE").equals("Sun") : f;
		}
		{
			Time f = TimeUtils.parseExperimental("last tuesday");
			assert f.format("EEE").equals("Tue") : f;
		}
		// TODO
		if (false) {
			Time f = TimeUtils.parseExperimental("2pm last tuesday");
			assert f.format("EEE").equals("Tue") : f;
			assert f.getHour() == 14;
		}
	}		
	

	@Test
	public void parseExperimentalStartOfLastMonth() {
		{
			Time f = TimeUtils.parseExperimental("end of last month");
			assert f.dt(new Time()).isShorterThan(TUnit.MONTH.dt);
			System.out.println("end of last month: "+f);
		}
		{
			Time f = TimeUtils.parseExperimental("start of last month");
			System.out.println("start of last month: "+f);
		}
		{
			Time f = TimeUtils.parseExperimental("start of next month");
			System.out.println("start of next month: "+f);
		}
		if (false) { // TODO
			Time f = TimeUtils.parseExperimental("end month");
			System.out.println("end this month: "+f);
			assert f.getMonth() == new Time().getMonth();
		}
	}
	
	@Test
	public void parseExperimentalStartOfThisMonth() {
		{
			Time f = TimeUtils.parseExperimental("start of this month");
			Time t = new Time();
			Time endAndABit = new Time(t.getYear(), t.getMonth(), 1);
			System.out.println("start of this month: "+f);
			assert Math.abs(f.diff(endAndABit)) < TUnit.MINUTE.getMillisecs();			
		}
		{
			Time f = TimeUtils.parseExperimental("start month");
			Time t = new Time();
			Time endAndABit = new Time(t.getYear(), t.getMonth(), 1);
			System.out.println("start month: "+f);
			assert Math.abs(f.diff(endAndABit)) < TUnit.MINUTE.getMillisecs();			
		}
		{
			Time f = TimeUtils.parseExperimental("end of this month");
			Time t = new Time();
			Time endAndABit = new Time(t.getYear(), t.getMonth() + 1, 1);
			assert Math.abs(f.diff(endAndABit)) < TUnit.MINUTE.getMillisecs();
			System.out.println("end of this month: "+f);
		}
		{
			Time f = TimeUtils.parseExperimental("end month");
			Time t = new Time();
			Time endAndABit = new Time(t.getYear(), t.getMonth() + 1, 1);
			assert Math.abs(f.diff(endAndABit)) < TUnit.MINUTE.getMillisecs();
			System.out.println("end month: "+f);
		}
	}

	@Test
	public void testEqualish() {
		Time jan1 = new Time(2011, 1, 1);
		Time jan2 = new Time(2011, 1, 2);
		assert TimeUtils.equalish(jan1, jan2, TUnit.WEEK);
		assert !TimeUtils.equalish(jan1, jan2, TUnit.HOUR);
	}

	@Test
	public void scratch() {
		System.out.println(new Time(1330442206497l));
		System.out.println(new Time(1330442161000l));
		// 2012-02-28 16:49:57.157834
	}

	@Test
	public void testAncient() {
		{
			System.out.println(TimeUtils.ANCIENT);
			Printer.out(TimeUtils.ANCIENT);
			long utc = TimeUtils.ANCIENT.getTime();

			assert utc < new Time().minus(TUnit.YEAR.dt).getTime();
			assert TimeUtils.ANCIENT.isBefore(new Time(1, 1, 1));
			assert utc < new Time(1, 1, 1).getTime();

			assert TimeUtils.ANCIENT.getCalendar().get(Calendar.ERA) == GregorianCalendar.BC : TimeUtils.ANCIENT
					.getCalendar().get(Calendar.ERA);
			assert TimeUtils.ANCIENT.getYear() < 0 : TimeUtils.ANCIENT
					.getYear();
		}
		{ // AD
			System.out.println(TimeUtils.AD);
			Printer.out(TimeUtils.AD+"\t"+TimeUtils.AD.getTime());
			long utc = TimeUtils.AD.getTime();

			assert utc < new Time().minus(TUnit.YEAR.dt).getTime();
			assert TimeUtils.AD.isBefore(new Time(2, 1, 1));
			assert utc < new Time(2, 1, 1).getTime();

			System.out.println(TimeUtils.AD.getYear());
			assert TimeUtils.AD.getYear() < 2 : TimeUtils.ANCIENT.getYear();
		}
	}

	@Test
	public void testBetween() {
		Time a = new Time(2000, 1, 1);
		Time b = new Time(2000, 1, 10);
		Time c = new Time(2000, 2, 1);
		assert TimeUtils.between(a, a, b);
		assert TimeUtils.between(b, a, b);
		assert TimeUtils.between(b, a, c);
		assert !TimeUtils.between(c, a, b);
	}

	@Test
	public void testGetDays() {
		{
			Time t = new Time();
			Time t2 = t.plus(1, TUnit.DAY);
			List<Time> days = Containers.getList(TimeUtils.getDays(t, t2));
			assert days.size() == 2 : days;
			Time a = days.get(0);
			assert a.getHour() == 0;
			assert a.getDayOfMonth() == t.getDayOfMonth();
			assert a.getYear() == t.getYear();
			assert a.getMonth() == t.getMonth();
		}
		{ // single day
			Time t = new Time();
			List<Time> days = Containers.getList(TimeUtils.getDays(t, t));
			assert days.size() == 1 : days;
			Time a = days.get(0);
			assert a.getHour() == 0;
			assert a.getDayOfMonth() == t.getDayOfMonth();
			assert a.getYear() == t.getYear();
			assert a.getMonth() == t.getMonth();
		}
	}

	@Test
	public void testGetStartOfDay() {
		Time t = new Time();
		Time start = TimeUtils.getStartOfDay(t);
		Time t2 = new Time(t.getYear(), t.getMonth(), t.getDayOfMonth());
		assert start.equals(t2) : start.diff(t2);
	}

	@Test
	public void testGetStartOfMonth() {
		{
			Time t = new Time(2000, 3, 2, 5, 12, 7);
			Time s = TimeUtils.getStartOfMonth(t);
			Time t2 = new Time(2000, 3, 1);
			assert s.equals(t2) : s + " " + s.dt(t2);
		}
		{
			Time t = new Time();
			Time start = TimeUtils.getStartOfMonth(t);
			Time t2 = new Time(t.getYear(), t.getMonth(), 1);
			assert start.equals(t2) : start + " " + start.dt(t2);
		}
		{ // then do +/-
			Time t = new Time(2000, 3, 2, 5, 12, 7);
			Time s = TimeUtils.getStartOfMonth(t);
			Time t2 = new Time(2000, 3, 1);
			assert s.equals(t2) : s + " " + s.dt(t2);
			Time prev = s.minus(TUnit.MONTH);
			Time p = new Time(2000, 2, 1);
			assert p.equals(prev) : prev + " " + prev.dt(p);
		}
	}

	@Test
	public void testInverseInSummer() {
		// parse should be the inverse of format, I think?
		String example_time = "2007-09-08 00:03:52";
		Time time = TimeUtils.parse(example_time, "yyyy-MM-dd HH:mm:ss");
		String time_str = time.format("yyyy-MM-dd HH:mm:ss");
		assertEquals(example_time, time_str);
	}

	@Test
	public void testInverseInWinter() {
		// parse should be the inverse of format, I think?
		String example_time = "2007-01-08 00:03:52";
		Time time = TimeUtils.parse(example_time, "yyyy-MM-dd HH:mm:ss");
		String time_str = time.format("yyyy-MM-dd HH:mm:ss");
		assertEquals(example_time, time_str);
	}

	@Test
	public void testParse() {
		Time time0 = TimeUtils.parse("00:00:00 GMT", "HH:mm:ss Z");
		assert time0.getHour() == 0;
		Time time1 = TimeUtils.parse("01:00:00 GMT", "HH:mm:ss Z");
		Time time10 = TimeUtils.parse("10:00:00 GMT", "HH:mm:ss Z");
		assert time10.getHour() == 10;
		Time time12 = TimeUtils.parse("12:00:00 GMT", "HH:mm:ss Z");
		assert time12.getHour() == 12;
		Time time13 = TimeUtils.parse("13:00:00 GMT", "HH:mm:ss Z");
		assert time13.getHour() == 13;
		Time time23 = TimeUtils.parse("23:00:00 GMT", "HH:mm:ss Z");
		assert time23.getHour() == 23;
		// 2009-09-18 12:00:00
		Time time = TimeUtils.parse("2009-09-18 12:00:00 GMT",
				"yyyy-MM-dd HH:mm:ss Z");
		String ts = time.toString();
		assert time.getYear() == 2009;
		assert time.getDayOfMonth() == 18 : time;
		assert time.getMonth() == 9;
	}

	@Test
	public void testParseAgainstTimeConstructor() {
		// This could equally well go in TimeTest ....
		String example_time = "2007-09-08 00:03:52";
		Time time1 = TimeUtils.parse(example_time, "yyyy-MM-dd HH:mm:ss");
		Time time2 = new Time(2007, 9, 8, 0, 3, 52);
		// get date from time1, then provide explicit time -- not too weird
		// a thing to be doing with this code ....
		Time time3 = new Time(time1.getYear(), time1.getMonth(),
				time1.getDayOfMonth(), 0, 3, 52);
		assertEquals(time1, time2);
		assertEquals(time2, time3);
	}

	@Test
	public void testParsePeriod() {
		Pair<Time> p = TimeUtils
				.parsePeriod("18/11/09 to 23/11/09", "dd/MM/yy");
		assert p.first.equals(new Time(2009, 11, 18)) : p;
		assert p.second.equals(new Time(2009, 11, 23)) : p;
	}

	@Test
	public void testToStringTime() {
		{
			Time t = new Time();
			t = t.minus(new Dt(0.5, TUnit.SECOND));
			String s = TimeUtils.toString(t, TUnit.SECOND);
			assert s.equals("now") : s;
			System.out.println(s);
		}
		{
			Time t = new Time();
			t = t.minus(new Dt(1.35, TUnit.SECOND));
			String s = TimeUtils.toString(t, TUnit.SECOND);
			assert s.equals("1 second ago") : s;
			System.out.println(s);
		}
		{
			Time t = new Time();
			t = t.minus(new Dt(10, TUnit.SECOND));
			String s = TimeUtils.toString(t);
			assert s.equals("10 seconds ago") : s;
			System.out.println(s);
		}
		{
			Time t = new Time();
			t = t.minus(new Dt(28, TUnit.HOUR));
			String s = TimeUtils.toString(t);
			assert s.equals("1 day ago") : s;
			System.out.println(s);
		}
		{
			Time t = new Time();
			t = t.minus(new Dt(22, TUnit.HOUR));
			String s = TimeUtils.toString(t);
			assert s.equals("1 day ago") : s;
			System.out.println(s);
		}
		{
			Time t = new Time();
			t = t.minus(new Dt(25, TUnit.HOUR));
			String s = TimeUtils.toString(t, TUnit.DAY);
			assert s.equals("1 day ago") : s;
			System.out.println(s);
		}
	}

	@Test
	public void testWithoutZ() {
		String example_time = "2007-09-08 00:03:52";
		Time time = TimeUtils.parse(example_time, "yyyy-MM-dd HH:mm:ss");
		assertEquals(2007, time.getYear());
		assertEquals(9, time.getMonth());
		assertEquals(8, time.getDayOfMonth());
		assertEquals(0, time.getHour());
		assertEquals(3, time.getMinutes());
		assertEquals(52, time.getSeconds());
	}
	
	@Test
	public void testWeekday() {
		{
			String date = "Thursday 7th November, 2013";
			Time time = TimeUtils.parseExperimental(date);
			Time time2 = new Time(2013, 11, 7);
			assertEquals(time2, time);
		}		 
	
		{
			String date = "7 November, 2013";
			Time time = TimeUtils.parseExperimental(date);
			Time time2 = new Time(2013, 11, 7);
			assertEquals(time2, time);
		}		 
	}
}
