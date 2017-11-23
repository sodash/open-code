package com.winterwell.datalog;

import java.util.Set;

import org.junit.Test;

import com.winterwell.utils.Utils;
import com.winterwell.utils.time.TUnit;

/**
 * Test in memory behaviour of DataLog
 * @author daniel
 *
 */
public class InMemoryTest {

	@Test
	public void testNewStat() {
		String tag = "test_"+Utils.getRandomString(10);
		DataLog.count(1, tag);		
		Rate rate = DataLog.get(tag);
		Set<String> live = DataLog.getLive();
		assert live.contains(tag);
		assert rate.isLessThan(new Rate(100, TUnit.MINUTE)) : rate;
		// The short-term behaviour is not precisely defined
		//  -- but I expect it to return a rate of 1-per-bucket
	}
	
}
