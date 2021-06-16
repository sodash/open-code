package com.winterwell.ical;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;

import com.winterwell.utils.time.Time;

public class ICalEventTest {

	@Test
	public void testGetRepeats() {
		String ical = "BEGIN:VEVENT\n"
				+ "DTSTART;TZID=Europe/London:20210302T103000\n"
				+ "DTEND;TZID=Europe/London:20210302T113000\n"
				+ "RRULE:FREQ=WEEKLY;BYDAY=TU\n"
				+ "EXDATE;TZID=Europe/London:20210406T103000\n"
				+ "DTSTAMP:20210615T134508Z\n"
				+ "ORGANIZER;CN=am@good-loop.com:mailto:am@good-loop.com\n"
				+ "UID:43q6ms332vmtvobgff_R20210302T103000@google.com\n"
				+ "ATTENDEE;CUTYPE=INDIVIDUAL;ROLE=REQ-PARTICIPANT;PARTSTAT=ACCEPTED;CN=dan\n"
				+ " @good-loop.com;X-NUM-GUESTS=0:mailto:dan@good-loop.com\n"
				+ "ATTENDEE;CUTYPE=INDIVIDUAL;ROLE=REQ-PARTICIPANT;PARTSTAT=ACCEPTED;CN=am@go\n"
				+ " od-loop.com;X-NUM-GUESTS=0:mailto:am@good-loop.com\n"
				+ "ATTENDEE;CUTYPE=INDIVIDUAL;ROLE=REQ-PARTICIPANT;PARTSTAT=ACCEPTED;CN=ry@g\n"
				+ " ood-loop.com;X-NUM-GUESTS=0:mailto:ry@good-loop.com\n"
				+ "ATTENDEE;CUTYPE=INDIVIDUAL;ROLE=REQ-PARTICIPANT;PARTSTAT=DECLINED;CN=dan\n"
				+ " .winters@gmail.com;X-NUM-GUESTS=0:mailto:dan.winters@gmail.com\n"
				+ "CREATED:20210211T171146Z\n"
				+ "DESCRIPTION:https://docs.google.com/presentation/d/1xIqZ_FubERa_Gxxo\n"
				+ " 2taBxosqQ8W3dGc/edit#slide=id.gbe18bd1b_0_22\\n\\nThis event has a vid\n"
				+ " eo call.\\nJoin: https://meet.google.com/yrg-dpxa-vra\\n(GB) +44 20 3910 5158\n"
				+ "  PIN: 489139953#\\nView more phone numbers: https://tel.meet/yrg-dpxa-vra?pi\n"
				+ " n=13761982&hs=7\n"
				+ "LAST-MODIFIED:20210608T232901Z\n"
				+ "LOCATION:\n"
				+ "SEQUENCE:0\n"
				+ "STATUS:CONFIRMED\n"
				+ "SUMMARY:Explore Squad Weekly Check-in\n"
				+ "TRANSP:OPAQUE\n"
				+ "END:VEVENT";
		ICalReader ir = new ICalReader(ical);
		ICalEvent e = ir.getEvents().get(0);
		assert e.timezone != null;
		assert e.start.equals(new Time(2021, 3,2,10,30,0)) : e.start;
		Time j1 = new Time(2021,6,1);
		Time j2 = new Time(2021,7,1);
		List<ICalEvent> rs = e.getRepeats(j1, j2);
		
		ICalEvent e2 = rs.get(0);
		System.out.println(e2);
		
		assert e2.start.equals(new Time(2021, 3,2,9,30,0)) : e.start;
	}

}
