package com.winterwell.utils.time;

import java.io.Serializable;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.winterwell.utils.Utils;

/**
 * This measures time of day/month/year.
 * 
 * Example use case: Create 2 TimeOfDay objects to mark start/end of office hours.
 * Use them to check whether specific Times are within hours.
 * 
 * @see TimeUtils#getStartOfDay(Time)
 * @author daniel
 * @testedby {@link TimeOfDayTest}
 */
public class TimeOfDay implements Serializable {
	private static final long serialVersionUID = 1L;
	
	final int hr;
	final int min;

	TimeZone timezone;
	
	public TimeZone getTimezone() {
		return timezone;
	}

	private int sec;

	/**
	 * @deprecated Uses GMT
	 * @param hr
	 * @param min
	 */
	public TimeOfDay(int hr, int min) {
		this(hr, min, TimeUtils._GMT_TIMEZONE);
	}

	/**
	 * 
	 * @param unit
	 * @param example
	 * @param timezone Can be null (=> GMT)
	 */
	public TimeOfDay(int hr, int min, TimeZone timezone) {
		this(hr, min, 0, timezone);
	}
	
	public TimeOfDay(int hr, int min, int sec, TimeZone timezone) {
		this.hr = hr;
		this.min = min;
		this.sec = sec;
		assert hr>=0 && hr<=24 : hr; 
		assert min>=0 && min <=59 : min;
		this.timezone = timezone;
	}
	
	static Pattern TOD = Pattern.compile("(\\d{1,2})(:\\d{2})?(:\\d{2})?(Z|[+\\-][\\d:]{1,4}|am|pm)?");
			
	public TimeOfDay(String v, TimeZone timezone) {
		Matcher m = TOD.matcher(v);
		if ( ! m.matches()) {
			throw new IllegalArgumentException(v);
		}
		int _hr = Integer.parseInt(m.group(1));
		String ampm = m.group(4);
		if ("pm".equals(ampm)) {
			hr = _hr+12;
		} else {
			hr = _hr;
		}
		String mins = m.group(2);
		if ( ! Utils.isBlank(mins)) {
			min = Integer.parseInt(mins.substring(1));
		} else min = 0;
		String secs = m.group(3);
		if ( ! Utils.isBlank(secs)) {
			sec = Integer.parseInt(secs.substring(1));
		} else sec = 0;
		this.timezone = timezone;
	}

	@Override
	public String toString() {
		return "TimeOfDay["+hr+":"+min+"; timezone:"+timezone+"]";
	}
	
	/**
	 * @return ISO 8601 format, e.g. hh:mmZ, or hh:mm+01
	 * See https://en.wikipedia.org/wiki/ISO_8601#Times
	 */
	public String toISOString() {
		String tz = "";
		if (timezone!=null) {
			if (timezone.equals(TimeUtils._GMT_TIMEZONE)) {
				tz = "Z";
			} else {
				Dt off = new Dt(timezone.getOffset(System.currentTimeMillis()), TUnit.MILLISECOND).convertTo(TUnit.HOUR);
				tz = oh((int)off.getValue());
			}
		}
		return oh(hr)+":"+oh(min)+tz;
	}
	
	/**
	 * add 0 padding if needed, e.g. 6am and 5 minutes = 06:05
	 * @param hour
	 * @return
	 */
	private String oh(int hour) {
		if (hour>=10) return String.valueOf(hour);
		return "0"+hour;
	}

	public Time advance(Time time) {
		Calendar cal = time.getCalendar();
		if (timezone!=null && cal.getTimeZone()!=timezone) {
			cal.setTimeZone(timezone);
		}
		cal = advance(cal);
		return new Time(cal);
	}
	
	/**
	 * Advance calendar to this time of day.
	 * @param cal
	 */
	public Calendar advance(Calendar cal) {
		long millis = cal.getTimeInMillis();
		// Copy and set timezone NB: This is done again in compareTo, but the result isn't returned.
		if (timezone != null && timezone != cal.getTimeZone()) {
			cal = (Calendar) cal.clone();
			cal.setTimeZone(timezone);
		}
		int c = compareTo(cal);
		if ( c<=0 ) {
			assert true;
			// cal is after time-of-day -- advance to the next day
			cal.add(Calendar.DAY_OF_YEAR, 1);
		}		
		TimeZone tz = cal.getTimeZone();
		assert timezone==null || tz.getRawOffset() == timezone.getRawOffset();
		// advance to time of day
		cal.set(Calendar.HOUR_OF_DAY, hr);
		cal.set(Calendar.MINUTE, min);
		cal.set(Calendar.SECOND, sec);
		cal.set(Calendar.MILLISECOND, 0);
		long millis2 = cal.getTimeInMillis();
		assert millis2 >= millis : cal;
		return cal;
	}
	
	/**
	 * Rewind/advance to this time of day.
	 * @param time
	 */
	public Time set(Time time) {
		// Copy and set timezone
		Calendar cal = time.getCalendar();
		if (timezone != null) {
			cal = (Calendar) cal.clone();
			cal.setTimeZone(timezone);
		}
		TimeZone tz = cal.getTimeZone();
		assert timezone==null || tz.getRawOffset() == timezone.getRawOffset();
		// set to time of day
		cal.set(Calendar.HOUR_OF_DAY, hr);
		cal.set(Calendar.MINUTE, min);
		cal.set(Calendar.SECOND, sec);
		cal.set(Calendar.MILLISECOND, 0);
		return new Time(cal);
	}

	/**
	 * @param t
	 * @return true if this is after t
	 */
	public boolean isAfter(Time t) {
		int c = compareTo(t);
		return c>0;
	}

	/**
	 * 
	 * @param t
	 * @return 1 if t is before this, -1 if this is before t, 0 if equal.
	 */
	public int compareTo(Time t) {
		GregorianCalendar cal = t.getCalendar();
		if (timezone!=null) cal.setTimeZone(timezone);
		return compareTo(cal);
	}
	/**
	 * 
	 * @param cal
	 * @return 1 if t is before this, -1 if this is before t, 0 if equal.
	 */
	public int compareTo(Calendar cal) {
		// Copy and set timezone
		if (timezone != null && timezone != cal.getTimeZone()) {
			cal = (Calendar) cal.clone();
			cal.setTimeZone(timezone);
		}
		int h = cal.get(Calendar.HOUR_OF_DAY);
		if (h<hr) return 1;
		if (h>hr) return -1;
		int m = cal.get(Calendar.MINUTE);
		if (m<min) return 1;
		if (m>min) return -1;
		// down to the second
		int s = cal.get(Calendar.SECOND);
		if (s<sec) return 1;
		if (s>sec) return -1;
		return 0;
	}
	
	/**
	 * 
	 * @param later
	 * @return true if this is strictly before later.
	 */
	public boolean isBefore(Time t) {
		int c = compareTo(t);
		return c<0;
	}

	public boolean isAt(Time t) {
		int c = compareTo(t);
		return c==0;
	}

	public void setTimezone(TimeZone timezone2) {
		this.timezone = timezone2;		
	}

}
