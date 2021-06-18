package com.goodloop.gcal;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.List;

import org.junit.Test;

import com.google.api.services.calendar.model.CalendarListEntry;
import com.winterwell.utils.Printer;

public class GCalClientTest {

	@Test
	public void testGCalSmokeTest() throws IOException {
		GCalClient gcc = new GCalClient();
		List<CalendarListEntry> list = gcc.getCalendarList();
		// hm - doesn't have all of GL there
		for (CalendarListEntry calendarListEntry : list) {
			Printer.out(calendarListEntry.getId()
					+"	"+calendarListEntry.getSummary()
					+"	"+calendarListEntry.getDescription()
					+"	"+calendarListEntry.getKind()
					+"	"+calendarListEntry.getAccessRole()
					);
		}
	}

}
