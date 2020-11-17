package com.winterwell.utils.time;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.winterwell.utils.BestOne;
import com.winterwell.utils.StrUtils;
import com.winterwell.utils.Utils;

/**
 * Time at work
 * 
 * @author daniel
 * @testedby  OfficeHoursTest}
 */
public class OfficeHours implements IShift {

	public static final String ALWAYS_ON = "24/7";

	List<IShift> shifts = new ArrayList();
	
	/**
	 * for debug
	 */
	private String spec;

	/**
	 * for debug
	 */
	private TimeZone timezone;	

	
	/**
	 * Default case: e.g. mon,tue,wed,thur,fri, 9-5 GMT
	 */
	public OfficeHours(TimeZone timezone) {
		this("mon,tue,wed,thu,fri 9:00-17:00",timezone);
	}

	public OfficeHours(String spec, TimeZone timezone) {
		this.spec = spec;
		this.timezone = timezone;
		String[] specs = spec.split(";");
		for (String s : specs) {
			s = s.trim();
			if (ALWAYS_ON.equals(s)) {
				shifts.add(IShift.ROUND_THE_CLOCK);
				continue;
			}			
			if (Utils.isBlank(s)) continue;						
			SimpleOfficeHours oh = new SimpleOfficeHours(s, timezone);
			shifts.add(oh);
		}
		assert ! shifts.isEmpty();
	}
	

	/**
	 * @param t
	 * @return true if t is within office hours.
	 */
	public boolean isOn(Time t) {
		for(IShift shift : shifts) {
			if (shift.isOn(t)) return true;
		}
		return false;
	}
	
	public String getSpec() {
		return spec;
	}

	@Override
	public String toString() {
		return spec;
	}
	/**
	 * 
	 * @param t
	 * @return next start-on-time after-or-equal t (ie. = t if t is "on")
	 */
	public Time nextOn(final Time t) {
		if (isOn(t)) {
			return t;
		}
		BestOne<Time> first = new BestOne<Time>();
		for(IShift shift : shifts) {
			Time t2 = shift.nextOn(t);
			assert t2.isAfter(t) : t2+" v "+t+" w "+shift+" from spec:"+spec+" tz:"+timezone;
			first.maybeSet(t2, - t2.getTime());
		}
		return first.getBest();
	}
	
	/**
	 * 
	 * @param t
	 * @return next end-off-time after-or-equal t (ie. = t if t is "off")
	 */
	public Time nextOff(Time t) {
		assert t != null;
		if ( ! isOn(t)) {
			return t;
		}
		BestOne<Time> first = new BestOne<Time>();
		for(IShift shift : shifts) {
			Time t2 = shift.nextOff(t);
			// ??should we test isOn(t2)??
			if ( ! t2.isAfter(t)) {
				continue; // this shift is off (maybe wrong day) -- but another shift may be on
			}
			first.maybeSet(t2, - t2.getTime());
		}
		Time off = first.getBest();
		assert off != null : this+" "+t;
		return off;
	}

	
	@Override
	public Time shiftStart(Time now) {		
		BestOne<Time> first = new BestOne<Time>();
		for(IShift shift : shifts) {
			Time t2 = shift.shiftStart(now);	
			assert t2.isBefore(now) || t2.equals(now);
			first.maybeSet(t2, t2.getTime());
		}
		return first.getBest();
	}
}

/**
 * Time at work
 * 
 * @author daniel
 * @testedby  OfficeHoursTest}
 */
class SimpleOfficeHours implements IShift {	
	
	TimeOfDay dayEnd;
	TimeOfDay dayStart;
	
	/**
	 * Uses Calendar.DAY_OF_WEEK - 1 (to be zero indexed) 
	 */
	final boolean[] onDays = new boolean[7];
	/**
	 * for debug
	 */
	private String spec;
	private TimeZone timezone;	

	
	/**
	 * Default case: e.g. mon,tue,wed,thur,fri, 9-5 GMT
	 */
	public SimpleOfficeHours(TimeZone timezone) {
		this("mon,tue,wed,thu,fri", 
				new TimeOfDay(9, 0, timezone), 
				new TimeOfDay(17, 0, timezone));
	}

	public SimpleOfficeHours(String spec, TimeZone timezone) {
		this.spec = spec;
		this.timezone = timezone;
		// HACK: push 09:00 - 17:00 into one bit
		spec = spec.replace(" - ", "-");
		List<String> bits = StrUtils.split(spec);
		
		// ugly copy & paste
		List<String> week = new ArrayList(7);
		Calendar gcal = Calendar.getInstance();
		// SUNDAY = 1, MONDAY = 2
		for (int i = 0; i < 7; i++) {
			gcal.set(Calendar.DAY_OF_WEEK, i+1);
			String day = gcal.getDisplayName(Calendar.DAY_OF_WEEK,
					Calendar.SHORT, Locale.ENGLISH);
			day = day.substring(0, 3).toLowerCase();
			week.add(day);
		}		
		
		// 24 hour clock -- TODO flexible
		Pattern time = Pattern.compile("(\\d?\\d)[:\\.](\\d\\d) ?- ?(\\d?\\d)[:\\.](\\d\\d)");
		boolean onDay = false;
		for (String bit : bits) {
			if (StrUtils.AZ.matcher(bit).matches()) {
				String day = bit.substring(0, 3).toLowerCase();
				int i = week.indexOf(day);
				assert i != -1 : bit+" from "+spec;
				onDays[i] = true;
				onDay = true;
				continue;
			}
			// times
			Matcher m = time.matcher(bit);
			boolean ok = m.matches();
			if ( ! ok) {
				throw new IllegalArgumentException("Could not match: "+bit+" from "+this.spec);
			}
			this.dayStart = new TimeOfDay(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)), timezone);
			this.dayEnd = new TimeOfDay(Integer.parseInt(m.group(3)), Integer.parseInt(m.group(4)), timezone);
		}		
		if (dayStart==null) dayStart = new TimeOfDay(0, 0, timezone);
		if (dayEnd==null) dayEnd = new TimeOfDay(23, 59, 59, timezone);
		
		// no days = all days
		if ( ! onDay) Arrays.fill(onDays, true);
	}
	
	/**
	 * @param days
	 *            comma separated, eg. "mon,tues,wed,thur,fri,sat,sun"
	 * @param dayStart
	 * @param dayEnd
	 */
	public SimpleOfficeHours(String days, TimeOfDay dayStart, TimeOfDay dayEnd) {
		this.dayStart = dayStart;
		this.dayEnd = dayEnd;
		this.spec = days;
		
		String[] daysOn = days.split(",");
		
		List<String> week = new ArrayList(7);
		Calendar gcal = Calendar.getInstance();
		// SUNDAY = 1, MONDAY = 2
		for (int i = 0; i < 7; i++) {
			gcal.set(Calendar.DAY_OF_WEEK, i+1);
			String day = gcal.getDisplayName(Calendar.DAY_OF_WEEK,
					Calendar.SHORT, Locale.ENGLISH);
			day = day.substring(0, 3).toLowerCase();
			week.add(day);
		}

		for (String day : daysOn) {
			day = day.substring(0, 3).toLowerCase();
			int i = week.indexOf(day);
			onDays[i] = true;
		}
	}


	/**
	 * @param t
	 * @return true if t is within office hours.
	 */
	public boolean isOn(Time t) {
		// Day of week?
		GregorianCalendar cal = t.getCalendar();
		if ( ! isOnDay(cal)) return false;
		
		// time of day?
		return isOnTime(t);
	}

	private boolean isOnTime(Time t) {
		if (dayStart.isAfter(t)) {
			return false;
		}
		if (dayEnd.isBefore(t)) {
			return false;
		}
		return true;
	}

	private boolean isOnDay(Calendar cal) {
		if (dayStart.getTimezone()!=null) {
			cal = (Calendar) cal.clone();
			cal.setTimeZone(dayStart.getTimezone());
		}
		int dow = cal.get(Calendar.DAY_OF_WEEK);
		return onDays[dow - 1];
	}

	@Override
	public String toString() {
		return "SimpleOfficeHours["+spec+"]";
	}
	
	public Time nextOn(Time t) {
		Calendar cal = t.getCalendar();
		if (isOnDay(cal) && isOnTime(t)) {
			return t;
		}
		while(true) {
			String cal1 = new Time(cal).toString();
			cal = dayStart.advance(cal);
			String cal2 = new Time(cal).toString();
			// Advance to next day?
			if (isOnDay(cal)) {
				return new Time(cal);
			}
		}
	}
	
	public Time shiftStart(Time now) {
		Time on = dayStart.set(now);
		if (on.isBefore(now) || on.equals(now)) return on;
		// step back a day
		Time now2 = now.minus(TUnit.DAY);
		Time on2 = dayStart.set(now2);
		return on2;
	}
	
	
	public Time nextOff(Time t) {
		Calendar cal = t.getCalendar();
		// off now?
		if ( ! isOnDay(cal) || ! isOnTime(t)) {
			return t;
		}
		// every day has an off time -- so no need for a loop as in nextOn()
		cal = dayEnd.advance(cal);
		return new Time(cal);		
	}

}
