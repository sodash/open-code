package com.winterwell.utils.time;

import java.util.Calendar;
import java.util.TimeZone;

import org.junit.Test;

import com.winterwell.utils.MathUtils;
import com.winterwell.utils.Printer;

/**
 * 
 * @author daniel
 *
 */

public class OfficeHoursTest {


	@Test
	public void testBug36684() {
		String s = "mon,tue,wed,thu,fri,sat,sun 09:00-18:00";
		TimeZone tz = null;
		OfficeHours oh = new OfficeHours(s, tz);
		Time start = new Time("27 Mar 2016 23:23:51 GMT");
		Time on = oh.nextOn(start);
		assert on.isAfter(start);
	}
	
	@Test
	public void testBug36973() {
//		at com.winterwell.utils.time.OfficeHours.nextOn(OfficeHours.java:96)
//        at com.winterwell.utils.time.OfficeHoursPeriod$OHIt.next2(OfficeHoursPeriod.java:53)
        
//		6 Apr 2016 08:00:00 GMT v 6 Apr 2016 23:56:46 GMT w SimpleOfficeHours[mon,tue,wed,thu,fri 9:00-17:00] from 
//		spec:mon,tue,wed,thu,fri 9:00-17:00 
//		tz:sun.util.calendar.ZoneInfo[id=,offset=0,dstSavings=3600000,useDaylight=true,transitions=242,lastRule=java.util.SimpleTimeZone[id=Europe/London,offset=0,dstSavings=3600000,useDaylight=true,startYear=0,startMode=2,startMonth=2,startDay=-1,startDayOfWeek=1,startTime=3600000,startTimeMode=2,endMode=2,endMonth=9,endDay=-1,endDayOfWeek=1,endTime=3600000,endTimeMode=2]]
			
		String s = "mon,tue,wed,thu,fri 9:00-17:00";
		TimeZone tz = TimeZone.getTimeZone("Europe/London");
		OfficeHours oh = new OfficeHours(s, tz);
		Time start = new Time("6 Apr 2016 23:56:46 GMT");
		Time on = oh.nextOn(start);
		assert on.isAfter(start);
	}
	
	@Test
	public void testAssertFail() {
		String s = "mon,tue,wed,thu,fri,sat,sun 00:00-23:59";
		TimeZone tz = TimeZone.getTimeZone("America/Panama");
		OfficeHours oh = new OfficeHours("mon,tue,wed,thu,fri,sat,sun 5:00-23:00", tz);
		Time off = oh.nextOff(new Time("14 May 2015 23:00:00 GMT"));
	}
	
	@Test
	public void testGreyhoundBug() {
		TimeZone tz = TimeZone.getTimeZone("America/Panama");
//		TimeZone tz = TimeZone.getTimeZone("EST");
//		TimeZone tz = TimeZone.getTimeZone("GMT");
//		OfficeHours oh = new OfficeHours("5:00-23:00", tz);
		OfficeHours oh = new OfficeHours("mon,tue,wed,thu,fri,sat,sun 5:00-23:00", tz);
		Time on = oh.nextOn(new Time());
		Time off = oh.nextOff(new Time());
		assert ! on.equals(off);
	}


	@Test
	public void testNoDays2() {
		TimeZone tz = TimeUtils._GMT_TIMEZONE;
		OfficeHours oh = new OfficeHours("5:00-23:00", tz);
		Time on = oh.nextOn(new Time());
		Time off = oh.nextOff(new Time());
		assert ! on.equals(off);
		Time fouram = new Time(2015,1,1,4,0,0);
		Time sixam = new Time(2015,1,1,6,0,0);
		assert oh.isOn(sixam);
		assert ! oh.isOn(fouram);
	}
	

	@Test
	public void testBug30235() {
		TimeZone tz = TimeUtils._GMT_TIMEZONE;
		OfficeHours oh = new OfficeHours(" mon,tue,wed,thu,fri 08:00 - 19:00", tz);
		Time on = oh.nextOn(new Time());
		Time off = oh.nextOff(new Time());
		assert ! on.equals(off);
		// Monday
		Time fouram = new Time(2015,11,16,4,0,0);
		Time tenam = new Time(2015,11,16,10,0,0);
		// Sunday
		Time tenamSun = new Time(2015,11,15,10,0,0);
		assert oh.isOn(tenam);
		assert ! oh.isOn(tenamSun);
		assert ! oh.isOn(fouram);
	}
	
	
	@Test
	public void testNoDays() {
		TimeZone tz = TimeZone.getTimeZone("America/Panama");
		OfficeHours oh = new OfficeHours("5:00-23:00", tz);
		Time on = oh.nextOn(new Time());
		Time off = oh.nextOff(new Time());
		assert ! on.equals(off);
	}
	
	
	@Test
	public void testAlwaysOn() {
		OfficeHours oh = new OfficeHours("mon,tue,wed,thu,fri,sat,sun 00:00-23:59", TimeUtils._GMT_TIMEZONE);
		assert oh.isOn(new Time());
		
		Time no = oh.nextOff(new Time());
		assert no != null;
		
		Time offMoment = new Time(2015,1,1,23,59,30);
		assert ! oh.isOn(offMoment);
		Time no2 = oh.nextOff(offMoment);
		assert no2 != null;
		
		Time a = new Time();
		Time b = new Time().plus(2, TUnit.DAY);
		
		OfficeHoursPeriod ohp = new OfficeHoursPeriod(oh, a, b);
		Dt dt = ohp.getTotalOfficeTime();
		Dt days = dt.convertTo(TUnit.DAY);
		assert MathUtils.equalish(days.getValue(), 2) : days;
	}
	
	@Test
	public void testHours() {
		{
			SimpleOfficeHours oh = new SimpleOfficeHours("11.30-18.00",null);
			System.out.println(oh);
			Printer.out(oh.onDays);
		}
		{
			SimpleOfficeHours oh = new SimpleOfficeHours("11:30-18:00",null);
			System.out.println(oh);
			Printer.out(oh.onDays);
		}
	}
	@Test
	public void testSpec() {
		{
			SimpleOfficeHours oh = new SimpleOfficeHours("mon,tues,wed,thurs,Friday 9:00-17:00",null);
			System.out.println(oh);
			Printer.out(oh.onDays);
		}
		{
			SimpleOfficeHours oh = new SimpleOfficeHours("mon,tues,wed,thur,fri 9:00-17:00",null);			
			System.out.println(oh);
			Printer.out(oh.onDays);
		} 
		// fails! -- the time bit gets split in two
		try {
			OfficeHours oh = new OfficeHours("mon 9:00 - 17:00",null);
			System.out.println(oh);
		} catch(Exception ex) {
			System.out.println("Oh well: "+ex);
		}
		// fails!
		try {
			OfficeHours oh = new OfficeHours("mon-fri 9-5",null);
			System.out.println(oh);
		} catch(Exception ex) {
			System.out.println("Oh well: "+ex);
		}
	}
	
	@Test
	public void testOfficeTime() {
		String spec = "mon,tues,weds,thurs,fri 9:00-17:00";
		OfficeHours oh = new OfficeHours(spec, TimeUtils._GMT_TIMEZONE);
		{	// one hour
			Time a = new Time(2014,1,1, 11,0,0);
			Time b = new Time(2014,1,1, 12,0,0);
			OfficeHoursPeriod ohp = new OfficeHoursPeriod(oh, a, b);
			Dt dt = ohp.getTotalOfficeTime();
			assert dt.equals(TUnit.HOUR.dt) : dt.getMillisecs()+" vs "+TUnit.HOUR.getMillisecs();
		}
		{	// out of hours
			Time a = new Time(2014,1,1, 18,0,0);
			Time b = new Time(2014,1,1, 19,0,0);
			OfficeHoursPeriod ohp = new OfficeHoursPeriod(oh, a, b);
			Dt dt = ohp.getTotalOfficeTime();
			assert dt.getMillisecs() == 0;
		}
		{	// 1 full day
			Time a = new Time(2014,1,1, 7,0,0);
			Time b = new Time(2014,1,1, 19,0,0);
			OfficeHoursPeriod ohp = new OfficeHoursPeriod(oh, a, b);
			Dt dt = ohp.getTotalOfficeTime();
			assert dt.equals(new Dt(8, TUnit.HOUR)) : dt;
		}
		{	// 1.5 days
			Time a = new Time(2014,1,1, 7,0,0);
			Time b = new Time(2014,1,2, 12,0,0);			
			OfficeHoursPeriod ohp = new OfficeHoursPeriod(oh, a, b);
			Dt dt = ohp.getTotalOfficeTime();
			assert dt.equals(new Dt(11, TUnit.HOUR)) : dt;
			System.out.println(a.format("EEE, MMM d, yy")+" to "+b+" = "+dt.convertTo(TUnit.HOUR));
		}
		{	// over the weekend
			Time a = new Time(2014,1,3, 12,0,0); // fri
			Time b = new Time(2014,1,6, 12,0,0); // mon			
			OfficeHoursPeriod ohp = new OfficeHoursPeriod(oh, a, b);
			Dt dt = ohp.getTotalOfficeTime();
			System.out.println(a.format("hh:mm EEE, MMM d, yy")+" to "+b.format("hh:mm EEE, MMM d, yy")+" = "+dt.convertTo(TUnit.HOUR));
			assert dt.equals(new Dt(8, TUnit.HOUR)) : dt;			
		}
	}
	
	
	
	@Test
	public void testFromSpec() {
		String spec = "mon,weds 8:00-13:30";
		SimpleOfficeHours oh = new SimpleOfficeHours(spec, TimeUtils._GMT_TIMEZONE);
		assert oh.dayEnd.hr == 13 : oh;
		assert oh.dayEnd.min == 30 : oh;
		assert oh.dayStart.hr == 8 : oh;
		assert oh.dayStart.min == 00 : oh;
		assert oh.onDays[Calendar.MONDAY-1];
		assert ! oh.onDays[Calendar.TUESDAY-1];
		assert oh.onDays[Calendar.WEDNESDAY-1];
		assert ! oh.onDays[Calendar.THURSDAY-1];
		assert ! oh.onDays[Calendar.FRIDAY-1];
		assert ! oh.onDays[Calendar.SATURDAY-1];
		assert ! oh.onDays[Calendar.SUNDAY-1];
	}
	

	@Test
	public void testNextOnOff_USA() {
		Time TEN_AM = new Time(2000,1,1,10,0,0);
		// US time
		String spec = "mon,weds 8:00-12:00";
		// = 2pm to 6pm GMT
		TimeZone us = TimeZone.getTimeZone("GMT-0600");
		assert us.getRawOffset() != 0;		
		OfficeHours oh = new OfficeHours(spec, us);
				
		Time on = oh.nextOn(TEN_AM);
		Time off = oh.nextOff(on);
		System.out.println(on+" to "+off);
		
		Time on2 = oh.nextOn(off.plus(TUnit.MINUTE));
		Time off2 = oh.nextOff(on2);
		System.out.println(on2+" to "+off2);
		
		assert on2.getHour() == 14;
		assert off2.getHour() == 18;
	}

	
	@Test
	public void testNextOnOff() {
		Time TEN_AM = new Time(2000,1,1,10,0,0);
		// US time
		String spec = "mon,tues,weds 8:00-12:00";
		// = 2pm to 6pm GMT
		OfficeHours oh = new OfficeHours(spec, TimeUtils._GMT_TIMEZONE);
				
		Time on = oh.nextOn(TEN_AM);
		Time off = oh.nextOff(on);
		System.out.println(on+" to "+off);
		
		Time on2 = oh.nextOn(off.plus(TUnit.MINUTE));
		Time off2 = oh.nextOff(on2);
		System.out.println(on2+" to "+off2);
		assert on2.getHour() == 8;
		assert off2.getHour() == 12;
	}
	
	@Test
	public void testWeekend() {
		Time TEN_AM = new Time(2000,1,1,10,0,0);
		String spec = "mon,tues,weds,thur,fri 8:00-16:00; sat 10:00-14:00";
		OfficeHours oh = new OfficeHours(spec, TimeUtils._GMT_TIMEZONE);
				
		Time t = TEN_AM;
		for(int i=0; i<10; i++) {
			Time on = oh.nextOn(t);
			Time off = oh.nextOff(on);
			System.out.println(on.format("EEE")+":\ton:"+on+" to off:"+off);	
			t = off.plus(TUnit.MINUTE);
		}
	}
	
	@Test
	public void testSimpleWeekend() {
		Time TEN_AM = new Time(2000,1,1,10,0,0);
		String spec = "mon,tues,weds,thur,fri 8:00-16:00";
		SimpleOfficeHours oh = new SimpleOfficeHours(spec, TimeUtils._GMT_TIMEZONE);
				
		Time t = TEN_AM;
		for(int i=0; i<10; i++) {
			Time on = oh.nextOn(t);
			Time off = oh.nextOff(on);
			System.out.println(on.format("EEE")+":\ton:"+on+" to off:"+off);	
			t = off.plus(TUnit.MINUTE);
		}
	}

}
