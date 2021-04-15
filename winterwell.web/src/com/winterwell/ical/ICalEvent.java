package com.winterwell.ical;


import java.util.ArrayList;
import java.util.List;

import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.utils.time.Dt;
import com.winterwell.utils.time.Period;
import com.winterwell.utils.time.Time;
import com.winterwell.utils.web.WebUtils2;

public class ICalEvent {

	public Time start;
	/**
	 * Can be null -- interpreted as one day.
	 * See https://stackoverflow.com/questions/15295887/does-an-ical-file-have-to-have-an-enddate
	 */
	public Time end;
	public String summary;
	public String description;
	public String uid;
	public Time created;
	public String location;
	public String raw;
	public Repeat repeat;
	public ICalEvent parent;
	
	public ICalEvent() {
	}
	
	/**
	 * 
	 * @param start
	 * @param end Can be null
	 * @param summary
	 */
	public ICalEvent(Time start, Time end, String summary) {
		this.start = start;
		this.end = end;
		this.summary = summary;
		if (start==null) throw new NullPointerException("null start not allowed for ICalEvent");
	}

	@Override
	public String toString() {
		String s = ICalWriter.format(start);
		return "BEGIN:VEVENT\r\n"
			+"DTSTART:"+s+"\r\n" // FIXME What is the format???
			+(end!=null? "DTEND:"+ICalWriter.format(end)+"\r\n" : "")				
			// TODO UID and others!
			+(summary==null? "" : "SUMMARY:"+ICalWriter.formatText(summary)+"\r\n")
			+(description==null? "" : "DESCRIPTION:"+ICalWriter.formatText(description)+"\r\n")
			+(location==null? "" : "LOCATION:"+ICalWriter.formatText(location)+"\r\n")
			+(repeat==null? "" : "RRULE:"+repeat.rrule+"\r\n")
			+"END:VEVENT\r\n";
	}
	
	public String getGoogleCalendarLink() {
		return WebUtils2.addQueryParameters("https://www.google.com/calendar/render?action=TEMPLATE",
				new ArrayMap("text",summary,
						"dates", ICalWriter.format(start)+"/"+ICalWriter.format(end),
						"details",description,
						"location",location
//						"sf",true,
//						"output","xml"
						));
	}
	
	public boolean isRepeating() {
		return repeat!=null;
	}

	/**
	 * @param start
	 * @param end
	 * @return All repeats within start and end, if it is repeating.
	 * If not -- return null.
	 */
	public List<ICalEvent> getRepeats(Time rstart, Time rend) {
		if (repeat==null) return null;
		List<Time> repeatPeriods = repeat.getRepeats(rstart, rend);
		List<ICalEvent> repeatEvents = new ArrayList();
		Dt dt = end==null? null : start.dt(end);
		for (Time t : repeatPeriods) {			
			Time repEnd = dt==null? null : t.plus(dt);
			ICalEvent e2 = new ICalEvent(t, repEnd, summary);
			e2.location = location;
			e2.parent = this;
			repeatEvents.add(e2);
		}
		return repeatEvents;
	}

	public void setRepeat(Repeat repeater) {
		// TODO modify raw
		this.repeat = repeater;
		if (repeat.since==null) repeat.setSince(start);
		assert repeat.since.equals(start) : repeat+" vs "+this;
	}

	public Period getPeriod() {
		return new Period(start, end);
	}
}
