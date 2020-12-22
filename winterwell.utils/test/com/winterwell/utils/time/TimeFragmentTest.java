package com.winterwell.utils.time;

import static org.junit.Assert.*;

import java.util.Calendar;

import org.junit.Test;

public class TimeFragmentTest {

	@Test
	public void testEG() {
		TimeFragment tf = new TimeFragment();
		tf.put(Calendar.YEAR, 2009);
		tf.setMonth(11);
		Time t0 = tf.getTime();
		assert t0==null;
		tf.put(Calendar.DAY_OF_MONTH, 18);
		Time t = tf.getTime();
		assert t.equals(new Time(2009,11,18)) : t;
	}

}
