package com.winterwell.utils.time;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.winterwell.utils.StrUtils;
import com.winterwell.utils.containers.Pair;

/**
 * Because Date and Calendar both have problems, but JodaTime is
 * over-engineered.
 * 
 * @author Daniel
 * @testedby {@link TimeUtilsTest}
 */
public class TimeUtils {
	/**
	 * The GMT standard timezone. 
	 * To get your own timezone, use TimeZone.getTimeZone(name);
	 */
	/*
	 * Note: this line of code must come at the start of this class, before any
	 * static Time objects are created. The leading _ ensures this happens after
	 * a sort-members clean-up.
	 */
	public static final TimeZone _GMT_TIMEZONE = TimeZone.getTimeZone("GMT");

	/**
	 * The start of AD. Expect bugs when working with earlier dates, as it's
	 * quite easy to lose the BC part (and Java will help you do so).
	 */
	public static final Time AD = new Time(1, 1, 1);
	
	/**
	 * Because ANCIENT and AD can both break Java date parsing. 1900 is a fair backstop for most stuff,
	 * and more reliable for transmission.
	 */
	public static final Time WELL_OLD = new Time(1900, 1, 1);
	
	/**
	 * 200 years from now. A lenient date to compare against for far-future.
	 */
	public static final Time WELL_FUTURE = new Time().plus(200, TUnit.YEAR);

	/**
	 * Create a Time stamp that can act as a sort of null or "zero". Uses 1
	 * million years BC. So there's the potential for issues with astronomical
	 * simulations, but otherwise should be safe.
	 */
	public static final Time ANCIENT = new Time(-1000000, 1, 1);

	public static final long DAY_IN_MILLISECS = 24 * 60 * 60 * 1000L;

	/**
	 * A Time stamp that can act as "never". Uses 1 million years AD. So there's
	 * the potential for issues with astronomical simulations, but otherwise
	 * should be safe.
	 */
	public static final Time DISTANT_FUTURE = new Time(1000000, 1, 1);

	/**
	 * A long long time from now.
	 */
	public static final Dt FOREVER = new Dt(Long.MAX_VALUE / 2,
			TUnit.MILLISECOND);

	public static final long HOUR_IN_MILLISECS = 60 * 60 * 1000L;

	public static final Dt NO_TIME_AT_ALL = new Dt(0, TUnit.MILLISECOND);

	/**
	 * 3 months. Just a convenient constant.
	 */
	public static final Dt QUARTER = new Dt(3, TUnit.MONTH);

	static final DateFormat timeStamp = new SimpleDateFormat(
			"EEE, d MMM yyyy HH:mm:ss");

	/**
	 * 1xxx, 2xxx, 3xxx or use ad/bc/ce
	 */
	static final Pattern YEAR = Pattern
			.compile("([123]\\d\\d\\d)|(\\d+) ?(a\\.?d\\.?|b\\.?c\\.?e?\\.?|c\\.?e\\.?)");

	/**
	 * Add n dts to date, e.g. 3 days to 12th May = 15th May. Creates a new date
	 * object.
	 */
	public static Date add(Date date, int n, TUnit dt) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		cal.add(dt.getCalendarField(), n);
		return cal.getTime();
	}

	/**
	 * 
	 * @param date
	 * @param start
	 * @param end
	 * @return true if date is within start and end *inclusive*
	 */
	public static boolean between(Time date, Time start, Time end) {
		assert !end.isBefore(start);
		return (date.isAfter(start) && date.isBefore(end))
				|| date.equals(start) || date.equals(end);
	}

	/**
	 * Difference between two dates in the selected unit.
	 * 
	 * @param start
	 * @param end
	 * @param unit
	 *            If this is months, the answers will be approximate
	 * @return
	 */
	public static double diff(Date start, Date end, TUnit unit) {
		long dt = end.getTime() - start.getTime();
		// not the most efficient way to do this
		return new Dt(dt, TUnit.MILLISECOND).convertTo(unit).getValue();
	}

	/**
	 * @param input
	 *            a length of time
	 * @return the same length of time with human-friendly units
	 */
	public static Dt fixUnits(Dt input) {
		TUnit bestUnit = pickTUnit(input.getMillisecs());
		return input.convertTo(bestUnit);
	}

	/**
	 * Convenience & safety around using {@link Calendar#DAY_OF_WEEK}
	 * @param t
	 * @return day
	 */
	public static KDay getDayOfWeek(Time t) {
		int dow = t.getCalendar().get(Calendar.DAY_OF_WEEK);
		return KDay.values()[dow - 1];
	}

	/**
	 * Convenience for {@link #getDays(Time, Time)}.
	 * 
	 * @param period
	 * @return an iterator which returns midnight for each day between start and
	 *         end (inclusive).
	 */
	public static TimeIterator getDays(Pair<Time> period) {
		return getDays(period.first, period.second);
	}

	/**
	 * @param start
	 * @param end
	 * @return an iterator which returns midnight for each day between start and
	 *         end (inclusive).
	 */
	public static TimeIterator getDays(Time start, Time end) {
		start = getStartOfDay(start);
		end = getStartOfDay(end);
		return new TimeIterator(start, end, new Dt(1, TUnit.DAY));
	}

	/**
	 * @param time
	 * @return midnight (GMT) on that day
	 * @see TimeOfDay which handles timezones
	 */
	public static Time getStartOfDay(Time time) {
		GregorianCalendar cal = time.getCalendar();
		// zero everything below day
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		Time t = new Time(cal);
		return t;
	}
	


	/**
	 * @param time
	 * @return
	 * @see TimeOfDay which handles timezones
	 */
	public static Time getEndOfDay(Time time) {
		GregorianCalendar cal = time.getCalendar();
		// zero everything below day
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		cal.set(Calendar.HOUR_OF_DAY, 24);
		Time t = new Time(cal);
		return t;
	}

	public static Time getStartOfHour(Time time) {
		GregorianCalendar cal = time.getCalendar();
		// zero everything below hour
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		Time t = new Time(cal);
		return t;
	}

	public static Time getStartOfMonth(Time time) {
		GregorianCalendar cal = time.getCalendar();
		// zero lots
		cal.set(Calendar.DAY_OF_MONTH, 1);
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		Time t = new Time(cal);
		return t;
	}

	/**
	 * @return e.g. Wed, 4 Jul 2008 14:02:20
	 */
	public static String getTimeStamp() {
		return TimeUtils.timeStamp.format(new Date());
	}

	/**
	 * Alternative to the confusing Date constructor.
	 * 
	 * @param year
	 *            The year, e.g. 2008
	 * @param month
	 *            The month, not zero indexed so january=12
	 * @param day
	 *            , The day-of-month, not zero-indexed
	 * @return a Date object
	 */
	public static Date newDate(int year, int month, int day) {
		assert month > 0 && day > 0 && month < 13 && day < 32;
		return new Date(year - 1900, month - 1, day);
	}

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
	public static Time parse(String string, String pattern) {
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
	public static Dt parseDt(String dt) throws IllegalArgumentException {
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

	private static TUnit parseDt2_unit(String us) {
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
	public static Time parseExperimental(String s) throws IllegalArgumentException {
		return parseExperimental(s, null);
	}
	
	/**
	 * 
	 * @param s
	 * @param isRelative Can be null. If provided, will be set to true for relative times, such as "today" or "yesterday"
	 * @return
	 */
	public static Time parseExperimental(String s, AtomicBoolean isRelative) throws IllegalArgumentException {
		s = s.trim().toLowerCase();
		// standard?
		try {
			Time t = new Time(s);
			return t;
		} catch (Exception e) {
			// oh well
		}

		// TODO new strategy:
		// - use regexs to pick out markers for day, month, hour, dt
		String month = null, day = null, hour = null;
		int year = -1;
		// - build a time object based on what we find

		{ // year
			Matcher m = YEAR.matcher(s);
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

		}
		
		if (month != null && year != -1) {
			String formatPattern = "dd MMM yyyy";
			if (day != null) formatPattern = "EEE " + formatPattern;
			DateFormat df = new SimpleDateFormat(formatPattern);
			df.setLenient(false);
			
			// day of month
			Matcher m = Pattern.compile("\\d+").matcher(s);
			Date date = null;
			while (m.find()) {
				int dayMonth = Integer.parseInt(m.group());
				String formatted = day + " " + dayMonth + " " + month + " " + year;
				try {
					date = df.parse(formatted);
					break;
				} catch (ParseException ex) {
					continue;
				}
			}
			if (date != null) return new Time(date);
		}

		// special strings
		if (s.equals("today") || s.equals("now")) {
			if (isRelative!=null) isRelative.set(true);
			return new Time();
		} else if (s.equals("yesterday")) {			
			s = "1 day ago";
		} else if (s.equals("tomorrow")) {			
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
						return lastDay;
					}
				}				
				return lastDay;
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
			if (startEnd==null) return t;
			// TODO don't assume month -- also handle "start of last week"
			Time t2 = TimeUtils.getStartOfMonth(t);
			if ("start".equals(startEnd)) {
				return t2;
			} else {
				Time t3 = t2.plus(TUnit.MONTH).minus(TUnit.MILLISECOND);
				return t3;
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

	/**
	 * @param timeSpec
	 *            e.g. "18/11/09 to 23/11/09"
	 * @param pattern
	 *            SimpleDateFormat, e.g. "dd/MM/yy"
	 * @testedby {@link TimeUtilsTest#testParsePeriod}
	 */
	public static Period parsePeriod(String timeSpec, String pattern) {
		String[] se = timeSpec.split("\\s+to\\s+");
		assert se.length == 2;
		Time start = parse(se[0], pattern);
		Time end = parse(se[1], pattern);
		return new Period(start, end);
	}

	/**
	 * Select the TUnit best suited to representing this amount of time.
	 * 
	 * @param milliseconds
	 *            Can be negative - the abs value is used
	 */
	public static TUnit pickTUnit(long milliseconds) {
		// give about a 10% tolerance, e.g. 90% of a day is a day
		milliseconds = Math.round(1.1 * Math.abs(milliseconds));
		for (int i = 1; i < TUnit.values().length; i++) {
			TUnit u = TUnit.values()[i];
			if (u.getMillisecs() > milliseconds) {
				TUnit v = TUnit.values()[i - 1];
				return v;
			}
		}
		return TUnit.YEAR;
	}

	/**
	 * Pick the best TUnit to describe this period. Period may be negative.
	 * 
	 * @param period
	 * @return
	 */
	public static String toString(Dt period) {
		return toString(period, TUnit.MILLISECOND);
	}

	/**
	 * Don't use smaller units than minimumUnit. Convert timeunit into readable
	 * form. Will round to the nearest unit, eg 1 hour 15 -> 1 hour. Negative
	 * time periods get a - sign.
	 * 
	 * @param period
	 * @param minimumUnit
	 * @return english
	 * @testedby {@link TimeUtilsTest#testToStringTime()}
	 */
	public static String toString(Dt period, TUnit minimumUnit) {
		assert period != null;
		assert minimumUnit != null;
		long millisecs = Math.abs(period.getMillisecs());
		TUnit bigUnit = pickTUnit(millisecs);
		// Force bigUnit to be at least minimumUnit
		if (bigUnit.compareTo(minimumUnit) < 0) {
			bigUnit = minimumUnit;
			return (period.getValue() < 0 ? "-" : "") + "less than one "
					+ minimumUnit.toString().toLowerCase();
		}

		double v = (1.0 * millisecs) / bigUnit.millisecs;
		int iv = (int) Math.round(v);
		return (period.getValue() < 0 ? "-" : "") + iv + " "
				+ bigUnit.toString().toLowerCase() + (iv > 1 ? "s" : "");
		//
		// TODO should this ever do remainders? Or just round off?
		// Dt firstPart = truncate(period, bigUnit);
		// if (Math.abs(new Double(firstPart.getValue()).intValue()) < 1
		// || bigUnit.getNextSmallerUnit().compareTo(minimumUnit) < 0) {
		// return toString2_wholeUnitToString(firstPart);
		// }
		// Dt remainder = new Dt(period.getTime() - firstPart.getTime(),
		// TUnit.MILLISECOND);
		// // do we want a remainder?
		// Dt secondPart = truncate(remainder, bigUnit.getNextSmallerUnit());
		// String retVal = toString2_wholeUnitToString(firstPart);
		// if (Math.floor(secondPart.getValue()) != 0) {
		// retVal += " and " + toString2_wholeUnitToString(secondPart);
		// }
		// return retVal;
	}

	/**
	 * Equivalent to {@link #toString(Time)}
	 * 
	 * @param millisecs
	 * @return a nice string for millisecs, based on the time now.
	 * @see Dt#toNiceString()
	 */
	public static String toString(long millisecs) {
		return toString(new Time(millisecs));
	}

	public static String toString(Period period) {
		// does it start/end now?
		long now = System.currentTimeMillis();
		Time e = period.getEnd();
		Time s = period.getStart();
		if (e.getTime() - s.getTime() >= TUnit.HOUR.millisecs) {
			if (Math.abs(e.getTime() - now) < TUnit.MINUTE.millisecs)
				return TimeUtils.toString(s) + " to now";
			if (Math.abs(s.getTime() - now) < TUnit.MINUTE.millisecs)
				return "now to " + TimeUtils.toString(e);
		}
		return TimeUtils.toString(s) + " to " + TimeUtils.toString(e);
	}

	/**
	 * Produce a nice String, based on the time now.
	 * 
	 * @param time
	 * @return e.g. "1 hour ago", "yesterday", "12th May 2009"
	 */
	public static String toString(Time time) {
		return toString(time, TUnit.SECOND);
	}

	/**
	 * Produce a nice String, based on the time now with the specified minimum
	 * unit.
	 * 
	 * @param time
	 * @param minimumUnit
	 * @return e.g. "1 hour ago", "yesterday", "12th May 2009"
	 * "now" covers anytime within minimumUnit of now.
	 */
	public static String toString(Time time, TUnit minimumUnit) {
		Time now = new Time();		
		Dt dt = now.dt(time);
		if (Math.abs(dt.getMillisecs()) < minimumUnit.millisecs) {
			return "now";
		}
		// TODO prefer exact dates for more than a few days ago??
		String s = toString(dt, minimumUnit);
		if (time.isBefore(now)) {
			assert s.charAt(0) == '-' : s;
			s = s.substring(1) + " ago";
		} else {
			s += " from now";
		}
		return s;
	}

	private static String toString2_wholeUnitToString(Dt time) {
		int num = Math.abs(new Double(time.getValue()).intValue());
		if (num == 0)
			return "less than one " + time.getUnit().toString().toLowerCase();
		return num + " " + time.getUnit().toString().toLowerCase()
				+ (num != 1 ? "s" : "");
	}

	private static Dt truncate(Dt period, TUnit unit) {
		Dt conversion = period.convertTo(unit);
		return wholeNumber(unit, conversion);
	}

	private static Dt wholeNumber(TUnit unit, Dt dt) {
		double value = dt.getValue();
		double trunc = value > 0 ? Math.floor(value) : Math.ceil(value);
		return new Dt(trunc, unit);
	}

	/**
	 * @param a
	 * @param b
	 * @param tolerance
	 * @return true if a & b are within tolerance (using millisecond comparison,
	 *         not calendar comparison) of each other.
	 */
	public static boolean equalish(Time a, Time b, TUnit tolerance) {
		return Math.abs(a.diff(b)) < tolerance.getMillisecs();
	}

	/**
	 * Returns a new Time with the same date as the first parameter, and the
	 * time of day (hour, minute, second) from the second parameter. Resets
	 * milliseconds to 0.
	 * 
	 * @param t
	 *            Input time
	 * @param time_of_day
	 *            Time to take time-of-day from
	 * @returns new Time with the same date as the original, but the specified
	 *          time of day.
	 */
	public static Time combineDateAndTime(Time t1, Time t2) {
		return new Time(t1.getYear(), t1.getMonth(), t1.getDayOfMonth(),
				t2.getHour(), t2.getMinutes(), t2.getSeconds());
	}

	/**
	 * Convenience for <code>a.isAfter(b)? a : b</code> that handles nulls
	 * 
	 * @param a
	 *            Can be null
	 * @param b
	 *            Can be null
	 * @return the latest most-recent of a and b. null if both a and b are null
	 */
	public static Time last(Time a, Time b) {
		if (a == null)
			return b;
		if (b == null)
			return a;
		return a.isAfter(b) ? a : b;
	}

	/**
	 * Convenience for <code>a.isBefore(b)? a : b</code> that handles nulls
	 * 
	 * @param a
	 *            Can be null
	 * @param b
	 *            Can be null
	 * @return the earliest of a and b. null if both a and b are null
	 */
	public static Time first(Time a, Time b) {
		if (a == null)
			return b;
		if (b == null)
			return a;
		return a.isBefore(b) ? a : b;
	}

	public static Time first(List<Time> times) {
		if (times.isEmpty())
			return null;
		Time f = times.get(0);
		for (Time time : times) {
			if (time.isBefore(f))
				f = time;
		}
		return f;
	}

	public static Time last(List<Time> times) {
		if (times.isEmpty())
			return null;
		Time f = times.get(0);
		for (Time time : times) {
			if (time.isAfter(f))
				f = time;
		}
		return f;
	}

	/**
	 * Compare with WELL_OLD (1900)
	 * @param start Must not be null
	 * @return true if start is eual-to / older than the backstop date WELL_OLD
	 */
	public static boolean isOldBackstop(Time start) {
		return start.getTime() <= WELL_OLD.getTime();
	}

	/**
	 * 
	 * @param t Only the date and hour from this are used. Minutes are not.
	 * @param timeZone
	 * @return Normally an int, but India does have a 5.5 offset
	 */
	public static float getLocalHour(Time t, TimeZone timeZone) 
	{
		float offset = timeZone.getOffset(t.getTime());
		float tz = offset / TUnit.HOUR.millisecs;
		return t.getHour() + tz;
	}


	public static List<Period> getShifts(IShift slots, final Time start, final Time end) {
		ArrayList<Period> m1Slots = new ArrayList();
		Time start2 = start.plus(TUnit.MILLISECOND);
		while(start2.isBefore(end)) {
			Time on = slots.isOn(start2)? slots.shiftStart(start2) : slots.nextOn(start2);
			slots.nextOn(on);
			Time off = slots.nextOff(on.plus(TUnit.MILLISECOND));
			assert off.isAfter(on);
			start2 = off.plus(TUnit.MILLISECOND);
			Period slot = new Period(on,off);			
			m1Slots.add(slot);
		}
		return m1Slots;		
	}
}
