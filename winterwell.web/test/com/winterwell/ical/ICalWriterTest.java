package com.winterwell.ical;


import static org.junit.Assert.assertEquals;

import java.io.File;

import org.junit.Test;

import com.winterwell.utils.io.FileUtils;
import com.winterwell.utils.time.Time;

public class ICalWriterTest {

	@Test
	public void testFormat() {
		ICalWriter w = new ICalWriter();
//		t	      For example, the following represents January 19,
////	      1998, at 0700 UTC:
////
////	       19980119T070000Z)
		Time t = new Time(1998, 1, 19, 7, 0, 0);
		String s = w.format(t);
		assert s.equals("19980119T070000Z") : s;
	}


	@Test
	public void testFormatTime_avoidBST() {
		// 5pm UTC
		Time t = new Time(2021, 5, 11, 17, 0, 0);
		String iso = t.toISOString();
		assert iso.equals("2021-05-11T17:00:00Z");
		String s = ICalWriter.format(t);
		assert s.equals("20210511T170000Z") : s;
	}

	
	

	@Test
	public void testOneEvent() {
		ICalWriter w = new ICalWriter();
		ICalEvent e = new ICalEvent(new Time(2021,5,11,17,0,0), new Time(2021,5,11,18,0,0), 
				"SEAD charity AGM");
		e.description = "Join: https://meet.google.com/apb-uoiu-dmo\n\n"
				+ "Or dial: ‪(GB) +44 20 3956 3652‬ PIN: ‪456 049 743‬#";
		e.location="https://meet.google.com/apb-uoiu-dmo";
		w.addEvent(e);
		System.out.println(w);
		File f = new File(FileUtils.getUserDirectory(), "Downloads/myevent.ics");
		System.out.println(f);
//		f.getParentFile().mkdirs();
		FileUtils.write(f, w.toString());
		System.out.println(e.getGoogleCalendarLink());
	}
	
	
	@Test
	public void testSummary() {
		ICalWriter w = new ICalWriter();
		String summary = "\"\"ical, with :special; \",\"characters @J, hello: \\";
		
		String _summary = w.formatText(summary);
		assertEquals("\\\"\\\"ical\\, with :special\\; \\\"\\,\\\"characters @J\\, hello: \\\\", _summary);
	}

}
