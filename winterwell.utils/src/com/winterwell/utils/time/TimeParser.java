package com.winterwell.utils.time;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.winterwell.utils.MathUtils;
import com.winterwell.utils.StrUtils;
import com.winterwell.utils.Utils;

/**
 * String -> Time code.
 * 
 * TODO refactor this, with options and stuff. Maybe even support other languages.
 * 
 * @author daniel
 *
 */
public class TimeParser {

	/**
	 * Parse a string representing a time/date. Uses the
	 * {@link SimpleDateFormat} format.
	 * <p>
	 * If the pattern does *not* contain a timezone (Z), this method will
	 * enforce GMT. Otherwise Java can get cute and use summer time.
	 * <p>
	 * If calling this method a lot, you should may want to use
	 * {@link SimpleDateFormat} directly to avoid reparsing the pattern.
	 * 
	 * @param string
	 * @param format
	 *            E.g. "HH:mm:ss dd/MM/yyyy" See {@link SimpleDateFormat} for
	 *            details.
	 *            <table border=0 cellspacing=3 cellpadding=0 summary= * *
	 *            "Chart shows pattern letters, date/time component, presentation, and examples."
	 *            * * >
	 *            <tr bgcolor="#ccccff">
	 *            <th align=left>Letter <th align=left>Date or Time Component
	 *            <th align=left>Presentation <th align=left>Examples
	 *            <tr>
	 *            <td><code>G</code> <td>Era designator <td>Text <td><code>AD
	 *            </code>
	 *            <tr bgcolor="#eeeeff">
	 *            <td><code>y</code> <td>Year <td>Year <td><code>1996</code>;
	 *            <code>96</code>
	 *            <tr>
	 *            <td><code>M</code> <td>Month in year <td>Month <td><code>July
	 *            </code>; <code>Jul</code>; <code>07</code>
	 *            <tr bgcolor="#eeeeff">
	 *            <td><code>w</code> <td>Week in year <td>Number <td><code>27
	 *            </code>
	 *            <tr>
	 *            <td><code>W</code> <td>Week in month <td>Number <td><code>2
	 *            </code>
	 *            <tr bgcolor="#eeeeff">
	 *            <td><code>D</code> <td>Day in year <td>Number <td><code>189
	 *            </code>
	 *            <tr>
	 *            <td><code>d</code> <td>Day in month <td>Number <td><code>10
	 *            </code>
	 *            <tr bgcolor="#eeeeff">
	 *            <td><code>F</code> <td>Day of week in month <td>Number <td>
	 *            <code>2</code>
	 *            <tr>
	 *            <td><code>E</code> <td>Day in week <td>Text <td><code>Tuesday
	 *            </code>; <code>Tue</code>
	 *            <tr bgcolor="#eeeeff">
	 *            <td><code>a</code> <td>Am/pm marker <td>Text <td><code>PM
	 *            </code>
	 *            <tr>
	 *            <td><code>H</code> <td>Hour in day (0-23) <td>Number <td>
	 *            <code>0</code>
	 *            <tr bgcolor="#eeeeff">
	 *            <td><code>k</code> <td>Hour in day (1-24) <td>Number <td>
	 *            <code>24</code>
	 *            <tr>
	 *            <td><code>K</code> <td>Hour in am/pm (0-11) <td>Number <td>
	 *            <code>0</code>
	 *            <tr bgcolor="#eeeeff">
	 *            <td><code>h</code> <td>Hour in am/pm (1-12) <td>Number <td>
	 *            <code>12</code>
	 *            <tr>
	 *            <td><code>m</code> <td>Minute in hour <td>Number <td><code>30
	 *            </code>
	 *            <tr bgcolor="#eeeeff">
	 *            <td><code>s</code> <td>Second in minute <td>Number <td><code>
	 *            55</code>
	 *            <tr>
	 *            <td><code>S</code> <td>Millisecond <td>Number <td><code>978
	 *            </code>
	 *            <tr bgcolor="#eeeeff">
	 *            <td><code>z</code> <td>Time zone <td>General time zone <td>
	 *            <code>Pacific Standard Time</code>; <code>PST</code>; <code>
	 *            GMT-08:00</code>
	 *            <tr>
	 *            <td><code>Z</code> <td>Time zone <td>RFC 822 time zone <td>
	 *            <code>-0800</code>
	 *            </table>
	 * @return
	 * @tesedby {@link TimeUtilsTest}
	 */
	public Time parse(String string, String pattern) {
		assert !pattern.contains("h") : "h is a tricksy bastard - you probably want H in "
				+ pattern;
		try {
			SimpleDateFormat format = new SimpleDateFormat(pattern);
			TimeZone zone = TimeZone.getTimeZone("GMT");
			format.setTimeZone(zone);
			Date date = format.parse(string);
			return new Time(date);
		} catch (ParseException e) {
			throw new IllegalArgumentException(string
					+ " did not match pattern " + pattern, e);
		}
	}

	/**
	 * @param dt
	 *            e.g. 10 minutes. Ignores +/- indicators such as "ago" or
	 *            "hence".
	 * @return always positive
	 * @testedby {@link TimeUnitTest#testParseDt()}
	 */
	public Dt parseDt(String dt) throws IllegalArgumentException {
		// trim and lower case
		dt = dt.trim().toLowerCase();
		Pattern delay = Pattern
				.compile("(a|[\\d\\.]+)?\\s*\\b(year|month|week|day|hour|hr|minute|min|second|sec)s?\\b");
		String[] bits = StrUtils.find(delay, dt);
		if (bits == null)
			throw new IllegalArgumentException("Could not parse dt-spec: " + dt);
		Double val = bits[1] == null || "a".equals(bits[1]) ? 1 : Double
				.valueOf(bits[1]);
		String us = bits[2];
		TUnit unit = parseDt2_unit(us);
		return new Dt(val, unit);
	}

	private TUnit parseDt2_unit(String us) {
		// try the enum
		try {
			TUnit unit = TUnit.valueOf(us.toUpperCase());
			assert unit != null;
			return unit;
		} catch (IllegalArgumentException e) {
			// ignore
		}
		if (us.equals("min"))
			return TUnit.MINUTE;
		if (us.equals("sec"))
			return TUnit.SECOND;
		throw new IllegalArgumentException(us);
	}

	/**
	 * Experimental handling of time strings
	 * 
	 * @param s
	 * @return
	 * @testedby {@link TimeUtilsTest#testParseExperimental()}
	 */
	public Time parseExperimental(String s) throws IllegalArgumentException {
		return parseExperimental(s, null);
	}
	
	/**
	 * 
	 * @param s
	 * @param isRelative Can be null. If provided, will be set to true for relative times, such as "today" or "yesterday"
	 * @return
	 * 
	 * TODO it'd be nice to use TimeFragment here
	 */
	public Time parseExperimental(String s, AtomicBoolean isRelative) throws IllegalArgumentException {
		Period period = parsePeriod(s, isRelative);
		return period.first;
	}
	
	/**
	 * TODO break this out into a TimeParser class so we can have language & timezone support.
	 * WARNING: will return a point time (a 0-length period) for many cases
	 * @param s a period or a time. Anything really, but only English.
	 * @param isRelative
	 * @return
	 * @throws IllegalArgumentException
	 */
	public Period parsePeriod(String s, AtomicBoolean isRelative) throws IllegalArgumentException {
		if (s.contains(" to ")) {
			String[] bits = s.split(" to ");
			Time t0 = parseExperimental(bits[0], isRelative);
			Time t1 = parseExperimental(bits[1], isRelative);
			return new Period(t0, t1);
		}
		s = s.trim().toLowerCase();
		// standard?
		try {
			Time t = new Time(s);
			
			// HACK: was it a day without a time?
			if (parsePeriod2_isWholeDay(s, t)) {
				return new Period(t, TimeUtils.getEndOfDay(t));
			}
			
			return new Period(t);
		} catch (Exception e) {
			// oh well
		}

		// Use regexs to pick out markers for day, month, hour, dt
		String month = null, day = null, hour = null;
		int year = -1;
		// - build a time object based on what we find

		{ // year
			Matcher m = TimeUtils.YEAR.matcher(s);
			if (m.find()) {
				year = Integer.valueOf(m.group(1));
				// BC?
				if (m.group().contains("b")) {
					year = -year;
				}
			}
		}

		{ // month markers -- 3 letters is enough to id a month
			Pattern MONTH = Pattern
					.compile("jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec");
			Matcher m = MONTH.matcher(s);
			if (m.find()) {
				month = m.group();
			}
		}

		{ // day of week
			Pattern DAY = Pattern.compile("mon|tue|wed|thu|fri|sat|sun");
			Matcher m = DAY.matcher(s);
			if (m.find()) {
				day = m.group();
				// guard against mon = month false match
				if ("mon".equals(day) && s.contains("month")) {
					day = null;
					if (m.find()) {
						day = m.group();
					}
				}
			}
		}
		
		{ // TODO hour:minute
			Pattern HOUR = Pattern.compile("(\\d\\d):(\\d\\d)|(\\d\\d?)am|(\\d\\d?)pm");
			Matcher m = HOUR.matcher(s);
			if (m.find()) {
				String hourMin = m.group();
				String g1 = m.group(1);
				String g2 = m.group(2);
				String g3 = m.group(3);
				String g4 = m.group(4);
				hour = Utils.or(g1,g3,g4); // TODO pm!
				if (g4!=null) {
					hour = String.valueOf((int)MathUtils.toNum(g4)+12);
				}
			}
		}
		
		// put together a date
		if (month != null) {
			if (year==-1) year = new Time().getYear();
			String formatPattern = "dd MMM yyyy";
			if (day != null) formatPattern = "EEE " + formatPattern;
			DateFormat df = new SimpleDateFormat(formatPattern);
			df.setLenient(false);
			// look for a day of month
			Matcher m = Pattern.compile("\\d+").matcher(s);
			Date date = null;
			while (m.find()) {
				int dayMonth = Integer.parseInt(m.group());
				String formatted = (day==null? "" : day + " ") + dayMonth + " " + month + " " + year;
				try {
					date = df.parse(formatted);
					break;
				} catch (ParseException ex) {
					continue;
				}
			}			
			if (date != null) return new Period(new Time(date));
			if (day==null) {
				String formatted = "1 " + month + " " + year;
				try {
					date = df.parse(formatted);
				} catch (ParseException ex) {
					// oh well
				}
				Time t = new Time(date);
				return new Period(t, TimeUtils.getEndOfMonth(new Time(date).plus(TUnit.DAY)));
			}
		}

		// special strings
		if (s.equals("now")) {
			if (isRelative!=null) isRelative.set(true);
			return new Period(new Time());
		}
		if (s.equals("today")) {
			if (isRelative!=null) isRelative.set(true);
			return new Period(TimeUtils.getStartOfDay(new Time()), TimeUtils.getEndOfDay(new Time()));
		}
		if (s.equals("yesterday")) {
			if (isRelative!=null) isRelative.set(true);
			s = "1 day ago";
		}
		if (s.equals("tomorrow")) {
			if (isRelative!=null) isRelative.set(true);
			s = "1 day from now";
		}
		
		// HACK "start/end"
		Pattern p = Pattern.compile("^(start|end)?( of )?");
		Matcher m = p.matcher(s);
		String startEnd = null;
		if (m.find()) {
			startEnd = m.group(1);
			s = s.substring(m.end());
		}
		
		if (s.startsWith("last")) {
			if (isRelative!=null) isRelative.set(true);
			if (day!=null) {
				Time lastDay = new Time();
				for(int i=0; i<7; i++) {
					lastDay = lastDay.minus(TUnit.DAY);
					String lday = lastDay.format("EEE");
					if (lday.toLowerCase().startsWith(day)) {
						return new Period(TimeUtils.getStartOfDay(lastDay), TimeUtils.getEndOfDay(lastDay));
					}
				}				
				return new Period(TimeUtils.getStartOfDay(lastDay), TimeUtils.getEndOfDay(lastDay));
			}
			s = s.replace("last", "1") + " ago";
		}
		if (s.startsWith("next")) {
			s = s.replace("next", "1") + " from now";
		}
		// a step spec, e.g. 1 week ago?
		try {
			Dt dt = parseDt(s);
			if (isRelative!=null) isRelative.set(true);
			Time t;
			if (s.contains("ago")) {				
				t = new Time().minus(dt);
			} else if (s.contains("this")) {
				// HACK test for "this month"
				// no-op
				t = new Time();
			} else if (s.trim().equals("month")) {
				t = new Time();
			} else {
				t = new Time().plus(dt);
			}
			if (startEnd==null) return new Period(t);
			// TODO don't assume month -- also handle "start of last week"
			Time t2 = TimeUtils.getStartOfMonth(t);
			if ("start".equals(startEnd)) {
				return new Period(t2);
			} else {
				Time t3 = t2.plus(TUnit.MONTH).minus(TUnit.MILLISECOND);
				return new Period(t3);
			}
		} catch (Exception e) {
			// oh well
		}		
			
		// a time? e.g. "7pm", "7pm today"??
		// TODO an actual time description, like "Monday 1st, December 1968"

		Calendar cal = new Time().getCalendar();
		if (year != -1) {
			cal.set(Calendar.YEAR, year);
		}

		// parse failed
		throw new IllegalArgumentException(s);
	}

	private boolean parsePeriod2_isWholeDay(String s, Time t) {
		if ( ! t.equals(TimeUtils.getStartOfDay(t))) {
			return false;
		}
		if (MathUtils.isNumber(s)) return false;
		// HACK: do we have an hour:minute part or other time marker?
		if (s.contains(":") || s.contains("am") || s.contains("pm")) {
			return false;
		}
		// HACK: ad hoc markers
		if (s.contains("start")) return false;
		// no time - so treat as whole day
		return true;
	}


}
