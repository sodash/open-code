package com.winterwell.ical;


import java.text.Format;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.time.FastDateFormat;

import com.winterwell.utils.StrUtils;
import com.winterwell.utils.Utils;
import com.winterwell.utils.log.KErrorPolicy;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.time.Time;

/**
 * TODO Crude & simple ICal reader (surprisingly a google, albeit a quick one, failed to find a decent jar).
 * 
 * See http://www.kanzaki.com/docs/ical/
 * 
 * @author daniel
 * @testedby ICalReaderTest
 */
public class ICalReader {

	private static final String LOGTAG = "ICalReader";
	private String ical;

	public ICalReader(String ical) {
		this.ical = ical;
	}
	
	public String getCalendarName() {
		Pattern p = Pattern.compile("^X-WR-CALNAME(;VALUE=TEXT)?:(.+)$", Pattern.MULTILINE);
		Matcher m = p.matcher(ical);
		if ( ! m.find()) return null;
		return m.group(2).trim();
	}
	
	KErrorPolicy errorPolicy = KErrorPolicy.REPORT;
	
	/**
	 * NB repeating events are not unravelled -- they appear once here
	 * @return
	 */
	public List<ICalEvent> getEvents() {
		Pattern p = Pattern.compile("BEGIN:VEVENT.+?END:VEVENT", Pattern.DOTALL);
		Matcher m = p.matcher(ical);
		List<ICalEvent> list = new ArrayList();
		while(m.find()) {				
			String se = m.group();

			// strip out alarms
			Pattern pAlarm = Pattern.compile("BEGIN:VALARM.+?END:VALARM", Pattern.DOTALL);
			String se2 = pAlarm.matcher(se).replaceAll("");
			se = se2;
			
			try {
				ICalEvent e = parseEvent(se);
				list.add(e);
			} catch(Exception ex) {
				switch(errorPolicy) {
				case REPORT:
					Log.e("ical", ex+" from "+se);
				case IGNORE: case RETURN_NULL:
					continue;
				default:
					throw Utils.runtime(ex);				
				}				
			}
		}
		return list;		
	}

	static Pattern pkey = Pattern.compile("^([A-Z\\-]+);?([A-Z]+=[^:]*)?:(.*)");
	
	ICalEvent parseEvent(String se) throws ParseException {		
		String[] lines = StrUtils.splitLines(se);
		String key= null; // summary can be multi-line
		ICalEvent e = new ICalEvent();	
		e.raw = se;		
		for (String line : lines) {
			if (Utils.isBlank(line)) {
				Log.d("ical", "Odd blank line in "+StrUtils.compactWhitespace(se));
				continue;
			}
			key = parseEvent2_line(e, line, key);
		}	
		if (e.isRepeating()) {
			e.repeat.setSince(e.start);
		}
		return e;
	}
	
	String parseEvent2_line(ICalEvent e, String line, String key) throws ParseException {
		String value = line;
		String[] k = StrUtils.find(pkey, line);
		if (k!=null) {
			if (k.length < 3) {	
				Log.e(LOGTAG, "weird ical line: "+line);
				return key;
			}
			key = k[1];
			value = k[3];
		}
		value = value.trim();
		// no blank entries
		if (value.isEmpty()) {
			return key;
		}
		switch(key) {
		case "DTSTAMP":
			// How does this differ from created??
			if (e.created==null) {
				e.created = parseTime(value, k[2]);
			}
			break;
		case "DTSTART":						
			e.start = parseTime(value, k[2]);
			break;
		case "DTEND":
			e.end = parseTime(value, k[2]);
			break;
		case "SUMMARY":
			e.summary = e.summary==null? value : e.summary+" "+value;
			break;
		case "LOCATION":
			e.location = value;
			break;
		case "UID":
			e.uid = value;
			break;
		case "CREATED":
			e.created = parseTime(value, k[2]);
			break;
		case "RRULE":
			e.repeat = new Repeat(value);
			break;
		case "EXDATE":
			// TODO what if this is given before the rrule?
			String[] values = value.split(",");
			for (String v : values) {
				if (Utils.isBlank(v)) continue;
				Time exdate = parseTime(v, k[2]);
				e.repeat.addExclude(exdate);				
			}
			break;
		}
		return key;
	}

	static Format sdfNoTimeZone = format("yyyyMMdd'T'HHmmss");
	static Format sdfDate = format("yyyyMMdd");
	static Pattern pkkv = Pattern.compile("([A-Z]+)=([^;:]+)");
	
	static Time parseTime(String value, String keyValueBumpf) throws ParseException {
		Format tempsdf = null;
		if ( ! Utils.isBlank(keyValueBumpf)) {
			tempsdf = cloneFormat(sdfNoTimeZone);
			if (keyValueBumpf.contains("TZID=")) {				
				Matcher m = pkkv.matcher(keyValueBumpf);
				m.find();
				String tz = m.group(2);
				TimeZone zone = TimeZone.getTimeZone(tz);
				tempsdf = setTimeZone(tempsdf, zone);				
			} else if (keyValueBumpf.equals("VALUE=DATE")) {
				tempsdf = cloneFormat(sdfDate);	
			}
		} else {
			if (value.length() == 8) {				
				// just a date, no time
				tempsdf = cloneFormat(sdfDate);	
			} else {
				// time and date
				tempsdf = cloneFormat(ICalWriter.sdf);
			}
		}				
		// NB FDF does not implmenet Format.parseObject?!
		FastDateFormat f = (FastDateFormat) tempsdf;
		Date parse = f.parse(value); 
		return new Time(parse);
	}

	/**
	 * {@link FastDateFormat} is immutable, so make a new one if the TZ changes
	 * @param tempsdf
	 * @param zone
	 * @return format
	 */
	private static Format setTimeZone(Format tempsdf, TimeZone zone) {
		FastDateFormat f = (FastDateFormat) tempsdf;
		TimeZone tz = f.getTimeZone();
		if (zone.equals(tz)) {
			return f;
		}
		FastDateFormat f2 = FastDateFormat.getInstance(f.getPattern(), zone);
		return f2;
	}

	private static Format cloneFormat(Format f) {
		assert f instanceof FastDateFormat; // simpledateformat would need a clone
		return f;
	}

	private static Format format(String dateFormat) {
		// allow for eg SImpleDateFormat if we want to switch back

		// NOTE: Format.parse and Format.format
		// are not thread safe... hence the apache FDF class
		// (http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4228335)

		return FastDateFormat.getInstance(dateFormat);
	}

	public void setErrorPolicy(KErrorPolicy errorPolicy) {
		this.errorPolicy = errorPolicy;
	}
	
}
