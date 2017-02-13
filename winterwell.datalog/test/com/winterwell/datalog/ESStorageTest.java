package com.winterwell.datalog;

import static junit.framework.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.junit.Test;

import com.google.common.util.concurrent.ListenableFuture;
import com.winterwell.es.client.ESHttpResponse;
import com.winterwell.maths.stats.distributions.d1.MeanVar1D;
import com.winterwell.maths.timeseries.ListDataStream;
import com.winterwell.utils.Utils;
import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.utils.time.Period;
import com.winterwell.utils.time.TUnit;
import com.winterwell.utils.time.Time;

public class ESStorageTest extends DatalogTestCase {

	{
		initCSV();
	}

	
	@Test
	public void testSaveEvent() throws InterruptedException, ExecutionException {		
		ESStorage storage = new ESStorage();
		storage.init(new StatConfig());
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
		ListenableFuture<ESHttpResponse> res = storage.saveEvent(
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
		double total = storage.getEventTotal("testdataspace", start, end, event2);
		assert total > 0;
	}

	
//	@Test
	public void testIO() {
		Time start = new Time();
		Time end = start.plus(10, TUnit.SECOND);
		Period p = new Period(start, end);

		Map<String, Double> tag2count = new HashMap<String, Double>();
		tag2count.put("hello", 4.0);

		Map<String, MeanVar1D> tag2mean = new HashMap<String, MeanVar1D>();
		MeanVar1D mv = new MeanVar1D();
		mv.train1(1.0);
		mv.train1(2.0);
		tag2mean.put("world", mv);
		
		ESStorage storage = new ESStorage();
		storage.init(new StatConfig());
		storage.save(p, tag2count, tag2mean);
		assertEquals(4.0, storage.getTotal("hello", p.first, p.second).get());
		ListDataStream stream = (ListDataStream) storage.getData("world", p.first, p.second, null, null).get();
		assertEquals(1.5, stream.get(0).x());
	}
}
