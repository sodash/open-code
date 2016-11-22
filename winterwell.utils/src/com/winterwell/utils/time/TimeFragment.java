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

	/**
	 * Dangerous: this uses now as a default to work from! 
	 * @return
	 */
    public Time getTime() {
    	GregorianCalendar cal = new GregorianCalendar();
    	// do we have a date?
		if ( ! isDaySpecific()) {
			return null;
		}    	
    	for(Map.Entry<Integer, Object> me : values.entrySet()) {
    		Object v = me.getValue();
    		Integer vi;
    		if (v instanceof String) {
    			vi = convertToCalendarValue(me.getKey(), (String) v);
    		} else {
    			vi = ((Number) v).intValue();
    		}    	
    		if (vi != null) {
    			cal.set(me.getKey(), vi);
    		}
    	}
    	return new Time(cal);
    }
    
    protected Integer convertToCalendarValue(Integer key, String v) {
		return null;
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
    
}
