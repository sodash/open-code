package com.winterwell.datalog;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

import java.util.concurrent.ConcurrentHashMap;

import org.junit.Test;

import com.winterwell.maths.stats.distributions.d1.MeanVar1D;
import com.winterwell.maths.timeseries.Datum;
import com.winterwell.maths.timeseries.ListDataStream;
import com.winterwell.utils.Utils;
import com.winterwell.utils.containers.Pair2;
import com.winterwell.utils.io.SqlUtils;
import com.winterwell.utils.time.Period;
import com.winterwell.utils.time.TUnit;
import com.winterwell.utils.time.Time;

public class StatReqSQLTest extends DatalogTestCase {
	
	@Test
	public void testSave() throws InterruptedException {
		DataLogImpl si = (DataLogImpl) DataLog.dflt;
		
		Period p1 = saveData(si, 2.0, "hello");
		Thread.sleep(1000);
		Period p2 = saveData(si, 5.0, "hello");
		
		double first = si.getTotal(p1.first, p1.second, "hello").get();
		double second = si.getTotal(p2.first, p2.second, "hello").get();
		double total = si.getTotal(null, null, "hello").get();
		
		assertEquals(2.0, first, 0.0001);
		assertEquals(5.0, second, 0.0001);
		assertTrue(total >= 7.0);
	}
	
	@Test
	public void testSave2() throws InterruptedException {
		DataLogImpl si = (DataLogImpl) DataLog.dflt;
		String tag = "StatReqSqlTestTag_" + Utils.getRandomString(10);
		
		Period p1 = saveData(si, 2.0, tag);
		Thread.sleep(1000);
		si.count(5.0, tag);
		
		Period p2 = new Period(p1.second.plus(1000, TUnit.MILLISECOND), new Time()); 
		
		// cache contains 5.0
		assertEquals(5.0, si.get(tag).doubleValue());
		
		double first = si.getTotal(p1.first, p1.second, tag).get();
		double second = si.getTotal(p2.first, p2.second, tag).get();
		double total = si.getTotal(null, null, tag).get();
		
		assertEquals(7.0, first, 0.0001);
		assertEquals(5.0, second, 0.0001);
		assertEquals(7.0, total, 0.0001);
	}
	
	@Test
	public void testStream() throws InterruptedException {
		DataLogImpl si = (DataLogImpl) DataLog.dflt;

		Time start = new Time();
		Time end = start.plus(TUnit.SECOND.dt);
		Period p = new Period(start, end);
		
		MeanVar1D mv = new MeanVar1D();
		mv.train1(1.0);
		mv.train1(2.0);
		si.tag2dist.put("bob", mv);
		
		ListDataStream stream = (ListDataStream) si.getData(p.first, p.second, null, null, "bob").get();
		assertEquals(1.5, stream.get(0).x(), 0.0001);
	}
	
	@Test
	public void testSaveHistory() {
		DataLogImpl si = (DataLogImpl) DataLog.dflt;

		String mar = "martag";
		Time t1 = new Time(2012, 3, 25);
		
		String oct = "octtag";		
		Time t2 = new Time(2012, 10, 28);
		
		si.count(t1, 3.0, mar);
		si.set(t2, 10.0, oct);
		
		Utils.sleep(6000);
		si.doSave();
		assertTrue(si.tagTime2count.isEmpty());
		
		double oct_x = si.getTotal(null, null, oct).get();
		double mar_x = si.getTotal(null, null, mar).get();
		
		assertTrue(oct_x >= 10.0);
		assertTrue(mar_x >= 3.0);
		
		si.count(t1, 3.0, mar);
		assertTrue(!si.tagTime2count.isEmpty());
		double mar_x2 = si.getTotal(null, null, mar).get();
		assertTrue(mar_x2 + "", mar_x2 >= 6.0);
	}
	
	/**
	 * Test that data arrive ordered by time, otherwise {@link ListDataStream#add(winterwell.maths.timeseries.Datum)} 
	 * throws IllegalArgumentException when trying to append an old data point #4321, #4988
	 */
	@Test (expected=IllegalArgumentException.class)
	public void testDatastream() {
		DataLogImpl si = (DataLogImpl) DataLog.dflt;

		Time start = new Time(2013, 01, 01);
		Time end = new Time(2013, 01, 20);
		Period period = new Period(start, end);
		
		String tag = "test_order_tag";
		SQLStorage storage = (SQLStorage) si.storage;
		for (int i=1; i<21; i++) {
			Time t = new Time(2013, 01, i);
			si.count(t, 1, tag);
		}
		storage.save(period, si.tag2count, si.tag2dist);
		
		StatReqSQL statreq = new StatReqSQL(KStatReq.DATA, tag, start.minus(1, TUnit.SECOND), end.plus(1, TUnit.SECOND), null, null);
		StatReq.initV(statreq);
		
		// This succeeds
		statreq.run();
		ListDataStream datastream = (ListDataStream) statreq.v;
		assertEquals(20, datastream.size());
		
		for (int i=1; i<21; i++) {
			Time t = new Time(2013, 01, i);
			Datum d = datastream.get(i-1);
			assertEquals(t, d.getTime());
			assertEquals(tag, d.getLabel());
		}
		
		String delete = "delete from " + SQLStorage.TABLE + " where tag = '" + tag + "';";
		SqlUtils.executeUpdate(delete, null);
		
		// This throws the exception
		Datum first = new Datum(start, 1, tag);
		Datum last = new Datum(end, 1, tag);
		StatReq.add(statreq, last);
		StatReq.add(statreq, first);
	}
	
	@Test
	public void testBucket() {
		DataLogImpl si = (DataLogImpl) DataLog.dflt;
		
		String tag = "test_tag";
		String delete = "delete from " + SQLStorage.TABLE + " where tag = '" + tag + "';";
		SqlUtils.executeUpdate(delete, null);
		
		DataLog.count(1, tag);
		Time start = si.start;
		Time at = start.plus(10, TUnit.MINUTE);
		DataLog.count(at, 2, tag);
		
		StatReqSQL statreq = new StatReqSQL(KStatReq.DATA, tag, start.minus(1, TUnit.SECOND), start.plus(15, TUnit.MINUTE), null, null);
		StatReq.initV(statreq);
		statreq.run();
		ListDataStream datastream = (ListDataStream) statreq.v;
		assertEquals(2, datastream.size());
		assertEquals(1, datastream.get(0).x(), 0.001);
		assertEquals(2, datastream.get(1).x(), 0.001);
		
		Time end = start.plus(12, TUnit.MINUTE);
		si.count(end, 5, tag);
		SQLStorage storage = (SQLStorage) si.storage;
		storage.saveHistory(si.tagTime2count);
		si.tagTime2count = new ConcurrentHashMap<Pair2<String,Time>, Double>();
		
		statreq = new StatReqSQL(KStatReq.DATA, tag, start.minus(1, TUnit.SECOND), start.plus(15, TUnit.MINUTE), null, null);
		StatReq.initV(statreq);
		statreq.run();
		datastream = (ListDataStream) statreq.v;
		assertEquals(3, datastream.size());
		assertEquals(1, datastream.get(0).x(), 0.001);
		assertEquals(2, datastream.get(1).x(), 0.001);
		assertEquals(5, datastream.get(2).x(), 0.001);
	}
}
