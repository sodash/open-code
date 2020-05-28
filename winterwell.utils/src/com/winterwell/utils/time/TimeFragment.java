package com.winterwell.utils.time;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;

/**
 * Describe part of a date or time, e.g. "11am", "Monday", or "21st June"
 * Created by daniel on 02/09/14.
 */
public class TimeFragment {
	private static final long serialVersionUID = 1L;
    
	/**
	 * Calendar field-index to value (String or Integer)
	 */

	private Map<Integer, Object> values = new HashMap();

	@Override
	public String toString() {
		return "TimeFragment[values=" + values +", time="+getTime()+"]";
	}
	/**
	 * How many properties have been set?
	 * @return
	 */
	public int numset() {
		return values.size();
	}
	/**
	 * Dangerous: this uses base (normally `now`) as a default to work from! 
	 * @return the time represented by this.
	 * TODO a getPeriod() version, which could better represent eg. "2018"
	 * @return null if base is unset, or day is unset 
	 */
    public Time getTime() {
    	if (base==null) {
    		return null;
    	}
    	// do we have a date?
		if ( ! isDaySpecific()) {
			return null;
		}
    	GregorianCalendar cal = (GregorianCalendar) base.clone();    	
    	for(Map.Entry<Integer, Object> me : values.entrySet()) {    		    		
    		Integer vi = getIntValue(me.getKey(), me.getValue());
    		if (vi != null) {
    			cal.set(me.getKey(), vi);
    		}
    	}
    	return new Time(cal);
    }
    
    private Integer getIntValue(int calField, Object value) {
    	if (value==null) return null;
		if (value instanceof String) {
			Integer vi = convertToCalendarValue(calField, (String) value);
			return vi;
		}
		return ((Number) value).intValue();		    
	}

	GregorianCalendar base = new Time().getCalendar();
    
	/**
	 * 
	 * @param base Can be null for "keep it vague"
	 * @return
	 */
    public TimeFragment setBase(Time base) {
		this.base = base==null? null : base.getCalendar();
		return this;
	}
    
    /**
     * TODO
     * @return
     * Never null -- unset = [AD, distant-future] 
     */
    private Period getPeriod() {
    	// TODO timezone
    	// set year, month, day, hour, minute, second
    	Integer year = getValue(TUnit.YEAR.getCalendarField());
    	if (year==null) {
    		return new Period(TimeUtils.AD, TimeUtils.DISTANT_FUTURE);    		
    	}
    	
    	Integer month = getValue(TUnit.MONTH.getCalendarField());
    	if (month==null) {
    		return new Period(new Time(year, 1, 1), new Time(year, 12, 31, 24, 0, 0));    		
    	}
    	
    	Integer day = getValue(TUnit.DAY.getCalendarField());
    	if (day==null) {
    		return new Period(new Time(year, month, 1), new Time(year, month + 1, 1).minus(TUnit.SECOND));    		
    	}
    	
    	Integer hr = getValue(TUnit.HOUR.getCalendarField());
    	if (hr==null) {
    		return new Period(new Time(year, month, day), new Time(year, month, day, 24, 0, 0));    		
    	}
    	
    	Integer min = getValue(TUnit.MINUTE.getCalendarField());
    	if (min==null) {
    		return new Period(new Time(year, month, day, hr, 0, 0), new Time(year, month, day, hr, 60, 0));    		
    	}
    	
    	Integer sec = getValue(TUnit.SECOND.getCalendarField());
    	if (sec==null) {
    		return new Period(new Time(year, month, day, hr, min, 0), new Time(year, month, day, hr, min, 60));    		
    	}
    	
    	return new Period(new Time(year, month, day, hr, min, sec), new Time(year, month, day, hr, min, sec));
    }
    
    
    private Integer getValue(int calendarField) {
    	Object v = values.get(calendarField);
    	if (v == null) {
    		if (base==null) return null;
    		int vi = base.get(calendarField);
    		return vi;
    	}
		if (v instanceof Number) return ((Number) v).intValue();
		return convertToCalendarValue(calendarField, (String) v);
	}

	protected Integer convertToCalendarValue(int calField, String v) {
		// are there special cases?
		return Integer.valueOf(v);
	}

	/**
     * 
     * @return true if this specifies a day.
     * Note: "3rd June" would return false, as it doesn't give the year.
     */
    public boolean isDaySpecific() {
    	boolean year = values.containsKey(Calendar.YEAR);
    	if ( ! year) return false;
    	boolean month = values.containsKey(Calendar.MONTH);
    	boolean dom = values.containsKey(Calendar.DAY_OF_MONTH);
    	if (year && month && dom) return true;
		boolean doy = values.containsKey(Calendar.DAY_OF_YEAR);
		boolean dow = values.containsKey(Calendar.DAY_OF_WEEK);
		boolean week = values.containsKey(Calendar.WEEK_OF_YEAR);
		if (year && doy) return true;
		if (year && week && dow) return true;
		boolean wom = values.containsKey(Calendar.WEEK_OF_MONTH);
		if (year && month && wom && dow) return true;
		return false;
	}

    /**
	 * Calendar field-index to value (String or Integer)
	 */
    public Map<Integer, Object> getValues() {
		return values;
	}
    
	public TimeFragment(){
    }
	
	public void putAll(TimeFragment fragments) {
		values.putAll(fragments.values);
	}
	
    public void put(TUnit field, int value) {
        values.put(field.getCalendarField(), value);
    }

    public void put(int calendarField, int value) {
        values.put(calendarField, value);
    }

    /**
     * Convenience for set year, month, day
     * @param date
     */
	public void setDate(Time date) {
		put(Calendar.YEAR, date.getYear());
		// why is month zero-indexed when other fields aren't?
		put(Calendar.MONTH, date.getMonth() - 1);
		put(Calendar.DAY_OF_MONTH, date.getDayOfMonth());
	}

	/**
	 * 
	 * @param i 1-12 (not zero-indexed)
	 */
	public void setMonth(int i) {
		assert i > 0 && i < 13 : i;
		put(Calendar.MONTH, i);
	}
    
}
