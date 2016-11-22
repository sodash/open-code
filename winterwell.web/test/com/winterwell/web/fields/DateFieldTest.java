package com.winterwell.web.fields;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.junit.Test;

import com.winterwell.utils.Utils;
import com.winterwell.utils.time.Dt;
import com.winterwell.utils.time.TUnit;
import com.winterwell.utils.time.Time;
import com.winterwell.utils.time.TimeUtils;
import com.winterwell.utils.web.WebUtils;

public class DateFieldTest {


	@Test
	public void testStdUK() throws ParseException {
		DateField df = new DateField("test");
		Time s1 = df.fromString("01/06/2016");
		Time s2 = df.fromString("1/6/16");
		SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yy");
		Date s0 = sdf.parse("1/6/16");
//		System.out.println(s0);
		assert s1.equals(new Time(2016,6,1));
		assert s2.equals(new Time(2016,6,1)) : s2;
	}
	@Test
	public void testBug14430() {
		String start = WebUtils.urlDecode("Thu%20Nov%2020%202014%2000%3A00%3A00%20GMT-0600%20%28Central%20America%20Standard%20Time%29");
		String end = WebUtils.urlDecode("Thu%20Nov%2027%202014%2012%3A00%3A00%20GMT-0600%20%28Central%20America%20Standard%20Time%29");
		System.out.println(start);
		System.out.println(end);
		DateField df = new DateField("test");
		Time s = df.fromString(start);
		Time e = df.fromString(end);
		System.out.println(start+" = "+s);
		System.out.println(end+" = "+e);
		assert e.isBefore(new Time());
	}
	
	
	@Test
	public void testRelativeDate() {
		String date = "1 week ago";
		DateField df = new DateField("test");
		Time x = df.fromString(date);
		System.out.println(x);
		Time weekAgo = new Time().minus(TUnit.WEEK);
		assert Math.abs(x.diff(weekAgo)) < TUnit.DAY.getMillisecs(); 
	}

	
	@Test
	public void testMiscDateFromWebParse() {
		String date = "Wed, 04 Sep 2013 17:17:06GMT";
		DateField df = new DateField("test");
		Time x = df.fromString(date);
		System.out.println(x);
	}
	
	@Test
	public void testBadDates() throws Exception {
		DateField df = new DateField("test");
		{
			Time t = df.fromString("20 November, 2011");
			assert t.diff(new Time(2011, 11, 20)) <= TUnit.DAY.millisecs : t;
		}
	}
	

	@Test
	public void testBackAndForth() throws Exception {
		DateField df = new DateField("test");
		{
			Time t1 = new Time();
			String s1 = df.toString(t1);
			Time t2 = df.fromString(s1);
			String s2 = df.toString(t2);
			Time t3 = df.fromString(s2);
			
			assert t3.equals(t2);
			assert Math.abs( t2.diff(t1) ) <= 1000;
		}
		for(int i=0; i<100; i++){
			Time t1 = new Time(Utils.getRandom().nextLong());
			String s1 = df.toString(t1);
			Time t2 = df.fromString(s1);
			String s2 = df.toString(t2);
			Time t3 = df.fromString(s2);
			
			assert t3.equals(t2);
			assert Math.abs( t2.diff(t1) ) <= 1000;
		}
	}

	
	@Test
	public void testBC() throws Exception {
		DateField df = new DateField("test");
		{
			String a = df.toString(TimeUtils.ANCIENT);
			System.out.println(a);
			Time old = df.fromString(a);
			assert old.equals(TimeUtils.ANCIENT) : a + " = " + old;
		}
	}

	@Test
	public void testParsingTimestamp() throws Exception {
		Time t = new Time(13710936090001L);
		System.out.println(t);
		Time t2 = new Time(1371093609000L);
		System.out.println(t2);
		
		DateField df = new DateField("end");
		String url = "https://copa.soda.sh/stream?end=1371093609001";
		Time v = df.getValue(url);
		System.out.println(v);
		assert v.isBefore(new Time());
	}

	@Test
	public void testConvertString2() throws Exception {
		DateField df = new DateField("test");
		{ // occasionally buggy?? misinterpreting month/day us/uk
			Time sep2011 = df.fromString("27/09/2011 13:57 +0100");
			assert sep2011.getYear() == 2011 : sep2011;
		}
		{ // 27th feb v0
			Time feb27 = df.fromString("27/02/2010 +0100");
			assert feb27.getDayOfMonth() == 27 : feb27;
			assert feb27.getYear() == 2010 : feb27;
			assert feb27.getCalendar().get(Calendar.MONTH) == 1 : feb27;
			String s = df.toString(feb27);
			Time f2 = df.fromString(s);
			assert f2.diff(feb27, TUnit.HOUR).getValue() < 8;
		}
		{ // 27th feb v0b
			Time feb27 = df.fromString("27/02/2010 0100");
			assert feb27.getDayOfMonth() == 27 : feb27;
			assert feb27.getYear() == 2010 : feb27;
			assert feb27.getCalendar().get(Calendar.MONTH) == 1 : feb27;
			String s = df.toString(feb27);
			Time f2 = df.fromString(s);
			assert f2.diff(feb27, TUnit.HOUR).getValue() < 8;
		}
		{ // 27th feb v1
			Time feb27 = df.fromString("27/02/2010");
			assert feb27.getDayOfMonth() == 27 : feb27;
			assert feb27.getYear() == 2010 : feb27;
			assert feb27.getCalendar().get(Calendar.MONTH) == 1 : feb27;
			String s = df.toString(feb27);
			Time f2 = df.fromString(s);
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
		DateField df = new DateField("test");
		{ // 27th feb v2
			Time feb27 = df.fromString("27th February");
			assert feb27.getDayOfMonth() == 27 : feb27;
			assert feb27.getYear() == 2010 : feb27;
			assert feb27.getCalendar().get(Calendar.MONTH) == 1 : feb27;
			String s = df.toString(feb27);
			Time f2 = df.fromString(s);
			assert f2.diff(feb27, TUnit.HOUR).getValue() < 8;
		}
		{ // 27th feb v3
			Time feb27 = df.fromString("27th Feb 2009");
			assert feb27.getDayOfMonth() == 27 : feb27;
			assert feb27.getYear() == 2009 : feb27;
			assert feb27.getCalendar().get(Calendar.MONTH) == 1 : feb27;
			String s = df.toString(feb27);
			Time f2 = df.fromString(s);
			assert f2.diff(feb27, TUnit.HOUR).getValue() < 8;
		}
	}
	
	@Test
	public void testConvertYahooString() {
		DateField df = new DateField("test");
		String yahoodate = "2012/3/25";
		Time mar25 = df.fromString(yahoodate);
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
		DateField df = new DateField("test");
		Time now = new Time();
		{ // check inverse
			String s = df.toString(now);
			Time n2 = df.fromString(s);
			assert n2.diff(now, TUnit.MINUTE).getValue() < 2;
		}
		{ // ago
			Time ago = df.fromString("2 days ago");
			assert ago.diff(now.minus(new Dt(2, TUnit.DAY)), TUnit.HOUR)
					.getValue() < 2;
		}
		{ // last
			Time ago = df.fromString("last week");
			assert ago.diff(now.minus(new Dt(1, TUnit.WEEK)), TUnit.HOUR)
					.getValue() < 2;
			ago = df.fromString("last month");
			assert ago.diff(now.minus(new Dt(1, TUnit.MONTH)), TUnit.HOUR)
					.getValue() < 2;
			ago = df.fromString("last year");
			assert ago.diff(now.minus(new Dt(1, TUnit.YEAR)), TUnit.DAY)
					.getValue() < 2;
		}
		{ // UK
			Time feb1st = df.fromString("01/02/2010");
			assert feb1st.getDayOfMonth() == 1 : feb1st;
			assert feb1st.getYear() == 2010 : feb1st;
			assert feb1st.getCalendar().get(Calendar.MONTH) == 1 : feb1st;
			String s = df.toString(feb1st);
			Time f2 = df.fromString(s);
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
		DateField df = new DateField("test");
		Time time = df.fromString("2012-02-02 07:04:12");
	}

}
