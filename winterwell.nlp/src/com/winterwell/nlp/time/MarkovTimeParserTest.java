package com.winterwell.nlp.time;

import static org.junit.Assert.assertEquals;

import java.text.ParseException;
import java.util.Calendar;

import org.junit.Test;

import com.winterwell.utils.time.TimeFragment;

public class MarkovTimeParserTest {

	@Test
	public void testToCanonical() {
		MarkovTimeParser mtp = new MarkovTimeParser();
		String s = mtp.toCanonical("Monday 17th June");
		assertEquals("mon 17th jun", s);
	}
	
	@Test
	public void testParse() throws ParseException {		
		MarkovTimeParser mtp = new MarkovTimeParser();
		TimeFragment tf = mtp.parse("Monday 7th");
		assert ! tf.isDaySpecific();
		assert tf.getValues().containsKey(Calendar.DAY_OF_WEEK);
		assert tf.getValues().containsKey(Calendar.DAY_OF_MONTH);
		Integer dow = (Integer) tf.getValues().get(Calendar.DAY_OF_WEEK);
		assert dow == Calendar.MONDAY : dow+" "+Calendar.MONDAY+" "+tf.getValues();
		Integer dom = (Integer) tf.getValues().get(Calendar.DAY_OF_MONTH);
		assert dom == 7;
	}

	@Test
	public void testGetObs2Value() throws ParseException {		
		MarkovTimeParser mtp = new MarkovTimeParser();
		Integer m = mtp.getObs2Value("mon");
		assert m == Calendar.MONDAY : m;
	}

	@Test
	public void testParseFullDate() throws ParseException {		
		MarkovTimeParser mtp = new MarkovTimeParser();
		TimeFragment tf = mtp.parse("Weds 22-4-15");
		assert tf.isDaySpecific();
		assert tf.getValues().containsKey(Calendar.DAY_OF_WEEK);
		assert tf.getValues().containsKey(Calendar.DAY_OF_MONTH);
		Integer dow = (Integer) tf.getValues().get(Calendar.DAY_OF_WEEK);
		assert dow == Calendar.WEDNESDAY : dow+" "+tf.getValues();
		Integer dom = (Integer) tf.getValues().get(Calendar.DAY_OF_MONTH);
		assert dom == Calendar.APRIL;
		Integer year = (Integer) tf.getValues().get(Calendar.YEAR);
		assert year == 2015 : year;
	}
}
