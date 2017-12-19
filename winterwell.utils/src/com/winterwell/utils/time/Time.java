package com.winterwell.utils.time;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.regex.Pattern;

import com.winterwell.utils.StrUtils;
import com.winterwell.utils.Utils;
import com.winterwell.utils.log.Log;

/**
 * A simple immutable alternative to the built-in mess of Date. This just holds
 * a UTC millisecond count. It cannot be edited, but provides convenient methods
 * for creating new Time objects.
 * <p>
 * Rationale: Date is a booby-trapped mess (years that gain and lose 1900
 * depending on the method?!, mixed zero-indexed and one-indexed properties?!,
 * etc.). {@link GregorianCalendar} isn't too bad, but it is kind of heavyweight
 * for many uses.
 * <p>
 * In places, this class uses Calendar - which is always the
 * {@link GregorianCalendar} with GMT (i.e. no daylight savings are applied!)
 * and 24 hour clock (ie. it uses {@link Calendar#HOUR_OF_DAY}). If you need
 * locale-specific calendar support, you should provide this yourself.
 * <p>
 * Can be sorted - sorts as earliest Time first.
 * 
 * @testedby {@link TimeTest}
 * @author daniel
 * 
 */
public class Time implements Serializable, Comparable<Time> {
	private static final long serialVersionUID = 1L;

	private static GregorianCalendar getCal(int year, int month, int day,
			int hour, int min, int sec) {
		// get a GMT time zone
		assert TimeUtils._GMT_TIMEZONE != null;
		GregorianCalendar cal = new GregorianCalendar(TimeUtils._GMT_TIMEZONE);
		// freshen it up
		cal.clear();
		cal.set(year, month - 1, day, hour, min, sec);
		// if (year<0) {
		// cal.set(Calendar.ERA, GregorianCalendar.BC);
		// }
		// Allow overflow, as calendar handles it nicely. But log a warning, as it could be a bug symptom
		if (month <= 0 || month > 12 || day <= 0 || day > 32) {
			Log.i("Time.getCal", "Odd day or month "+day+" of "+month+" in "+cal);
		}
//		assert day > 0 && day < 32 : day;
		return cal;
	}

	/**
	 * Unix Time code (no. seconds since 1970). This is the standard definition
	 * of time in many computer systems. Here it is expressed in milliseconds
	 * (the Java convention)
	 */
	private final long ut;

	/**
	 * Once upon a time called *right now*.
	 */
	public Time() {
		ut = System.currentTimeMillis();
	}

	public Time(Calendar cal) {
		this.ut = cal.getTime().getTime();
	}

	/**
	 * Create a Time object from a Date object.
	 * 
	 * @param date
	 */
	public Time(Date date) {
		this.ut = date.getTime();
	}

	public Time(double longValue) {
		this((long) longValue);
		// double err = Math.abs(longValue() - longValue);
		// if (err > 100) {
		// Log.report("Rounding error of "+new Dt(err, TUnit.MILLISECOND),
		// Level.WARNING);
		// }
	}

	/**
	 * Note: This does not behave the same as the (confusing) Date constructor.<br>
	 * 
	 * The Time will be midnight GMT (i.e. no daylight savings applied) at the
	 * start of the specified date.
	 * 
	 * @param year
	 *            The year, e.g. 2008
	 * @param month
	 *            The month, not zero indexed (unlike Calendar.MONTH) so
	 *            January=1. Must be in [1,12]
	 * @param day
	 *            The day-of-month, not zero-indexed (like
	 *            Calendar.DAY_OF_MONTH). Must be in [1,31]
	 */
	public Time(int year, int month, int day) {
		this(year, month, day, 0, 0, 0);
	}

	/**
	 * Note: This does <i>not</i> behave the same as the (confusing) Date
	 * constructor.
	 * <p>
	 * - zero-indexing is consistently not used (except for hour/min/sec where 0
	 * is a normal value)<br>
	 * - All times are in GMT (no daylight savings are applied) <br>
	 * 
	 * @param year
	 *            The year, e.g. 2008
	 * @param month
	 *            The month, not zero indexed so January=1. Must be in [1,12]
	 * @param day
	 *            The day-of-month, not zero-indexed. Must be in [1,31]
	 */
	public Time(int year, int month, int day, int hour, int min, int sec) {
		this(getCal(year, month, day, hour, min, sec));
		assert month > 0 && day > 0 : "Not zero-indexed!";
	}

	/**
	 * @param ut
	 *            Unix Time code (no. seconds since 1970). This is the standard
	 *            definition of time in many computer systems. Here it is
	 *            expressed in milliseconds (the Java convention)
	 */
	public Time(long ut) {
		this.ut = ut;
	}

	/**
	 * Uses {@link Date#parse(String)} to parse the string. 
	 * Behaviour will be locale-specific. It's best to use something else.
	 * 
	 * You should use
	 * {@link TimeUtils#parse(String, String)} for other patterns. 
	 * 
	 * @param date Can be a UTC timecode, or something Date can parse, or a ISO 8601 format dateTtime. Cannot be null
	 * @testedby {@link TimeTest#testTime()}
	 */
	public Time(String date) {
		if (date==null) throw new NullPointerException();
		ut = parse(date);
	}	

	private static final Pattern DATE_ONLY = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");
	
	private static long parse(String date) {
		// Is it a timecode?
		if (date.length() > 8 && date.length() < 24 && StrUtils.isInteger(date)) {
			return Long.valueOf(date);
		}
		// One Special case short value
		if ("0".equals(date)) {
			return 0;
		}
		// ISO date only?
		if (DATE_ONLY.matcher(date).matches()) {
			date += "T00:00:00Z"; // make it midnight GMT
		}				
		// Try ISO 8601 format
		// Note: This does not fully handle 8601 -- there are valid offsets which will cause an error.
		// E.g. "+0100" or "+01"
		try {
//				OffsetDateTime odt = OffsetDateTime.parse(date);
//				long oes = odt.toEpochSecond();
			ZonedDateTime zp = ZonedDateTime.parse(date);
			long zes = zp.toEpochSecond();
//				Date.from(zp.toInstant())
//				Instant ip = Instant.parse(date);
//				TemporalAccessor j8 = DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(date);
//				Instant instant = Instant.from(j8);
//				long es = instant.getEpochSecond();
//				Date idate = Date.from(instant);
//				SimpleDateFormat sdf = new SimpleDateFormat(iso8601inferZone);
//				Date parsed = sdf.parse(date);
//				_ut = parsed.getTime();
			return 1000*zes;
		} catch(Exception ex2) {	
			try {
				// Try Date, which can handle Time.toString()
				return Date.parse(date);
			} catch(Exception ex3) {
				// be more informative! _What_ failed to parse
				throw new IllegalArgumentException(StrUtils.ellipsize(date, 100));
			}
		}			
	}

	/**
	 * Sorts earliest-first
	 */
	@Override
	public int compareTo(Time o) {
		return (ut < o.ut ? -1 : (ut == o.ut ? 0 : 1));
	}

	/**
	 * Return the date formatted in the most common European style. XXX - maybe
	 * not a good idea from the i18n point of view.
	 * 
	 * @return
	 */
	public String ddMMyyyy() {
		return format("dd/MM/yyyy");
	}

	/**
	 * Time difference
	 * 
	 * @param b
	 * @return time from this to b in milliseconds, ie. positive if b is after
	 *         this.
	 * @see #diff(Time, TUnit)
	 */
	public long diff(Time b) {
		long dt = b.ut - ut;
		return dt;
	}

	/**
	 * Time difference: compute the time difference in the specified units. The
	 * dt will be positive if b is after this.
	 * 
	 * @param b
	 * @param unit
	 * @return time from this to b, ie. positive if b is after this
	 */
	public Dt diff(Time b, TUnit unit) {
		long d = diff(b);
		Dt dt = new Dt(d, TUnit.MILLISECOND);
		return unit == TUnit.MILLISECOND ? dt : dt.convertTo(unit);
	}

	/**
	 * Positive if b is after this time.
	 * 
	 * @param b
	 * @return
	 */
	public Dt dt(Time b) {
		long dt = diff(b);
		return new Dt(dt, TUnit.MILLISECOND);
	}

	/**
	 * equals() if same class and identical millisecond timecode.
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Time other = (Time) obj;
		if (ut != other.ut)
			return false;
		return true;
	}

	/**
	 * Less inconvenient way of getting a formatted string. Always format as GMT
	 * time so this works as the inverse operation to TimeUtils.parse()
	 * 
	 * @param format
	 *            E.g. "EEE, MMM d, yyyy" for "Wed, Jul 4, 2001", "h:mm a" for
	 *            12:08 PM. See {@link SimpleDateFormat} for details.
	 * @return
	 * @see SimpleDateFormat
	 */
	public String format(String format) {
		SimpleDateFormat df = new SimpleDateFormat(format);
//		TimeZone zone = TimeZone.getTimeZone("GMT");
		df.setTimeZone(TimeUtils._GMT_TIMEZONE);
		return df.format(getDate());
	}

	/**
	 * @return a new Gregorian calendar set to this time. Always uses the GMT/UTC timezone.
	 */
	public GregorianCalendar getCalendar() {
		GregorianCalendar cal = new GregorianCalendar(TimeUtils._GMT_TIMEZONE);
		cal.setTimeInMillis(ut);
		return cal;
	}
	
	/**
	 * Date is horrible, but useful for inter-operating with other systems.
	 */
	public Date getDate() {
		return new Date(ut);
	}

	/**
	 * not zero indexed
	 */
	public int getDayOfMonth() {
		return getCalendar().get(Calendar.DAY_OF_MONTH);
	}

	/**
	 * Convenience for using {@link GregorianCalendar} to get the hour in 24
	 * hour clock (GMT timezone).
	 * 
	 * @return hour, e.g. 23 for 11pm
	 */
	public int getHour() {
		return getCalendar().get(Calendar.HOUR_OF_DAY);
	}

	/**
	 * Convenience for using {@link GregorianCalendar} to get the minutes past
	 * the hour.
	 */
	public int getMinutes() {
		return getCalendar().get(Calendar.MINUTE);
	}

	/**
	 * not zero indexed so January=1
	 */
	public int getMonth() {
		return 1 + getCalendar().get(Calendar.MONTH);
	}

	/**
	 * Convenience for using {@link GregorianCalendar} to get the seconds in the
	 * minutes.
	 */
	public int getSeconds() {
		return getCalendar().get(Calendar.SECOND);
	}

	/**
	 * The UTC value for this Time, ie. milliseconds since Unix time-zero.
	 * <p>
	 * Note: this has the same behaviour as the {@link Date#getTime()} method,
	 * giving drop-in compatibility with Date
	 * 
	 * @return
	 */
	public long getTime() {
		return ut;
	}

	/**
	 * Convenience for using {@link GregorianCalendar} to get the year.
	 * 
	 * @return year, e.g. 2008 Negative for BC (which is different from the
	 *         Calendar class!)
	 */
	public int getYear() {
		GregorianCalendar cal = getCalendar();
		int yr = cal.get(Calendar.YEAR);
		int era = cal.get(Calendar.ERA);
		if (era == GregorianCalendar.BC)
			return -yr;
		return yr;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (ut ^ (ut >>> 32));
		return result;
	}

	/**
	 * @param t
	 * @return true if this is <em>strictly</em> after time t; false otherwise
	 */
	public boolean isAfter(Time t) {
		return ut > t.ut;
	}
	
	/**
	 * @param t
	 * @return true if this is after or equal to time t; false otherwise
	 */
	// NB: Yes, you could do this with t.isBefore(this). But this method can make for easier to read code.
	public boolean isAfterOrEqualTo(Time t) {
		return ut >= t.ut;
	}

	/**
	 * @param t
	 * @return true if t is <em>strictly</em> after this time; false otherwise
	 */
	public boolean isBefore(Time t) {
		return ut < t.ut;
	}
	/**
	 * @param t
	 * @return true if t is after this time or equal to it; false otherwise
	 */
	// NB: Yes, you could do this with t.isAfter(this). But this method can make for easier to read code.
	public boolean isBeforeOrEqualTo(Time t) {
		return ut <= t.ut;
	}

	/**
	 * Equivalent to {@link #getTime()}
	 */
	public long longValue() {
		return ut;
	}

	/**
	 * Opposite of {@link #plus(Dt)}
	 */
	public Time minus(Dt dt) {
		dt = new Dt(-dt.getValue(), dt.getUnit());
		return plus(dt);
	}

	/**
	 * Convenience for {@link #minus(Dt)}
	 * 
	 * @param n
	 * @param unit
	 * @return
	 */
	public Time minus(double n, TUnit unit) {
		return minus(new Dt(n, unit));
	}

	/**
	 * Convenience for {@link #minus(Dt)}
	 */
	public Time minus(TUnit dt) {
		return minus(dt.dt);
	}

	/**
	 * @param dt
	 * @return A new object that is time+dt. Uses Calendar to step neatly.
	 */
	public Time plus(Dt dt) {
		GregorianCalendar cal = getCalendar();
		dt.addTo(cal);
		return new Time(cal);
	}

	/**
	 * Add on more time. Uses Calendar to perform integer addition. E.g. adding
	 * 12 months = 1 year
	 * 
	 * @param n
	 *            Can be negative to subtract some time
	 * @param unit
	 * @return a new Time object
	 */
	public Time plus(int n, TUnit unit) {
		GregorianCalendar cal = getCalendar();
		cal.add(unit.getCalendarField(), n);
		return new Time(cal);
	}

	/**
	 * Convenience for {@link #plus(Dt)} where Dt = 1 unit
	 */
	public Time plus(TUnit dt) {
		return plus(dt.dt);
	}
	
	/**
	 * See ??
	 */
	final static String iso8601Z = "yyyy-MM-dd'T'HH:mm:ss'Z'";
	
	/**
	 * @return ISO 8601 format yyyy-MM-ddTHH:mm:ssZ
	 */
	public String toISOString() {	
		return format(iso8601Z);
	}
	/**
	 * Human readable GMT time. This is NOT ISO8601
	 * @see #toISOString()
	 */
	@SuppressWarnings("deprecation")
	@Override
	public String toString() {
		// GregorianCalendar cal = getCalendar();
		// if (cal.get(Calendar.ERA) == GregorianCalendar.BC) {
		// // TODO do something sensible here
		// }
		return getDate().toGMTString();
	}

	/**
	 * Lenient constructor - never throws an Exception.
	 * Use-case parsing input data, discarding "junk"
	 * @param string
	 * @return Time or null
	 */
	public static Time of(String string) {
		if (Utils.isBlank(string)) return null;
		try {
			Time time = new Time(string);
			return time;
		} catch(Exception ex) {
			return null;
		}
	}

}
