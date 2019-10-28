package com.winterwell.ical;


import static org.junit.Assert.assertEquals;

import org.junit.Test;

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
	public void testSummary() {
		ICalWriter w = new ICalWriter();
		String summary = "\"\"ical, with :special; \",\"characters @J, hello: \\";
		
		String _summary = w.formatText(summary);
		assertEquals("\\\"\\\"ical\\, with :special\\; \\\"\\,\\\"characters @J\\, hello: \\\\", _summary);
	}

}
