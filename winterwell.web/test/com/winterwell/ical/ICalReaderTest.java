
package com.winterwell.ical;

import java.io.File;
import java.text.ParseException;
import java.util.List;

import org.junit.Test;

import com.winterwell.utils.io.FileUtils;
import com.winterwell.utils.log.KErrorPolicy;
import com.winterwell.utils.time.Time;


public class ICalReaderTest {


	@Test
	public void testExDate() throws ParseException {
		ICalReader r = new ICalReader("");
		String se = "BEGIN:VEVENT\n" + 
				"DTSTART;TZID=Europe/London:20171115T110000\n" + 
				"DTEND;TZID=Europe/London:20171115T123000\n" + 
				"RRULE:FREQ=WEEKLY;BYDAY=WE\n" + 
				"EXDATE;TZID=Europe/London:20180103T110000\n" + 
				"EXDATE;TZID=Europe/London:20171227T110000\n" + 
				"EXDATE;TZID=Europe/London:20171206T110000\n" + 
				"DTSTAMP:20180112T150452Z\n" + 
				"ORGANIZER;CN=daniel.winterstein@gmail.com:mailto:daniel.winterstein@gmail.c\n" + 
				" om\n" + 
				"UID:lq4s9poqcfcgd2g473v8k5h794_R20171115T110000@google.com\n" + 
				"ATTENDEE;CUTYPE=INDIVIDUAL;ROLE=REQ-PARTICIPANT;PARTSTAT=NEEDS-ACTION;CN=fw\n" + 
				" b2@hw.ac.uk;X-NUM-GUESTS=0:mailto:fwb2@hw.ac.uk\n" + 
				"ATTENDEE;CUTYPE=INDIVIDUAL;ROLE=REQ-PARTICIPANT;PARTSTAT=ACCEPTED;CN=m.summ\n" + 
				" ers@zonefox.com;X-NUM-GUESTS=0:mailto:m.summers@inquisitivesystems.net\n" + 
				"ATTENDEE;CUTYPE=INDIVIDUAL;ROLE=REQ-PARTICIPANT;PARTSTAT=ACCEPTED;CN=c.leon\n" + 
				" ard@inquisitive-systems.com;X-NUM-GUESTS=0:mailto:c.leonard@zonefox.com\n" + 
				"ATTENDEE;CUTYPE=INDIVIDUAL;ROLE=REQ-PARTICIPANT;PARTSTAT=ACCEPTED;CN=Daniel\n" + 
				"  Winterstein;X-NUM-GUESTS=0:mailto:daniel.winterstein@gmail.com\n" + 
				"ATTENDEE;CUTYPE=INDIVIDUAL;ROLE=REQ-PARTICIPANT;PARTSTAT=ACCEPTED;CN=m.goul\n" + 
				" d@inquisitive-systems.com;X-NUM-GUESTS=0:mailto:m.gould@zonefox.com\n" + 
				"CREATED:20160704T105144Z\n" + 
				"DESCRIPTION:\n" + 
				"LAST-MODIFIED:20171214T095704Z\n" + 
				"LOCATION:\n" + 
				"SEQUENCE:3\n" + 
				"STATUS:CONFIRMED\n" + 
				"SUMMARY:ZoneFox AI weekly meeting\n" + 
				"TRANSP:OPAQUE\n" + 
				"END:VEVENT";
		ICalEvent e = r.parseEvent(se);
		assert e.isRepeating();
		Time s = new Time(2017,12,1), end = new Time(2017,12,31);
		List<ICalEvent> repeats = e.getRepeats(s, end);
		assert ! repeats.isEmpty();
		// 4 weeks, 2 excluded
		assert repeats.size() == 2;
		
		List<ICalEvent> repeats2 = e.getRepeats(new Time(2018,1,1), new Time(2018,2,1));
		// 4 weeks, 0 excluded
		assert repeats2.size() == 4 : repeats2;
	}
	
	
	@Test
	public void testRepeatingRule() throws ParseException {
		ICalReader r = new ICalReader("");
		String se = "BEGIN:VEVENT"
+"\nDTSTART;TZID=Europe/London:20150710T170000"
+"\nDTEND;TZID=Europe/London:20150710T171500"
+"\nRRULE:FREQ=WEEKLY;UNTIL=20160722T160000Z;INTERVAL=6;BYDAY=FR"
+"\nDTSTAMP:20160912T093529Z"
+"\nUID:1vemvj6n6hb3dqpp3msrqsrfn0@google.com"
+"\nCREATED:20140317T171808Z"
+"\nDESCRIPTION:15 minutes show and tell of anything you like."
+"\nLAST-MODIFIED:20160829T194958Z"
+"\nLOCATION:"
+"\nSEQUENCE:3"
+"\nSTATUS:CONFIRMED"
+"\nSUMMARY:Show-n-tell: DA"
+"\nTRANSP:OPAQUE"
+"\nEND:VEVENT";
		ICalEvent e = r.parseEvent(se);
		assert e.isRepeating();
		Time s = new Time(), end = new Time();
		List<ICalEvent> repeats = e.getRepeats(s, end);
		assert repeats.isEmpty();
		
		List<ICalEvent> repeats2 = e.getRepeats(new Time(2014,1,1), new Time(2018,1,1));
		assert ! repeats2.isEmpty();
	}
	
	@Test
	public void testGetCalendarName() {
		File icalf = TestUtils.getTestFile("ical", "https://www.google.com/calendar/ical/92v2m458khm50ic03rj3uu95f4%40group.calendar.google.com/private-6451230e4826d67dad83cafa959a738b/basic.ics");
		String ical = FileUtils.read(icalf);
		ICalReader r = new ICalReader(ical);
		String cn = r.getCalendarName();
		assert ! cn.isEmpty();
		System.out.println(cn);
	}

	@Test
	public void testGetEvents() {
		File icalf = TestUtils.getTestFile("ical", "https://www.google.com/calendar/ical/92v2m458khm50ic03rj3uu95f4%40group.calendar.google.com/private-6451230e4826d67dad83cafa959a738b/basic.ics");
		String ical = FileUtils.read(icalf);
		System.out.println(ical);
		ICalReader r = new ICalReader(ical);
		r.setErrorPolicy(KErrorPolicy.THROW_EXCEPTION);
		List<ICalEvent> es = r.getEvents();
		assert ! es.isEmpty();
		for (ICalEvent iCalEvent : es) {
			System.out.println(iCalEvent);
			assert iCalEvent.start != null : iCalEvent.raw;
		}
	}

}
