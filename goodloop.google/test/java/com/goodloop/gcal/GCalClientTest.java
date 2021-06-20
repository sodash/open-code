package com.goodloop.gcal;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.model.Calendar;
import com.google.api.services.calendar.model.CalendarListEntry;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventAttendee;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.EventReminder;
import com.winterwell.utils.Printer;
import com.winterwell.utils.Utils;
import com.winterwell.utils.time.TUnit;
import com.winterwell.utils.time.Time;
import com.winterwell.utils.time.TimeUtils;

public class GCalClientTest {

	@Test
	public void testGCalSmokeTest() throws IOException {
		GCalClient gcc = new GCalClient();
		List<CalendarListEntry> list = gcc.getCalendarList();
		// hm - doesn't have all of GL there
		for (CalendarListEntry calendarListEntry : list) {
			Printer.out(calendarListEntry.getId() // just their email!
					+"	"+calendarListEntry.getSummary()
					+"	"+calendarListEntry.getDescription()
//					+"	"+calendarListEntry.getKind() boring
					+"	"+calendarListEntry.getAccessRole()
					);
		}
	}

	@Test
	public void testMake1to1() throws IOException {
		GCalClient gcc = new GCalClient();
		Calendar dw = gcc.getCalendar("daniel@good-loop.com");
		System.out.println(dw);
		Calendar dw2 = gcc.getCalendar("daniel.winterstein@gmail.com");
		System.out.println(dw2);		
//		Calendar da = gcc.getCalendar("daniel.appel.winterwell@gmail.com");
//		System.out.println(da);
		
		Event event = new Event()
			    .setSummary("Test 2 GCalClient #ChatRoundabout "+Utils.getNonce())
			    .setDescription("A lovely event")
			    ;

			DateTime startDateTime = new DateTime(new Time().toISOString());
			EventDateTime start = new EventDateTime()
			    .setDateTime(startDateTime)
			    .setTimeZone("GMT");
			event.setStart(start);

			DateTime endDateTime = new DateTime(new Time().plus(TUnit.HOUR).toISOString());
			EventDateTime end = new EventDateTime()
			    .setDateTime(endDateTime)
			    .setTimeZone("GMT");
			event.setEnd(end);

//			String[] recurrence = new String[] {"RRULE:FREQ=DAILY;COUNT=2"};
//			event.setRecurrence(Arrays.asList(recurrence));

			EventAttendee[] attendees = new EventAttendee[] {
			    new EventAttendee().setEmail("daniel@good-loop.com")
			    	.setResponseStatus("tentative"),
			    new EventAttendee().setEmail("daniel.winterstein@gmail.com")
			    	.setResponseStatus("tentative"),
			};
			event.setAttendees(Arrays.asList(attendees));

			EventReminder[] reminderOverrides = new EventReminder[] {
			    new EventReminder().setMethod("email").setMinutes(10),
			    new EventReminder().setMethod("popup").setMinutes(1),
			};
			Event.Reminders reminders = new Event.Reminders()
			    .setUseDefault(false)
			    .setOverrides(Arrays.asList(reminderOverrides));
			event.setReminders(reminders);

			String calendarId = dw.getId(); // "primary";
			Event event2 = gcc.addEvent(calendarId, event, false, true);
			
			Printer.out(event2.toPrettyString());
		}
}
