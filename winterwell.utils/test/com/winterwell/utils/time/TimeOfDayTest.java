package com.winterwell.utils.time;

import java.util.Calendar;
import java.util.TimeZone;

import org.junit.Test;

public class TimeOfDayTest {



	@Test
	public void testParseString() {
		{
			TimeOfDay tod = new TimeOfDay("10pm", TimeUtils._GMT_TIMEZONE);
			assert tod.hr==22 : tod;
			assert tod.min==0;
		}
		{
			TimeOfDay tod = new TimeOfDay("10am", TimeUtils._GMT_TIMEZONE);
			assert tod.hr==10;
			assert tod.min==0;
		}
		{
			TimeOfDay tod = new TimeOfDay("10:00Z", TimeUtils._GMT_TIMEZONE);
			assert tod.hr==10;
			assert tod.min==0;
		}
		{
			TimeOfDay tod = new TimeOfDay("6:20am", TimeUtils._GMT_TIMEZONE);
			assert tod.hr==6;
			assert tod.min==20;
		}
		{
			TimeOfDay tod = new TimeOfDay("06:20", TimeUtils._GMT_TIMEZONE);
			assert tod.hr==6;
			assert tod.min==20;
		}
		{
			TimeOfDay tod = new TimeOfDay("16:20", TimeUtils._GMT_TIMEZONE);
			assert tod.hr==16;
			assert tod.min==20;
		}
	}
	
	@Test
	public void testTimezoneBug() {
		{
			TimeZone tz = TimeZone.getTimeZone("America/Panama");
			TimeOfDay tod = new TimeOfDay(5,0,tz);
			Time t1 = new Time();
			Time t2 = tod.advance(t1);
			assert t2.isAfter(t1);
		}
		{
			TimeZone tz = TimeZone.getTimeZone("EST");
			TimeOfDay tod = new TimeOfDay(5,0,tz);
			Time t1 = new Time();
			Time t2 = tod.advance(t1);
			assert t2.isAfter(t1);
		}
	}

	@Test
	public void testAdvanceTime() {
		TimeOfDay tod = new TimeOfDay(9, 0);
		Time t1 = new Time(2014,1,1,7,30,10);
		Time t2 = tod.advance(t1);
		Time t3 = tod.advance(t2);
		Time t4 = tod.advance(t3);
		assert t2.equals(new Time(2014,1,1,9,0,0)) : t2;
		assert t3.equals(new Time(2014,1,2,9,0,0)) : t3;
		assert t4.equals(new Time(2014,1,3,9,0,0)) : t4;
	}
	

	@Test
	public void testAdvanceTimeGMT() {
		TimeZone tz = TimeZone.getTimeZone("Europe/London");
		TimeOfDay tod = new TimeOfDay(9, 0, tz);
		Time t1 = new Time("6 Apr 2016 23:56:46 GMT");
		Time t2 = tod.advance(t1);
		Calendar c2 = tod.advance(t1.getCalendar());
		assert t2.isAfter(t1);
		System.out.println(t2);
		assert t2.equals(new Time(2016,4,7,8,0,0)) : t2;
	}
	
	@Test
	public void testIsAt() {
		{
			TimeOfDay tod = new TimeOfDay(7, 0, null);
			Time t1 = new Time(2014,2,2, 7,0,0);
			assert tod.isAt(t1);
		}
		{
			TimeOfDay tod = new TimeOfDay(7, 0, null);
			Time t1 = new Time(2014,2,2, 8,0,0);
			assert ! tod.isAt(t1);
		}
	}

	
	@Test
	public void testAdvanceTime_USA() {
		// Pacific Time
		TimeZone us = TimeZone.getTimeZone("GMT-0800");
		assert us.getRawOffset() != 0;
		// 10am PST = 6pm GMT
		TimeOfDay tod = new TimeOfDay(10, 0, us);
		TimeOfDay tod_UK = new TimeOfDay(10, 0, TimeUtils._GMT_TIMEZONE);
		{	// 9 GMT
			Time t1 = new Time(2014,1,1, 9,0,0);
			Time t2 = tod.advance(t1);
			Time t2_uk = tod_UK.advance(t1);
			assert t2.equals(new Time(2014,1,1, 18,0,0)) : t2;
			assert t2_uk.equals(new Time(2014,1,1, 10,0,0)) : t2_uk;
		}
		{	// 11:30 GMT
			Time t1 = new Time(2014,1,1, 11,30,10);
			Time t2 = tod.advance(t1);
			Time t2_uk = tod_UK.advance(t1);
			assert t2.equals(new Time(2014,1,1, 18,0,0)) : t2;
			assert t2_uk.equals(new Time(2014,1,2, 10,0,0)) : t2_uk;
		}
		{	// 7pm GMT
			Time t1 = new Time(2014,1,1, 19,0,0);
			Time t2 = tod.advance(t1);
			Time t2_uk = tod_UK.advance(t1);
			assert t2.equals(new Time(2014,1,2, 18,0,0)) : t2;
			assert t2_uk.equals(new Time(2014,1,2, 10,0,0)) : t2_uk;
		}
	}

	
	@Test
	public void testIsAfter() {
		TimeOfDay tod = new TimeOfDay(9, 0);
		Time t1 = new Time(2014,1,1,7,30,10);
		Time t2 = new Time(2014,1,1,9,30,10);
		assert tod.isAfter(t1);
		assert tod.isBefore(t2);
	}

	@Test
	public void testCompareToCalendar() {
		TimeOfDay tod = new TimeOfDay(9, 0);
		Time t1 = new Time(2014,1,1,7,30,10);
		Time t3 = new Time(2014,1,1,17,30,10);
		Time t2 = tod.advance(t1);
		assert tod.compareTo(t1) > 0;
		assert tod.compareTo(t3) < 0;
		assert tod.compareTo(t2) == 0;
	}

}
