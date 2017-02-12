package com.winterwell.datalog;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.junit.Test;

import com.winterwell.maths.stats.distributions.d1.MeanVar1D;
import com.winterwell.maths.timeseries.ListDataStream;
import com.winterwell.utils.Utils;
import com.winterwell.utils.containers.Pair2;
import com.winterwell.utils.time.Period;
import com.winterwell.utils.time.TUnit;
import com.winterwell.utils.time.Time;

/**
 * @tested {@link SQLStorage}
 * @author Agis
 *
 */
public class SQLStorageTest extends DatalogTestCase {

	static String sqlNightmare = "he's\";--DROP; DROP TABLES; DROP TABLE*;;'\n\r\t\"'";
	

	/**
	 * Tests that stat handles past results correctly
	 */
	@Test
	public void testStatPastResults(){
		
		Time t5 = new Time();
		Time t4 = new Time().minus(1 , TUnit.MINUTE);
		Time t3 = new Time().minus(2 , TUnit.MINUTE);
		Time t2 = new Time().minus(3 , TUnit.MINUTE);
		Time t1 = new Time().minus(4 , TUnit.MINUTE);
		
		Time t0 = new Time().minus(10 , TUnit.DAY);
		
		{
			// Test 1, 5 results, out of order.
			String testTag= "statTestTag" + Utils.getRandomString(10);
			DataLog.count(t1, 2, testTag);
			DataLog.count(t3, 3, testTag);
			DataLog.count(t5, 5, testTag);
			DataLog.count(t2, 7, testTag);
			DataLog.count(t4, 11, testTag);

			DataLog.flush();

			ListDataStream l = (ListDataStream) DataLog.getData(new Time().minus(TUnit.HOUR), new Time(), null, null, testTag).get();
			System.out.println(l);
			// If these are coming back in order, the time will be higher each time, and the 
			// results will match the array below. Note: The list returns a final zero. 
			double [] resultArray = {2.0, 7.0, 3.0, 11.0, 5.0, 0.0};
			assert isDataStreamValid(l, resultArray);
		}				
		{
			// Test 2, 7 results, out of order, and some at same time.
			String testTag = "statTestTag" + Utils.getRandomString(10);
			System.out.println("stat test tag "+testTag);
			DataLog.count(t1, 1, testTag);
			DataLog.count(t3, 2, testTag);
			DataLog.count(t5, 4, testTag);
			
			DataLog.flush();
			
			DataLog.count(t5, 8, testTag);
			DataLog.count(t3, 16, testTag);
			DataLog.count(t1, 32, testTag);
			DataLog.count(t2, 64, testTag);
			
			DataLog.flush();			
			
			ListDataStream l = (ListDataStream) DataLog.getData(new Time().minus(TUnit.HOUR), new Time(), null, null, testTag).get();
			System.out.println(l);
			
			// If these are coming back in order, the time will be higher each time, and the 
			// results will match the array below. Note: The list returns a final zero. 
			double [] resultArray = {33.0, 64.0, 18.0, 12.0, 0.0};
			assert isDataStreamValid(l, resultArray);
		}
		
		{	// Same test, multiple runs
			String testTag = "statTestTagRepeat" + Utils.getRandomString(10);
			DataLog.set(t1, 1, testTag);
			DataLog.set(t2, 2, testTag);
			DataLog.set(t3, 3, testTag);
			DataLog.set(t4, 4, testTag);
			DataLog.set(t5, 5, testTag);			

			DataLog.flush();
			ListDataStream l = (ListDataStream) DataLog.getData(new Time().minus(TUnit.HOUR), new Time(), null, null, testTag).get();
			System.out.println("12345:	"+ l);
			DataLog.flush();
			
			DataLog.count(t1, 1, testTag);
			DataLog.count(t2, 2, testTag);
			DataLog.count(t3, 3, testTag);
			DataLog.count(t4, 4, testTag);
			DataLog.count(t5, 5, testTag);
			
			DataLog.flush();
			ListDataStream l2 = (ListDataStream) DataLog.getData(new Time().minus(TUnit.HOUR), new Time(), null, null, testTag).get();
			System.out.println("12345x2:	"+ l2);
			DataLog.flush();
			
			// If these are coming back in order, the time will be higher each time, and the 
			// results will match the array below. Note: The list returns a final zero. 
			double [] resultArray = {2,4,6,8,10,0};
			assert isDataStreamValid(l2, resultArray);
		}

	}
	
	@Test
	public void testSetHistoric() {
		Time t2 = new Time().minus(1, TUnit.MINUTE);
		Time t1 = new Time().minus(2, TUnit.MINUTE);
		
		String testTag = "statTestTagHistoric" + Utils.getRandomString(10);
		DataLog.count(t1, 1, testTag);
		DataLog.count(t2, 2, testTag);
		DataLog.flush();
		
		DataLog.set(t1, 2, testTag);
		DataLog.set(t2, 3, testTag);
		
		ListDataStream l = (ListDataStream) DataLog.getData(new Time().minus(5, TUnit.MINUTE), new Time(), null, null, testTag).get();
		System.out.println(l);
		double [] resultArray = {2,3,0};
		assert isDataStreamValid(l, resultArray);
	}

	/**
	 * Intentionally perverse test to try to catch edge cases.
	 */
	@Test
	public void testSetHistoricEvil() {
		Time t2 = new Time().minus(1, TUnit.MINUTE);
		Time t1 = new Time().minus(2, TUnit.MINUTE);
		String testTag = "statTestTagHistoric" + Utils.getRandomString(10);
		{
			
			DataLog.count(t1, 1, testTag);
			DataLog.count(t2, 2, testTag);
			DataLog.flush();
		
			DataLog.set(t1, 2, testTag);
			DataLog.set(t2, 3, testTag);
			DataLog.count(t1, 10, testTag);
			DataLog.count(t2, 10, testTag);
		
			DataLog.flush();
		
			ListDataStream l = (ListDataStream) DataLog.getData(new Time().minus(3, TUnit.MINUTE), new Time(), null, null, testTag).get();
			double [] resultArray = {12,13,0};
			assert isDataStreamValid(l, resultArray);
	
		}
		{
			String badTestTag = sqlNightmare + Utils.getRandomString(10);
			DataLog.set(t1, 2, badTestTag);
			DataLog.set(t2, 3, badTestTag);
			DataLog.count(t1, 10, badTestTag);
			DataLog.count(t2, 10, badTestTag);
			DataLog.set(t1, 4, badTestTag);
			DataLog.set(t2, 5, badTestTag);
			DataLog.flush();
			ListDataStream l = (ListDataStream) DataLog.getData(new Time().minus(3, TUnit.MINUTE), new Time(), null, null, badTestTag).get();
			double [] resultArray = {4,5,0};
			assert isDataStreamValid(l, resultArray);
		}
		{
			DataLog.set(t1, 2, testTag);
			DataLog.flush();
			DataLog.set(t2, 3, testTag);
			DataLog.flush();
			DataLog.count(t1, 10, testTag);
			DataLog.flush();
			DataLog.count(t2, 10, testTag);
			DataLog.flush();
			DataLog.set(t1, 4, testTag);
			DataLog.flush();
			DataLog.set(t2, 5, testTag);
			DataLog.flush();
			
			ListDataStream l = (ListDataStream) DataLog.getData(new Time().minus(3, TUnit.MINUTE), new Time(), null, null, testTag).get();
			double [] resultArray = {4,5,0};
			assert isDataStreamValid(l, resultArray);
		}
		
	}

	/**
	 * Even more perverse test to try to catch edge cases.
	 */
	@Test
	public void testSetHistoricEvil2(){
	{
		String badTestTag = sqlNightmare + Utils.getRandomString(10);
		Time t5 = new Time();
		Time t4 = new Time().minus(1 , TUnit.MINUTE);
		Time t3 = new Time().minus(2 , TUnit.MINUTE);
		Time t2 = new Time().minus(3 , TUnit.MINUTE);
		Time t1 = new Time().minus(4 , TUnit.MINUTE);
		
		Time t0 = new Time().minus(10 , TUnit.DAY);
		DataLog.flush();
		DataLog.count(t0, 100, badTestTag);
		DataLog.flush();
		DataLog.count(t1, 2, badTestTag);
		DataLog.flush();
		DataLog.count(t3, 3, badTestTag);
		DataLog.flush();
		DataLog.count(t5, 5, badTestTag);
		DataLog.flush();
		DataLog.count(t2, 7, badTestTag);
		DataLog.flush();
		DataLog.count(t4, 11, badTestTag);	
		
		DataLog.count(t0, 1, badTestTag, "hello");
		DataLog.flush();
		DataLog.set(t1, 0, badTestTag);
		DataLog.flush();
		DataLog.count(t2, 3, badTestTag);
		DataLog.flush();
		DataLog.count(t3, 4, badTestTag);
		DataLog.flush();
		DataLog.count(t4, 6, badTestTag);
		DataLog.flush();
		DataLog.count(t5, 6, badTestTag);

		ListDataStream l = (ListDataStream) DataLog.getData(new Time().minus(11, TUnit.DAY), new Time(), null, null, badTestTag).get();
		double [] resultArray = {101,0,10,7,17,11,0};
		assert isDataStreamValid(l, resultArray);
	}
	}

	@Test
	public void testIO() {
		Time start = new Time();
		Time end = start.plus(TUnit.SECOND.dt);
		Period p = new Period(start, end);

		Map<String, Double> tag2count = new HashMap<String, Double>();
		tag2count.put("hello", 4.0);
		tag2count.put(sqlNightmare, 5.0);

		Map<String, MeanVar1D> tag2mean = new HashMap<String, MeanVar1D>();
		MeanVar1D mv = new MeanVar1D();
		mv.train1(1.0);
		mv.train1(2.0);
		tag2mean.put("world", mv);
		tag2mean.put(sqlNightmare, mv);
		
		SQLStorage storage = new SQLStorage(newStatConfig());
		storage.initStatDB();
		storage.save(p, tag2count, tag2mean);
		
		assertEquals(4.0, storage.selectSum(null, p.first, p.second, null, "hello"));
		Iterator<Object[]> stream = storage.getReader(null, p.first, p.second, null, "world");
		assertTrue(stream.hasNext());
		assertEquals(1.5, stream.next()[2]);
		assertTrue(!stream.hasNext());
	}
	
	@Test
	public void testIOHistory() {
		String jan = "jantag";
		Time t1 = new Time(2012, 01, 13);
		Pair2<String, Time> key1 = new Pair2<String, Time>(jan, t1);
		
		String feb = "febtag";		
		Time t2 = new Time(2012, 02, 25);
		Pair2<String, Time> key2 = new Pair2<String, Time>(feb, t2);
		
		String smarch = "s'marchtag";		
		Time t3 = new Time(2012, 03, 25);
		Pair2<String, Time> key3 = new Pair2<String, Time>(smarch, t3);
		
		ConcurrentMap<Pair2<String, Time>, Double> tag2time2count = new ConcurrentHashMap<Pair2<String,Time>, Double>();
		tag2time2count.put(key1, 1.0);
		tag2time2count.put(key2, 2.0);
		tag2time2count.put(key3, 3.0);

		SQLStorage storage = new SQLStorage(newStatConfig());
		storage.initStatDB();
		storage.saveHistory(tag2time2count);
		
		Time s = new Time(0);
		Time e = new Time();
		assertTrue(storage.selectSum(null, s, e, null, jan) >= 1.0);
		assertTrue(storage.selectSum(null, s, e, null, feb) >= 2.0);
		assertTrue(storage.selectSum(null, s, e, null, smarch) >= 3.0);
	}

	private StatConfig newStatConfig() {
		return new StatConfig();
	}
}
