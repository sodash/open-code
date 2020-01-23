package com.winterwell.ical;

import java.util.List;

import org.junit.Test;

import com.winterwell.gson.Gson;
import com.winterwell.utils.Printer;
import com.winterwell.utils.time.Time;

public class RepeatTest {


	@Test
	public void testGson() {
		Gson gson = new Gson();
		Repeat repeat = new Repeat("FREQ=MONTHLY;INTERVAL=3;");
		String json = gson.toJson(repeat);
		Repeat repeat2 = gson.fromJson(json);
		Printer.out(repeat2);
	}
	
	@Test
	public void testGetNext() {
		{	// every quarter
			Repeat repeat = new Repeat("FREQ=MONTHLY;INTERVAL=3;");
			repeat.setSince(new Time(2000,1,1));
			Time next = repeat.getNext(new Time(2000,1,1));
			assert next.equals(new Time(2000, 4, 1)) : next;
		}
		{	// until gone
			Repeat repeat = new Repeat("FREQ=MONTHLY;UNTIL=19990101;");
			repeat.setSince(new Time(2000,1,1));
			Time next = repeat.getNext(new Time(2000,1,1));
			assert next == null;
		}
		{	// until fine
			Repeat repeat = new Repeat("FREQ=MONTHLY;UNTIL=20010101;");
			repeat.setSince(new Time(2000,1,1));
			Time next = repeat.getNext(new Time(2000,1,1));
			assert next.equals(new Time(2000,2,1)) : next;
		}
		{	// since in
			Repeat repeat = new Repeat("FREQ=MONTHLY");
			repeat.setSince(new Time(2000,5,3));
			Time next = repeat.getNext(new Time(2000,1,1));
			assert next.equals(new Time(2000,5,3)) : next;
		}
	}

	
	@Test
	public void testRepeats() {
		{	// every quarter
			Repeat repeat = new Repeat("FREQ=MONTHLY;INTERVAL=3;");
			repeat.setSince(new Time(2000,1,1));
			List<Time> qs = repeat.getRepeats(new Time(2000,1,1), new Time(2000,12,30));
			assert qs.get(0).equals(new Time(2000, 1, 1)) : qs;
			assert qs.size() == 4 : qs;
		}
	}

	@Test
	public void testRepeatsWithExclude() {
		{	// every quarter
			Repeat repeat = new Repeat("FREQ=MONTHLY;INTERVAL=3;");
			repeat.setSince(new Time(2000,1,1));
			repeat.addExclude(new Time(2000,4,1));
			List<Time> qs = repeat.getRepeats(new Time(2000,1,1), new Time(2000,12,30));
			assert qs.get(0).equals(new Time(2000, 1, 1)) : qs;
			assert qs.get(1).equals(new Time(2000, 7, 1)) : qs;
			assert qs.size() == 3 : qs;
		}
	}

}
