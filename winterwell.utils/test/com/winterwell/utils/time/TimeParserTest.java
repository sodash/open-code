package com.winterwell.utils.time;

import org.junit.Test;

public class TimeParserTest {

	@Test
	public void testParsePeriodEndOf() {
		TimeParser tp = new TimeParser();
		tp.setNow(new Time(2020,11,5));
		{
			Time eom = tp.parseExperimental("end of month", null);
			assert Math.abs(eom.diff(new Time(2020,12,1))) < 1000 : eom;
		}
		{
			Time eom = tp.parseExperimental("end-of-month", null);
			assert Math.abs(eom.diff(new Time(2020,12,1))) < 1000 : eom;
		}
		{
			Time eow = tp.parseExperimental("end-of-week", null);
			assert eow.equals(new Time(2020,11,9)) : eow.toISOString();
		}
	}

}
