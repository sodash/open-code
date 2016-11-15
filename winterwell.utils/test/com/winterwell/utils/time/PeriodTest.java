package com.winterwell.utils.time;

import org.junit.Test;

import winterwell.utils.time.Period;
import winterwell.utils.time.Time;

public class PeriodTest {

	@Test
	public void testToString() {
		// shortish gap
		Period p1 = new Period(new Time(2015,1,1,9,0,0), new Time(2015,1,2,15,0,0));
		String p1s = p1.toString();
		assert p1s.equals("1 Jan 2015 09:00 to 2 Jan 2015 15:00") : p1s;
		
		// longer gap
		Period p2 = new Period(new Time(2015,1,1,9,0,0), new Time(2015,2,2,15,0,0));
		String p2s = p2.toString();
		assert p2s.equals("1 Jan 2015 09:00 to 2 Feb 2015 15:00") : p2s;
		
		// clean days
		Period p3 = new Period(new Time(2015,1,1), new Time(2015,2,2));
		String p3s = p3.toString();
		assert p3s.equals("1 Jan 2015 to 2 Feb 2015") : p2s;		

	}
}
