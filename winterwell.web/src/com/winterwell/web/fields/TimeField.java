package com.winterwell.web.fields;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

import com.winterwell.utils.Constant;
import com.winterwell.utils.StrUtils;
import com.winterwell.utils.Utils;
import com.winterwell.utils.threads.ICallable;
import com.winterwell.utils.time.Period;
import com.winterwell.utils.time.Time;
import com.winterwell.utils.time.TimeUtils;

/**
 * TODO Like DateField, adding relative-time answers such as "last week"
 * 
 * Enter dates (and times). TODO a (popup) javascript calendar widget. TODO
 * handle time zones configurably
 * 
 * @author daniel
 * @testedby {@link TimeFieldTest}
 */
public class TimeField extends AField<ICallable<Time>> {

	private static final long serialVersionUID = 1L;

	boolean preferEnd;
	
	/**
	 * If true, then a month will be interpreted as the end of the month.
	 * @param preferEnd
	 * @return 
	 */
	public TimeField setPreferEnd(boolean preferEnd) {
		this.preferEnd = preferEnd;
		return this;
	}
	
	public TimeField(String name) {
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
	public ICallable<Time> fromString(String v) {
		AtomicBoolean isRel = new AtomicBoolean();
		// HACK fixing bugs elsewhere really. Handle "5+days+ago" from a query
		v = v.replace('+', ' ');		
		
		Time t = parse(v, isRel);
		if ( ! isRel.get()) {
			return new Constant(t);
		}
		// ??Relative but future (eg. "tomorrow")... make it absolute? No -- let the caller make that decision.		
		// e.g. "1 day ago" or "tomorrow"
		return new RelTime(v); 
	}
	
	/**
	 * Attempts to parse a string to a date/time as several standard formats, returns null for specific malformed strings similar to "+0000", and finally attempts natural-language parsing.
	 * @param v A String which should represent a date/time.
	 * @param isRelative True if the String represents a time relative to now
	 * @return A Time object on successful parsing, or null for strings similar to "+0000"
	 */
	Time parse(String v, AtomicBoolean isRelative) {
		assert isRelative==null || ! isRelative.get() : v;
		// UTC milliseconds code?
		if (StrUtils.isInteger(v)) {
			return new Time(Long.parseLong(v));
		}
		for (SimpleDateFormat df : DateField.formats) {
			try {
				// NOTE: SimpleDateFormat.parse and SimpleDateFormat.format
				// are not thread safe... hence the .clone
				// (http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4228335)
				// - @richardassar
				SimpleDateFormat df2 = (SimpleDateFormat) df.clone();
				String patternForDebug = df2.toPattern();
				// Don't use heuristics to interpret inputs that don't exactly match the format.
				df2.setLenient(false);
				
				Date date = df2.parse(v);
				if (date==null) continue; // WTF?! Happens.
				// NB: includes timezone				
				
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
		Period t = TimeUtils.parsePeriod(v, isRelative);
		// start/end of month
		return preferEnd? t.getEnd() : t.getStart();
	}

	@Override
	public String toString(ICallable<Time> _time) {
		// relative?
		if (_time instanceof RelTime) {
			return ((RelTime) _time).v;
		}
		try {
			Time time = _time.call();
			return toString(time);
		} catch (Exception e) {
			throw Utils.runtime(e);
		}
	}

	public String toString(Time time) {
		return DateField.toString2(time);
	}

	@Override
	public Class<ICallable<Time>> getValueClass() {
		return (Class) Callable.class;
	}
}


final class RelTime implements ICallable<Time>, Serializable {
	private static final long serialVersionUID = 1L;
	final String v;
	
	public RelTime(String v) {
		this.v = v;
	}
	
	@Override
	public String toString() {
		return "RelTime["+v+"]";
	}

	@Override
	public Time call() {
		return TimeUtils.parseExperimental(v);
	}
}
