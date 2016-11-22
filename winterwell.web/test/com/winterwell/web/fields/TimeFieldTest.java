package com.winterwell.web.fields;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.concurrent.Callable;

import org.junit.Test;

import com.winterwell.utils.time.Dt;
import com.winterwell.utils.time.TUnit;
import com.winterwell.utils.time.Time;
import com.winterwell.utils.time.TimeUtils;

public class TimeFieldTest {


	@Test
	public void testTimeOfDay() throws Exception {
		{String date = "5pm";
		TimeField df = new TimeField("test");
		Callable<Time> x = df.fromString(date);
		System.out.println(x+" "+x.call());
		}
		{
			String date = "10:30";
			TimeField df = new TimeField("test");
			Callable<Time> x = df.fromString(date);
			System.out.println(x+" "+x.call());
		}
	}

	@Test
	public void testRelativeDate() throws Exception {
		String date = "1 week ago";
		TimeField df = new TimeField("test");
		Time x = df.fromString(date).call();
		System.out.println(x);
		Time weekAgo = new Time().minus(TUnit.WEEK);
		assert Math.abs(x.diff(weekAgo)) < TUnit.DAY.getMillisecs(); 
	}

	
	@Test
	public void testStartOfMonth() {
		String date = "start month";
		DateField df = new DateField("test");
		Time x = df.fromString(date);
		System.out.println(x);
	}
	
	@Test
	public void testPlus() throws Exception {
		String date = "5+days+ago";
		TimeField df = new TimeField("test");
		Callable<Time> x = df.fromString(date);
		System.out.println(x.call());
		Dt dt = x.call().dt(new Time());
		assert dt.isShorterThan(TUnit.WEEK.dt);
		assert new Dt(2, TUnit.DAY).isShorterThan(dt);
	}

	
	@Test
	public void testBC() throws Exception {
		TimeField df = new TimeField("test");
		{
			String a = df.toString(TimeUtils.ANCIENT);
			System.out.println(a);
			Time old = df.fromString(a).call();
			assert old.equals(TimeUtils.ANCIENT) : a + " = " + old;
		}
	}

	@Test
	public void testConvertString2() throws Exception {
		TimeField df = new TimeField("test");
		{ // occasionally buggy?? misinterpreting month/day us/uk
			Time sep2011 = df.fromString("27/09/2011 13:57 +0100").call();
			assert sep2011.getYear() == 2011 : sep2011;
		}
		{ // 27th feb v0
			Time feb27 = df.fromString("27/02/2010 +0100").call();
			assert feb27.getDayOfMonth() == 27 : feb27;
			assert feb27.getYear() == 2010 : feb27;
			assert feb27.getCalendar().get(Calendar.MONTH) == 1 : feb27;
			String s = df.toString(feb27);
			Time f2 = df.fromString(s).call();
			assert f2.diff(feb27, TUnit.HOUR).getValue() < 8;
		}
		{ // 27th feb v0b
			Time feb27 = df.fromString("27/02/2010 0100").call();
			assert feb27.getDayOfMonth() == 27 : feb27;
			assert feb27.getYear() == 2010 : feb27;
			assert feb27.getCalendar().get(Calendar.MONTH) == 1 : feb27;
			String s = df.toString(feb27);
			Time f2 = df.fromString(s).call();
			assert f2.diff(feb27, TUnit.HOUR).getValue() < 8;
		}
		{ // 27th feb v1
			Time feb27 = df.fromString("27/02/2010").call();
			assert feb27.getDayOfMonth() == 27 : feb27;
			assert feb27.getYear() == 2010 : feb27;
			assert feb27.getCalendar().get(Calendar.MONTH) == 1 : feb27;
			String s = df.toString(feb27);
			Time f2 = df.fromString(s).call();
			assert f2.diff(feb27, TUnit.HOUR).getValue() < 8;
		}
	}
	
	/**
	 * More experimental expressions
	 * 
	 * @throws Exception
	 */
//	@Test FIXME nice but not essential
	public void testConvertString3() throws Exception {
		TimeField df = new TimeField("test");
		{ // 27th feb v2
			Time feb27 = df.fromString("27th February").call();
			assert feb27.getDayOfMonth() == 27 : feb27;
			assert feb27.getYear() == 2010 : feb27;
			assert feb27.getCalendar().get(Calendar.MONTH) == 1 : feb27;
			String s = df.toString(feb27);
			Time f2 = df.fromString(s).call();
			assert f2.diff(feb27, TUnit.HOUR).getValue() < 8;
		}
		{ // 27th feb v3
			Time feb27 = df.fromString("27th Feb 2009").call();
			assert feb27.getDayOfMonth() == 27 : feb27;
			assert feb27.getYear() == 2009 : feb27;
			assert feb27.getCalendar().get(Calendar.MONTH) == 1 : feb27;
			String s = df.toString(feb27);
			Time f2 = df.fromString(s).call();
			assert f2.diff(feb27, TUnit.HOUR).getValue() < 8;
		}
	}
	
	@Test
	public void testConvertYahooString() throws Exception {
		TimeField df = new TimeField("test");
		String yahoodate = "2012/3/25";
		Time mar25 = df.fromString(yahoodate).call();
		assert mar25.getYear() == 2012 : mar25;
		assert mar25.getMonth() == 03 : mar25;
		assert mar25.getDayOfMonth() == 25 : mar25;
	}
	
	@Test(expected=ParseException.class)
	public void testWrongPatterns1() throws ParseException {
		DateFormat df = new SimpleDateFormat("yyyy/MM/dd");
		String date = "28/10/2012";
		Time oct28 = new Time(df.parse(date));
		
		// Wrong parsing
		assert oct28.getYear() != 2012 : oct28;
		assert oct28.getMonth() != 10 : oct28;
		assert oct28.getDayOfMonth() != 28 : oct28;
		
		// throw exception
		df.setLenient(false);
		df.parse(date);
	}
	
	@Test(expected=ParseException.class)
	public void testWrongPatterns2() throws ParseException {
		DateFormat df = new SimpleDateFormat("dd/MM/yyyy");
		String date = "2012/05/01";
		Time may01 = new Time(df.parse(date));
		
		// Wrong parsing
		assert may01.getYear() != 2012 : may01;
		assert may01.getMonth() != 05 : may01;
		assert may01.getDayOfMonth() != 01 : may01;
		
		df.setLenient(false);
		df.parse(date);
	}
	
	@Test
	public void testConvertStringString() throws Exception {
		TimeField df = new TimeField("test");
		Time now = new Time();
		{ // check inverse
			String s = df.toString(now);
			Time n2 = df.fromString(s).call();
			assert n2.diff(now, TUnit.MINUTE).getValue() < 2;
		}
		{ // ago
			Time ago = df.fromString("2 days ago").call();
			assert ago.diff(now.minus(new Dt(2, TUnit.DAY)), TUnit.HOUR)
					.getValue() < 2;
		}
		{ // last
			Time ago = df.fromString("last week").call();
			assert ago.diff(now.minus(new Dt(1, TUnit.WEEK)), TUnit.HOUR)
					.getValue() < 2;
			ago = df.fromString("last month").call();
			assert ago.diff(now.minus(new Dt(1, TUnit.MONTH)), TUnit.HOUR)
					.getValue() < 2;
			ago = df.fromString("last year").call();
			assert ago.diff(now.minus(new Dt(1, TUnit.YEAR)), TUnit.DAY)
					.getValue() < 2;
		}
		{ // UK
			Time feb1st = df.fromString("01/02/2010").call();
			assert feb1st.getDayOfMonth() == 1 : feb1st;
			assert feb1st.getYear() == 2010 : feb1st;
			assert feb1st.getCalendar().get(Calendar.MONTH) == 1 : feb1st;
			String s = df.toString(feb1st);
			Time f2 = df.fromString(s).call();
			assert f2.diff(feb1st, TUnit.HOUR).getValue() < 8;
		}
	}

	/**
	 * Parse a poorly formatted date.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testParseBadRss() throws Exception {
		TimeField df = new TimeField("test");
		Time time = df.fromString("2012-02-02 07:04:12").call();
	}

}
