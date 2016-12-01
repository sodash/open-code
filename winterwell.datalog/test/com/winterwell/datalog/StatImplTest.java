package com.winterwell.datalog;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.junit.Test;

import com.winterwell.maths.timeseries.Datum;
import com.winterwell.maths.timeseries.ListDataStream;
import com.winterwell.utils.Utils;
import com.winterwell.utils.time.Time;

import com.winterwell.utils.threads.IFuture;

/**
 * @tested {@link StatImpl}
 * @author daniel
 *
 */
public class StatImplTest extends DatalogTestCase {
	
	@Test
	public void testCount() throws InterruptedException {
		// force an init
		Stat.count(1, "dummy");		
		StatImpl si = (StatImpl) Stat.dflt;
		
		si.count(1, "hello", "world");
		Rate c = si.get("hello", "world");
		assert c.x >= 1 : c;
		
		// multi-thread
		Stat.set(0, "test", "foo");
		ExecutorService exe = Executors.newFixedThreadPool(20);
		final AtomicInteger done = new AtomicInteger();
		for(int i=0; i<1000; i++) {
			exe.submit(new Runnable() {				
				@Override
				public void run() {
					Stat.count(1, "test", "foo");
					Rate fc = Stat.get("test", "foo");
//					System.err.println(fc+" "+done);
					done.incrementAndGet();
					assert fc.x > 0 ;
				}
			});	
		}
		exe.shutdown();
		exe.awaitTermination(1000, TimeUnit.MILLISECONDS);
		assert done.get() == 1000 : done;
		Rate foos = Stat.get("test", "foo");
		assert foos.get() == 1000 : foos;
		Stat.flush();
		
		String[] tagBits = {"test", "foo"};
		IFuture<Double> fd = Stat.getTotal(null, null, tagBits);
		double total = fd.get();
		System.out.println(total);
		assert total >= 1000;		
	}
	
	@Test
	public void testCache() {
		StatImpl si = (StatImpl) Stat.dflt;
		si.count(3, "tag1");
		Rate r = si.get("tag1");
		assert r.get() == 3.0 : r.get();
	}
	
	@Test
	public void testGetTotal() throws IOException {
		StatImpl si = (StatImpl) Stat.dflt;
		Time s = new Time();
		for(int i=0; i<10; i++) {
			Utils.sleep(300);
			si.count(1, "testTotal");
		}

		IStatReq<Double> total = si.getTotal(s, new Time(), "testTotal");
		Double v = total.get();
		assert v == 10 : v;
	}
	
	@Test
	public void testCountHistoricData() {
		StatImpl si = (StatImpl) Stat.dflt;
		Time at = new Time(2012, 7, 18);
		si.count(at, 7.0, "hello", "world");
		si.count(at, 3.0, "hello");
		
		Time start = new Time(2012, 6, 1);
		Time end = new Time(2012, 8, 1);
		List<Datum> hellos = si.currentHistoric("hello", start, end);
		List<Datum> helloworlds = si.currentHistoric("hello/world", start, end);
		
		Assert.assertEquals(10.0, hellos.get(0).x(), 0.0001);
		Assert.assertEquals(7.0, helloworlds.get(0).x(), 0.0001);
	}
	
	@Test
	public void testCurrentHistoricData() {
		StatImpl si = (StatImpl) Stat.dflt;
		Time at = new Time(2012, 8, 18);
		si.count(at, 7.0, "hello2", "world2");
				
		Time start = new Time(2012, 7, 1);
		Time end = new Time(2012, 9, 1);
		List<Datum> list = si.currentHistoric("hello2/world2", start, end);
		Assert.assertTrue(list.size() > 0);
		Assert.assertEquals(7.0, list.get(0).x(), 0.0001);
		
		si.set(at, 3.0, "hello2", "world2");
		list = si.currentHistoric("hello2/world2", start, end);
		Assert.assertEquals(0, list.size());
	}
	
	@Test
	public void testSetHierarchy() {
		{
			String topTag = "testTag" + Utils.getRandomString(10);
			String childTag = "testTag2";
			String[] tagBits = {topTag, childTag};
			StatImpl si = (StatImpl) Stat.dflt;
			si.count(5, tagBits);
			
			si.set(10, topTag);
			si.count(5, topTag);
			
			ListDataStream l = (ListDataStream) si.getData(null, null, null, null, topTag).get();
			double[] resultArray = {15,0};
			assert isDataStreamValid(l, resultArray);
			
			l = (ListDataStream) si.getData(null, null, null, null, tagBits).get();
			resultArray = new double[]{5,0};
			assert isDataStreamValid(l, resultArray);
		}
		{
			String topTag = "testTag" + Utils.getRandomString(10);
			String childTag = "testTag2";
			String[] tagBits = {topTag, childTag};
			StatImpl si = (StatImpl) Stat.dflt;
			si.count(5, tagBits);
			
			si.set(10, tagBits);
			si.count(5, tagBits);
			
			ListDataStream l = (ListDataStream) si.getData(null, null, null, null, topTag).get();
			double[] resultArray = new double[]{10,0}; // Is this correct, or should it be 15?
			assert isDataStreamValid(l, resultArray);
			
			l = (ListDataStream) si.getData(null, null, null, null, tagBits).get();
			resultArray = new double[]{15,0};
			assert isDataStreamValid(l, resultArray);
		}
	}
}
