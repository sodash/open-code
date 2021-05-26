
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
	public void testMeetUrl() throws ParseException {
	String se = "BEGIN:VEVENT\n"
			+ "DTSTART:20210526T094500Z\n"
			+ "DTEND:20210526T100000Z\n"
			+ "DTSTAMP:20210526T094800Z\n"
			+ "ORGANIZER;CN=georgia@good-loop.com:mailto:georgia@good-loop.com\n"
			+ "UID:6hh1t3s1tlo7ho9itrlqpu96ag@google.com\n"
			+ "ATTENDEE;CUTYPE=INDIVIDUAL;ROLE=REQ-PARTICIPANT;PARTSTAT=ACCEPTED;CN=daniel\n"
			+ " @good-loop.com;X-NUM-GUESTS=0:mailto:daniel@good-loop.com\n"
			+ "ATTENDEE;CUTYPE=INDIVIDUAL;ROLE=REQ-PARTICIPANT;PARTSTAT=ACCEPTED;CN=georgi\n"
			+ " a@good-loop.com;X-NUM-GUESTS=0:mailto:georgia@good-loop.com\n"
			+ "ATTENDEE;CUTYPE=INDIVIDUAL;ROLE=REQ-PARTICIPANT;PARTSTAT=ACCEPTED;CN=daniel\n"
			+ " .winterstein@gmail.com;X-NUM-GUESTS=0:mailto:daniel.winterstein@gmail.com\n"
			+ "CREATED:20210521T142436Z\n"
			+ "DESCRIPTION:This event has a video call.\\nJoin: https://meet.google.com/ruf\n"
			+ " -sfez-gmz\\n(GB) +44 20 3956 2248 PIN: 203833576#\\nView more phone numbers: \n"
			+ " https://tel.meet/ruf-sfez-gmz?pin=3820063801776&hs=7\n"
			+ "LAST-MODIFIED:20210522T082402Z\n"
			+ "LOCATION:\n"
			+ "SEQUENCE:0\n"
			+ "STATUS:CONFIRMED\n"
			+ "SUMMARY:Daniel / Georgia\n"
			+ "TRANSP:OPAQUE\n"
			+ "END:VEVENT";
	ICalReader r = new ICalReader("");
	ICalEvent e = r.parseEvent(se);
	assert e.description.contains("https://meet.google.com/ruf-sfez-gmz") : e.description;
}
	@Test
	public void testUID() throws ParseException {
		String se = "BEGIN:VEVENT\n"
				+ "DTSTART;TZID=Europe/London:20210525T150000\n"
				+ "DTEND;TZID=Europe/London:20210525T160000\n"
				+ "RRULE:FREQ=MONTHLY;BYDAY=-1TU\n"
				+ "DTSTAMP:20210525T124107Z\n"
				+ "ORGANIZER;CN=\"Good-Loop: General inc out-of-office\":mailto:92v2m458khm50ic0\n"
				+ " 3rj3uu95f4@group.calendar.google.com\n"
				+ "UID:5bv1aflj1glpq7rcu7553v8v2q@google.com\n"
				+ "ATTENDEE;CUTYPE=INDIVIDUAL;ROLE=REQ-PARTICIPANT;PARTSTAT=ACCEPTED;CN=daniel\n"
				+ " @good-loop.com;X-NUM-GUESTS=0:mailto:daniel@good-loop.com\n"
				+ "ATTENDEE;CUTYPE=INDIVIDUAL;ROLE=REQ-PARTICIPANT;PARTSTAT=NEEDS-ACTION;CN=ed\n"
				+ " inburgh.team;X-NUM-GUESTS=0:mailto:edinburgh.team@good-loop.com\n"
				+ "CREATED:20210524T085132Z\n"
				+ "DESCRIPTION:This event has a video call.\\nJoin: https://meet.google.com/zjv\n"
				+ " -spep-iwt\\n(GB) +44 20 3937 4227 PIN: 613451175#\\nView more phone numbers: \n"
				+ " https://tel.meet/zjv-spep-iwt?pin=5780544693823&hs=7\n"
				+ "LAST-MODIFIED:20210524T093439Z\n"
				+ "LOCATION:\n"
				+ "SEQUENCE:0\n"
				+ "STATUS:CONFIRMED\n"
				+ "SUMMARY:Release Freeze! - Submit all code & docs for QA. If it was part of \n"
				+ " the sprint - submit it\\, then email Dan W and Dan A. Thank you!\n"
				+ "TRANSP:OPAQUE\n"
				+ "END:VEVENT";
		ICalReader r = new ICalReader("");
		ICalEvent e = r.parseEvent(se);
		assert e.uid.equals("5bv1aflj1glpq7rcu7553v8v2q@google.com") : e.uid;
	}

	@Test
	public void testTimeZoneBST() throws ParseException {
		ICalReader r = new ICalReader("");
		String se = "BEGIN:VEVENT\n"
				+ "DTSTART;TZID=Europe/London:20210525T120000\n"
				+ "DTEND;TZID=Europe/London:20210525T130000\n"
				+ "DTSTAMP:20210525T101441Z\n"
				+ "ORGANIZER;CN=daniel@good-loop.com:mailto:daniel@good-loop.com\n"
				+ "UID:325i2q6mg67j5fasrha1aemc8k@google.com\n"
				+ "ATTENDEE;CUTYPE=INDIVIDUAL;ROLE=REQ-PARTICIPANT;PARTSTAT=ACCEPTED;CN=daniel\n"
				+ " @good-loop.com;X-NUM-GUESTS=0:mailto:daniel@good-loop.com\n"
				+ "ATTENDEE;CUTYPE=INDIVIDUAL;ROLE=REQ-PARTICIPANT;PARTSTAT=NEEDS-ACTION;CN=ev\n"
				+ " e@good-loop.com;X-NUM-GUESTS=0:mailto:eve@good-loop.com\n"
				+ "RECURRENCE-ID;TZID=Europe/London:20210525T120000\n"
				+ "CREATED:20210221T204003Z\n"
				+ "DESCRIPTION:<a href=\"https://trello.com/b/TQTgJQw5/good-loop-design-board\">\n"
				+ " https://trello.com/b/TQTgJQw5/good-loop-design-board</a>\\n\\nThis event has \n"
				+ " a video call.\\nJoin: https://meet.google.com/qbj-wymz-fmq\\n(GB) +44 20 3957\n"
				+ "  1932 PIN: 868867985#\\nView more phone numbers: https://tel.meet/qbj-wymz-f\n"
				+ " mq?pin=2889186823036&hs=7\n"
				+ "LAST-MODIFIED:20210511T135440Z\n"
				+ "LOCATION:\n"
				+ "SEQUENCE:0\n"
				+ "STATUS:CONFIRMED\n"
				+ "SUMMARY:Eve <> Dan - How does the week look?\n"
				+ "TRANSP:OPAQUE\n"
				+ "END:VEVENT";
		ICalEvent e = r.parseEvent(se);
		assert e.start.equals(new Time(2021,05,25,11,0,0)) : e.start;
		Time s = new Time(2021,05,25), end = new Time(2021,05,26);
	}

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
