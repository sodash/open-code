package com.winterwell.datalog;

import static junit.framework.Assert.assertEquals;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.junit.BeforeClass;
import org.junit.Test;

import com.winterwell.es.client.ESHttpClient;
import com.winterwell.es.client.ESHttpResponse;
import com.winterwell.es.client.IESResponse;
import com.winterwell.es.client.SearchResponse;
import com.winterwell.es.client.admin.DeleteIndexRequest;
import com.winterwell.es.client.admin.GetAliasesRequest;
import com.winterwell.es.client.admin.IndicesAdminClient;
import com.winterwell.maths.stats.distributions.d1.ExponentialDistribution1D;
import com.winterwell.maths.stats.distributions.d1.HistogramData;
import com.winterwell.maths.stats.distributions.d1.IDistribution1D;
import com.winterwell.maths.stats.distributions.d1.MeanVar1D;
import com.winterwell.maths.timeseries.IDataStream;
import com.winterwell.maths.timeseries.ListDataStream;
import com.winterwell.maths.timeseries.LogGridInfo;
import com.winterwell.nlp.query.SearchQuery;
import com.winterwell.utils.MathUtils;
import com.winterwell.utils.Printer;
import com.winterwell.utils.Utils;
import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.utils.containers.Containers;
import com.winterwell.utils.threads.IFuture;
import com.winterwell.utils.time.Dt;
import com.winterwell.utils.time.Period;
import com.winterwell.utils.time.TUnit;
import com.winterwell.utils.time.Time;
import com.winterwell.utils.web.SimpleJson;

public class ESStorageTest {

	private static boolean setupFlag;

	@Test
	public void testIndexSwapping() {
		
		DataLogImpl dl = DataLog.getImplementation();
		ESStorage ess = (ESStorage) dl.getStorage();
		Dataspace ds = new Dataspace("swaptest");
		ESHttpClient esjc = ess.client(ds);
		IndicesAdminClient indices = esjc.admin().indices();
		
		// delete previous indices
		IESResponse del = indices.prepareDelete("datalog.swaptest*").get();
		
		// register ds
		boolean yes = ess.registerDataspace(ds);
		assert yes;
		
		// we should have read & write indices		
		String readIndex = ess.readIndexFromDataspace(ds);		
		boolean ier = indices.indexExists(readIndex);
		String writeIndex = ess.writeIndexFromDataspace(ds);
		final Time now = new Time();
		String baseIndex = ess.baseIndexFromDataspace(ds, now);
		boolean iew = indices.indexExists(writeIndex);
		assert ier && iew;
		IESResponse isw = indices.indexSettings(writeIndex).get();
		IESResponse isr = indices.indexSettings(readIndex).get();
				
		// save an event
		String tag = "test_"+Utils.getRandomString(4);
		DataLogEvent event1 = new DataLogEvent(ds, 1, tag, null);
		Period bucketPeriod = dl.getCurrentBucket();
		ess.saveEvent(ds, event1, bucketPeriod);
		Utils.sleep(1100);

		// find it again
		// NB: add an hour to avoid odd issues around bucketing seen in testing
		SearchResponse sr = ess.getData2(event1, bucketPeriod.getStart(), bucketPeriod.getEnd().plus(TUnit.HOUR), true);
		List<Map> hits = sr.getHits();
		assert ! hits.isEmpty();
		
		// re-register is a no-op
		boolean noop = ess.registerDataspace(ds);
		assert ! noop;
				
		// progress a month
		Time nextMon = now.plus(TUnit.MONTH);
		boolean op = ess.registerDataspace2(ds, nextMon);
		assert op;
		String newBaseIndex = ess.baseIndexFromDataspace(ds, nextMon);
		
		// TODO the underlying base for write should have moved, and read should have two bases
		IESResponse isw3 = indices.indexSettings(writeIndex).get();
		IESResponse isr3 = indices.indexSettings(readIndex).get();
		IESResponse baseAliases = indices.getAliases(baseIndex).get();
		IESResponse newBaseAliases = indices.getAliases(newBaseIndex).get();
		
		IESResponse readAliases = indices.getAliases(readIndex).get();
		Set<String> ras = GetAliasesRequest.getBaseIndices(readAliases);
		assert ras.contains(newBaseIndex);
		assert ras.contains(baseIndex);
		
		IESResponse writeAliases = indices.getAliases(writeIndex).get();
		Set<String> was = GetAliasesRequest.getBaseIndices(writeAliases);
		assert was.contains(newBaseIndex);
		assert ! was.contains(baseIndex);
		
	}
	

	@BeforeClass
	public static void setup() {
		if ( setupFlag) return;
		setupFlag = true;
		DataLogConfig config = new DataLogConfig();
		
		// 1 second saves!!
		config.interval = new Dt(1, TUnit.SECOND);
		
		config.storageClass = ESStorage.class;
		
		config.noSystemStats = true;
		
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
	public void testSaveOverlappingEvents() throws InterruptedException, ExecutionException {				
		Time start = new Time().minus(TUnit.MINUTE);
		ESStorage storage = (ESStorage) DataLog.getImplementation().getStorage();						
		storage.init(new DataLogConfig());
		String gby = Utils.getRandomString(4);
		Dataspace dataspace = new Dataspace("testoverlapspace");
		
		ESHttpClient esjc = storage.client(dataspace);
		// clean out old
		DeleteIndexRequest del = esjc.admin().indices().prepareDelete(storage.writeIndexFromDataspace(dataspace));
		del.get();
		Utils.sleep(100);

		DataLogEvent event = new DataLogEvent(dataspace, 
				gby,
				1, new String[] {"testoverlap1"}, new ArrayMap(
				"w", 11, // diff num in both
				"pub", "egpub1", // diff in both
				"cid", "oxfam1", // only in event 1
				"tracker", "foo1@trk", // diff props in both
				"blah1prop", "blah1"
				));
		
		storage.registerDataspace(dataspace);
		storage.registerEventType(dataspace, event.getEventType0());
		
		Period period = new Period(start, new Time());
		// save it!
		Future<ESHttpResponse> res = storage.saveEvent(dataspace, event, period);
		ESHttpResponse r = res.get();
		r.check();
		// pause for ES
		Utils.sleep(1500);

		// save again -- different event, same group		
		DataLogEvent event2 = new DataLogEvent(dataspace, 
				gby,
				1, new String[] {"testoverlap2"}, new ArrayMap(
				"w", 12,
				"pub", "egpub2",
				"tracker", "bar2@trk",
				"campaign", "Test2", // only in event 2
				"blah2prop", "blah2"
				));
		assert event.getId().equals(event2.getId());
		Printer.out("ID: "+event.getId());
		Future<ESHttpResponse> res2 = storage.saveEvent(dataspace, event2, period);
		ESHttpResponse r2 = res2.get();
		r2.check();
		
		// pause for ES
		Utils.sleep(1500);
		
		// get		
		String index = storage.readIndexFromDataspace(dataspace);		
		String eid = event.getId(); //dataspace+"/"+gby;
		Map<String, Object> evt = esjc.get(index, storage.ESTYPE, eid);
		assert evt != null;
		Printer.out("---------");
		Printer.out(evt);
		Printer.out("---------");
		assert Containers.same(Containers.asList(evt.get("evt")), Arrays.asList("testoverlap1","testoverlap2"));
		DataLogEvent dle = DataLogEvent.fromESHit(dataspace, evt);
		assert dle!=null;
		Object p = dle.getProp("tracker");
		assert p.equals("bar2@trk") : del;
		assert dle.getProp("blah1prop").equals("blah1");
		assert dle.getProp("blah2prop").equals("blah2");
		assert dle.getProp("cid").equals("oxfam1");
		assert dle.getProp("campaign").equals("Test2");
		
		Time end = new Time();
		StatReq<IDataStream> data1 = storage.getData("testoverlap1", start, end, null, null);
		StatReq<IDataStream> data2 = storage.getData("testoverlap2", start, end, null, null);
		
		List<String> breakdown = Arrays.asList("evt");
		SearchResponse events = storage.doSearchEvents(dataspace, 10, 10, start, end, new SearchQuery(""), breakdown);
		Map aggs = events.getAggregations();
		Printer.out("---------");
		Printer.out(aggs); // the event gets counted under both tags :)
		Printer.out("---------");
		Number c1 = SimpleJson.get(aggs, "by_evt", "buckets", 0, "doc_count");
		Number c2 = SimpleJson.get(aggs, "by_evt", "buckets", 1, "doc_count");
		assert c1.intValue() == 1 : aggs;
		assert c2.intValue() == 1 : aggs;
		
		List<Map> egs = events.getHits();
		Printer.out(egs);
		System.out.println(egs);
	}
	
	
	@Test
	public void testGetData() throws InterruptedException, ExecutionException {		
		ESStorage storage = (ESStorage) DataLog.getImplementation().getStorage();
		
		// make sure some stats are in the system
		DataLogImpl dli = (DataLogImpl) DataLog.getImplementation();
		dli.statSystemStats();
		dli.flush();
		Utils.sleep(1200);
		
		Time end = new Time().plus(TUnit.MINUTE);
		Time s = end.minus(TUnit.DAY);
		StatReq<Double> total = storage.getTotal("mem_used", s, end);
		assert total.get() != null;
		assert total.get() > 0;
		
		StatReq<IDataStream> data = storage.getData("mem_used", s, end, null, null);
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
		Dataspace ds = new Dataspace("testdataspace");
		DataLogEvent event = new DataLogEvent(ds, 3.5, "testevent", new ArrayMap(
				"publisher", "egpub",
				"tracker", "szkpogoeegglvcszwtao@trk"
				));
		Period period = new Period(new Time().minus(TUnit.MINUTE), new Time());
		Future<ESHttpResponse> res = storage.saveEvent(
				ds, event, period);
		ESHttpResponse r = res.get();
		r.check();
		// pause for ES
		Utils.sleep(2000);
		// get it
		Time start = new Time().minus(TUnit.DAY.dt);
		Time end = new Time();
		DataLogEvent event2 = new DataLogEvent(ds, 0, "testevent", new ArrayMap(
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
		setup();
		Time s = new Time().minus(TUnit.MINUTE);
		String salt = Utils.getRandomString(4);
		String stag = "testtotal"+salt;
		for(int i=0; i<10; i++) {
			Utils.sleep(100);
			DataLog.count(1, stag);
		}
		DataLog.flush();
		Utils.sleep(1200);
		Time now = new Time().plus(5, TUnit.MINUTE);
		IDataLogReq<Double> total = DataLog.getTotal(s, now, stag);
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
