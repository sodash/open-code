package com.winterwell.datalog;

import static junit.framework.Assert.assertEquals;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.junit.BeforeClass;
import org.junit.Test;

import com.winterwell.es.client.ESHttpResponse;
import com.winterwell.maths.chart.LogGridInfo;
import com.winterwell.maths.stats.distributions.d1.ExponentialDistribution1D;
import com.winterwell.maths.stats.distributions.d1.HistogramData;
import com.winterwell.maths.stats.distributions.d1.IDistribution1D;
import com.winterwell.maths.stats.distributions.d1.MeanVar1D;
import com.winterwell.maths.timeseries.IDataStream;
import com.winterwell.maths.timeseries.ListDataStream;
import com.winterwell.utils.MathUtils;
import com.winterwell.utils.Utils;
import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.utils.threads.IFuture;
import com.winterwell.utils.time.Dt;
import com.winterwell.utils.time.Period;
import com.winterwell.utils.time.TUnit;
import com.winterwell.utils.time.Time;

public class ESStorageTest {

	@BeforeClass
	public static void setup() {
		DataLogConfig config = new DataLogConfig();
		config.interval = new Dt(1, TUnit.SECOND);
		config.storageClass = ESStorage.class;
		DataLog.dflt = new DataLogImpl(config);
		DataLog.init(config);
	}

	
	
//	@Test
	public void testHistogram() throws InterruptedException, ExecutionException {
		Time start = new Time();
		DataLogConfig config = DataLog.getImplementation().getConfig();
		String tag = "distrotest_"+Utils.getRandomString(4);
		// count upto ~1 billion in 30 buckets
		config.setTagHandler(tag, () -> new HistogramData(new LogGridInfo(30)));
		
		DataLogImpl dl = (DataLogImpl) DataLog.getImplementation();
		ExponentialDistribution1D exp = new ExponentialDistribution1D(0.1);
		for(int i=0; i<100; i++) {
			double x = exp.sample();			
			dl.mean(x, tag);
		}
		
		MeanRate mr = dl.getMean(tag);
		IDistribution1D histo = mr.x;
		assert histo instanceof HistogramData : histo;
		assert MathUtils.approx(histo.getMean(), 10) : histo;
		
		// save
		dl.flush();
		Utils.sleep(1000);
		Time end = new Time();
		
		IFuture<MeanRate> m = dl.getMean(start, end, tag);
		MeanRate mr2 = m.get();
		IDistribution1D histo2 = mr2.x;
		assert histo2 instanceof HistogramData : histo2;
		assert MathUtils.approx(histo2.getMean(), 10) : histo2;
	}

	
	@Test
	public void testGetData() throws InterruptedException, ExecutionException {		
		ESStorage storage = (ESStorage) DataLog.getImplementation().getStorage();
		
		StatReq<Double> total = storage.getTotal("mem_used", new Time().minus(TUnit.DAY), new Time());
		assert total.get() != null;
		
		StatReq<IDataStream> data = storage.getData("mem_used", new Time().minus(TUnit.DAY), new Time(), null, null);
		assert ! data.get().isEmpty();
	}
	
	@Test
	public void testSaveEvent() throws InterruptedException, ExecutionException {		
		ESStorage storage = new ESStorage();
		storage.init(new DataLogConfig());
//		{charity: Alzheimers Research UK, 
		// publisher: www.good-loop.com} 
		// tracker:szkpogoeegglvcszwtao@trk 
		// ref:http://www.good-loop.com/live-demo 
		// ip:88.98.205.94 
		// ENDMSG http://www.good-loop.com/live-demo 88.98.205.94
		DataLogEvent event = new DataLogEvent("testdataspace", 3.5, "testevent", new ArrayMap(
				"publisher", "egpub",
				"tracker", "szkpogoeegglvcszwtao@trk"
				));
		Period period = new Period(new Time().minus(TUnit.MINUTE), new Time());
		Future<ESHttpResponse> res = storage.saveEvent(
				"testdataspace", event, period);
		ESHttpResponse r = res.get();
		r.check();
		// pause for ES
		Utils.sleep(2000);
		// get it
		Time start = new Time().minus(TUnit.DAY.dt);
		Time end = new Time();
		DataLogEvent event2 = new DataLogEvent("testdataspace", 0, "testevent", new ArrayMap(
				"publisher", "egpub"
				));
		double total = storage.getEventTotal(start, end, event2);
		assert total > 0;
	}


	
	@Test
	public void testTest() {
		DataLog.test();
	}
	
	@Test
	public void testGetTotal() throws IOException {
		Time s = new Time();
		String salt = Utils.getRandomString(4);
		for(int i=0; i<10; i++) {
			Utils.sleep(300);
			DataLog.count(1, "testTotal"+salt);
		}
		DataLog.flush();
		Utils.sleep(10000);
		IDataLogReq<Double> total = DataLog.getTotal(s, new Time(), "testTotal"+salt);
		Double v = total.get();
		assert v == 10 : v;
	}
	
	/**
	 * This is meaningful for the CSV-backed Stat
	 * where a Cache is used for batch operations 
	 * and we have Cache_hit and Cache_miss statistics.
	 * @throws IOException
	 */
	@Test
	public void testGet() throws IOException {
//		assert Stat.saveThread != null;
		Time start = new Time();
		Utils.sleep(1000); // wait for it to start saving
		String salt = Utils.getRandomString(4);
		for(int i=0; i<10; i++) {
			DataLog.count(1, "test", salt);
			Utils.sleep(300);
		}
		Time end = new Time();
		// flush the data
		DataLog.flush();

		for(int i=0; i<5; i++) {
			String[] tagBits = {"test", salt};
			Future<Double> ttl1 = DataLog.getTotal(null, null, tagBits);
			Rate h = DataLog.get("Cache_hit", "Stat");
			Rate m = DataLog.get("Cache_miss", "Stat");
			System.out.println(ttl1+"\thits:"+h+"\tmisses:"+DataLog.get("Cache_miss", "Stat"));
			Utils.sleep(i*10);
		}
	}

	
	
	@Test
	public void testIO() {
		Time start = new Time();
		Time end = start.plus(10, TUnit.SECOND);
		Period p = new Period(start, end);

		String tag1 = "test_hello"+Utils.getRandomString(6);
		String tag2 = "test_world"+Utils.getRandomString(6);
		
		Map<String, Double> tag2count = new HashMap<String, Double>();
		tag2count.put(tag1, 4.0);

		Map<String, IDistribution1D> tag2mean = new HashMap<>();
		MeanVar1D mv = new MeanVar1D();
		mv.train1(1.0);
		mv.train1(2.0);
		tag2mean.put(tag2, mv);
		
		ESStorage storage = new ESStorage();
		storage.init(new DataLogConfig());
		storage.save(p, tag2count, tag2mean);
		Utils.sleep(1500);
		assertEquals(4.0, storage.getTotal(tag1, p.first, p.second).get());
		ListDataStream stream = (ListDataStream) storage.getData(tag2, p.first, p.second, null, null).get();
		assertEquals(1.5, stream.get(0).x());
	}
}
