package com.winterwell.web.fields;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.mail.internet.MailDateFormat;

import com.winterwell.utils.StrUtils;
import com.winterwell.utils.time.Time;
import com.winterwell.utils.time.TimeUtils;

/**
 * Enter dates (and times). TODO a (popup) javascript calendar widget. TODO
 * handle time zones configurably
 * 
 * Note: This uses a human-readable format which can lose upto a second of precision.
 * 
 * @see TimeField which is more flexible
 * TODO gut this, and use TimeField to do the heavy lifting.
 * 
 * @see DateFormatField which is more rigid / predictable
 * @author daniel
 * @testedby {@link DateFieldTest}
 */
public class DateField extends AField<Time> {	
	
	static final SimpleDateFormat[] formats = initFormats();

	private static final long serialVersionUID = 1L;

	private static SimpleDateFormat[] initFormats() {
		List<SimpleDateFormat> _formats = new ArrayList();
		
		_formats.add(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sssZ")); // ISO 8601, with milliseconds.
		_formats.add(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss'Z'")); // WTF, Java doesn't parse Z properly?
		_formats.add(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss Z")); // ISO 8601, with milliseconds.
		_formats.add(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")); // ISO 8601, with seconds.
		_formats.add(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")); // WTF, Java doesn't parse Z properly?
		_formats.add(new SimpleDateFormat("yyyy-MM-dd'T'HH:mmZ")); // ISO 8601, without seconds.
		
		// formats[0] -- the "canonical" format
		_formats.add(new SimpleDateFormat("dd/MM/yyyy HH:mm:ss Z"));
		_formats.add(new SimpleDateFormat("dd/MM/yyyy HH:mm Z"));
		try {
			Class<?> mdf = Class.forName("javax.mail.internet.MailDateFormat");
			Object mdfi = mdf.newInstance();
			_formats.add((SimpleDateFormat) mdfi);
		} catch (Exception ex) {
			// oh well
		}
		// This is for YahooSearch
		// UK standard format (2-digit year first, otherwise they get misread)
		_formats.add(new SimpleDateFormat("dd/MM/yy"));
		_formats.add(new SimpleDateFormat("dd/MM/yyyy"));
		// US standard
		_formats.add(new SimpleDateFormat("MM/dd/yy"));
		// year first "computer friendly" 
		_formats.add(new SimpleDateFormat("yyyy/MM/dd"));		
		_formats.add(new SimpleDateFormat("MMM yyyy"));		
		_formats.add(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
		_formats.add(new SimpleDateFormat("yyyy-MM-dd")); // that's a shit format (homeserve "rss" feed)

		// assume GMT! And 2000!
		TimeZone zone = TimeZone.getTimeZone("GMT");
		for (SimpleDateFormat df : _formats) {
			df.setTimeZone(zone);
			if (df instanceof MailDateFormat) {
				continue; // MDF throws an exception if 2digityearstart is touched!
			}
			// 21st century
			Date c = df.get2DigitYearStart();
			df.set2DigitYearStart(new Time(2000,1,1).getDate());
		}
		return _formats.toArray(new SimpleDateFormat[0]);
	}


	/**
	 * First tries the "canonical" "HH:mm dd/MM/yyyy", then the other formats,
	 * finally {@link TimeUtils#parseExperimental(String)}.
	 */
	public static Time parse(String v) {
		return parse(v, null);
	}
	
	/**
	 * NB: duplicated in TimeField
	 * 
	 * Attempts to parse a string to a date/time as several standard formats, returns null for specific malformed strings similar to "+0000", and finally attempts natural-language parsing.
	 * @param v A String which should represent a date/time.
	 * @param isRelative True if the String represents a time relative to now
	 * @return A Time object on successful parsing, or null for strings similar to "+0000"
	 */
	private static Time parse(String v, AtomicBoolean isRelative) {
		assert isRelative==null || ! isRelative.get() : v;
		// UTC milliseconds code?
		if (StrUtils.isInteger(v)) {
			return new Time(Long.parseLong(v));
		}
		for (SimpleDateFormat df : formats) {
			try {
				// NOTE: SimpleDateFormat.parse and SimpleDateFormat.format
				// are not thread safe... hence the .clone
				// (http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4228335)
				// - @richardassar
				SimpleDateFormat df2 = (SimpleDateFormat) df.clone();
				
				// Don't use heuristics to interpret inputs that don't exactly match the format.
				df2.setLenient(false);
				
				Date date = df2.parse(v);
				if (date==null) continue; // WTF?! Happens.
				// NB: includes timezone				
				String patternForDebug = df.toPattern();
//				Date c = df2.get2DigitYearStart();				
				Time t = new Time(date);
				return t;
			} catch (Exception e) {
				// oh well - try something else
			}
		}

		// catch malformed strings with a time zone and no date/time & return null
		if(v.matches("^[+-]\\d\\d\\d\\d\\W*$")) {
			return null;
		}
		
		// support for e.g. "yesterday"
		Time t = TimeUtils.parseExperimental(v, isRelative);
		return t;
	}

	public DateField(String name) {
		super(name, "text");
		// The html5 "date" type is not really supported yet.
		// What it does do on Firefox is block non-numerical text entry, which
		// we want to support
		cssClass = "DateField";
	}

	/**
	 * First tries the "canonical" "HH:mm dd/MM/yyyy", then the other formats,
	 * finally {@link TimeUtils#parseExperimental(String)}.
	 */
	@Override
	public Time fromString(String v) {
		return parse(v);
	}

	@Override
	public String toString(Time time) {
		return toString2(time);
	}
	
	static String toString2(Time time) {
		// BC?
		if (time.isBefore(TimeUtils.AD)) {
			// TODO include BC for a pretty string
			return Long.toString(time.getTime());
		}
		// send a human readable ISO8601 string
		String s = time.toISOString();

		//		((SimpleDateFormat) formats[0].clone()).format(time
//				.getDate());		
		// NOTE: SimpleDateFormat.parse ___AND___ SimpleDateFormat.format are
		// not thread safe...
		// hence the .clone
		// (http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4228335) -
		// @richardassar
		return s;
	}
	
	@Override
	public Class getValueClass() {
		return Time.class;
	}
}
