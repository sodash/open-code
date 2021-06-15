package com.winterwell.ical;


import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import com.winterwell.utils.Utils;
import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.time.Dt;
import com.winterwell.utils.time.TUnit;
import com.winterwell.utils.time.Time;
import com.winterwell.utils.time.TimeUtils;

/**
 * See http://www.kanzaki.com/docs/ical/rrule.html
 * @author daniel
 * @testedby RepeatTest
 */
public class Repeat {
	
	
	@Override
	public String toString() {
		// TODO a better tostring
		return "every "+(interval==1?"":interval+" ")+freq.toString().toLowerCase()+"(s)";
	}

	private String rrule;
	TUnit freq;
	
	/**
	 * @see #interval
	 * @return
	 */
	public TUnit getFreq() {
		return freq;
	}
	

	Time since;	
	
	/**
	 * must be set when making the Repeat. But the ical could specify start after the rrule, so 
	 * as a convenience this can be set.
	 */
	void setSince(Time since) {
		this.since = since;
	}
	/**
	 * Is this inclusive??
	 */
	Time until;
	/**
	 * Can be comma separated. Can have a number prefix, eg 1FR = first firday
	 */
	private String byday;
	/**
	 * e.g. 2 = every other day/week/etc
	 */
	private int interval = 1;
	private int count;
	private String bymonth;
	/**
	 * e.g. on the 5th of each month
	 */
	private String bymonthday;
	private List<Time> exclude;
	private static Map<String,TUnit> tunitForFreq = new ArrayMap(
			"MINUTELY", TUnit.MINUTE, // Only used for testing. What is the correct ical term?? 
			"HOURLY", TUnit.HOUR,
			"DAILY", TUnit.DAY, 
			"WEEKLY", TUnit.WEEK, 
			"MONTHLY", TUnit.MONTH, 
			"YEARLY", TUnit.YEAR);
	
	/**
	 * Has to handle out of order or broken lines (google does this)
	 * @param rrule e.g. "every 3 months" is "FREQ=MONTHLY;INTERVAL=3;"
	 */
	public Repeat(String rrule) {
		this.rrule= rrule;
		try {
			parse();
		} catch (ParseException e) {
			throw Utils.runtime(e);
		}
	}
	
	void add(String rrulebit) {
		if (this.rrule==null) {
			this.rrule = "";
		}
		this.rrule += rrulebit;
		try {
			parse();
		} catch (ParseException e) {
			throw Utils.runtime(e);
		}
	}
	
	public static String freqForTUnit(TUnit tunit) {
		if (tunit==TUnit.DAY) return "DAILY";
		String sfreq = tunit.toString()+"LY";
		return sfreq;
	}

	private void parse() throws ParseException {
		// e.g. FREQ=WEEKLY;UNTIL=20160722T160000Z;INTERVAL=6;BYDAY=FR
		String[] bits = rrule.split(";");
		for (String bit : bits) {
			if (Utils.isBlank(bit)) continue;
			String[] kv = bit.split("=");
			if (kv.length!=2) {
				Log.e("ical", "odd rrule "+rrule);
				continue;
			}
			String v = kv[1];
			switch(kv[0]) {
			case "FREQ":
				freq = (TUnit) tunitForFreq .get(v);
				break;
			case "UNTIL":
				until = ICalReader.parseTime(v, null);
				break;
			case "BYDAY":
				byday = v;
				break;
			case "BYMONTH":
				bymonth = v;
				break;
			case "BYMONTHDAY":
				bymonthday = v;
				break;
			case "INTERVAL":
				interval = Integer.valueOf(v);
				break;
			case "COUNT":
				count = Integer.valueOf(v);
				break;
			}
		}
	}

	/**
	 * 
	 * @param start Inclusive
	 * @param end Inclusive
	 * @return
	 */
	public List<Time> getRepeats(Time start, Time end) {
		Utils.check4null(start, end);
		// step 1msec back, so that start and end are exclusive
		start = start.minus(TUnit.MILLISECOND);
		end = end.plus(TUnit.MILLISECOND);
		assert interval>0;
		// If since is set, start there to get e.g. "1st day of the month" right when stepping 
		// -- as the start/end window might not match this
		Time mark = since;
		List<Time> periods = new ArrayList();
		int cnt = 0;
		while(mark.isBeforeOrEqualTo(end)) {
			Time n = getNext2(mark, start, end, cnt);
			if (n==null) break;
			periods.add(n);
			mark = n.plus(interval, freq);
		}
		return periods;
	}

	public void addExclude(Time exdate) {
		if (exclude==null) exclude = new ArrayList();
		exclude.add(exdate);
	}

	public Time getNext(Time prev) {
		Time windowStart = Utils.or(prev, new Time());
		Time backstop = TimeUtils.WELL_FUTURE;
		Time mark = since;
		int cnt = 0;
		return getNext2(mark, windowStart, backstop, cnt);
	}
	
	TimeZone timezone;
	
	/**
	 * 
	 * @param mark Can return this
	 * @param windowStart This is EXCLUSIVE
	 * @param backstop This is EXCLUSIVE
	 * @param cnt
	 * @return
	 */
	private Time getNext2(Time mark, Time windowStart, Time backstop, int cnt) {
		assert ! mark.isBefore(since);
		while(mark.isBefore(backstop)) {
			// all done?
			if (until!=null) {
				if (mark.isAfter(until)) { // should this be inclusive??
					return null;
				}
			}
			// excluded?
			boolean ok = true;
			if (exclude != null) {			
				for(Time ex : exclude) {
					if (ex.equals(mark)) {
						ok = false;
						break;
					}
				}
			}
			// over the count?
			cnt++;
			if (count>0 && cnt>count) {
				return null;
			}			
			// in the window?
			if (mark.isBeforeOrEqualTo(windowStart)) {
				ok = false;
			}
			// yeh!
			if (ok) {
				return mark;
			}
			
			// step forward
			// done late, so we get the event itself included
			mark = mark.plus(new Dt(interval, freq), timezone);
		}
		// no more
		return null;
	}

	public String getRrule() {
		return rrule;
	}

}
