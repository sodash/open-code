package com.winterwell.datalog;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

import java.util.Iterator;

import org.junit.Test;

import com.winterwell.maths.timeseries.Datum;
import com.winterwell.maths.timeseries.ListDataStream;
import com.winterwell.utils.time.Period;

public class StatReqCSVTest extends DatalogTestCase {
	
	{
		initCSV();
	}
	
	@Test
	public void testSave() throws InterruptedException {
		StatImpl si = (StatImpl) DataLog.dflt;
		Period p1 = saveData(si, 2.0, "hello", "world");
		Thread.sleep(2000);
		Period p2 = saveData(si, 5.0, "hello", "world");
		
		String[] tagBits = {"hello", "world"};
		double first = si.getTotal(p1.first, p1.second, tagBits).get();
		double second = si.getTotal(p2.first, p2.second, tagBits).get();
		double total = si.getTotal(null, null, tagBits).get();
		
		assertEquals(2.0, first);
//		assertEquals(5.0, second);
		assertTrue(total >= 7.0);
	}
	
	@Test
	public void testGetData() throws InterruptedException {		
		StatImpl si = (StatImpl) DataLog.dflt;
		Period p1 = saveData(si, 1.0, "tag1");
		Thread.sleep(1000);
		Period p2 = saveData(si, 3.0, "tag1");
		Thread.sleep(1000);
		Period p3 = saveData(si, 5.0, "other");
		
		// Test period
		ListDataStream stream = (ListDataStream) si.getData(p1.first, p2.second, null, null, "tag1").get();
		assertTrue(! stream.isEmpty());
		Iterator<Datum> itr = stream.iterator();
		assertEquals(1.0, itr.next().x());
		assertEquals(3.0, itr.next().x());
	}
}
