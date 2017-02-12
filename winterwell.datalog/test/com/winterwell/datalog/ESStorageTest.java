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
		storage.init();
//		{charity: Alzheimers Research UK, 
		// publisher: www.good-loop.com} 
		// tracker:szkpogoeegglvcszwtao@trk 
		// ref:http://www.good-loop.com/live-demo 
		// ip:88.98.205.94 
		// ENDMSG http://www.good-loop.com/live-demo 88.98.205.94
		Map event = new ArrayMap(
				"publisher", "egpub",
				"tracker", "szkpogoeegglvcszwtao@trk"
				);
		ListenableFuture<ESHttpResponse> res = storage.saveEvent(1, "testdataspace", event);
		ESHttpResponse r = res.get();
		r.check();
		
		Time start = new Time().minus(TUnit.DAY.dt);
		Time end = new Time();
		double total = storage.getEventTotal(start, end, "testdataspace");
	}

	
	@Test
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
		storage.init();
		storage.save(p, tag2count, tag2mean);
		assertEquals(4.0, storage.getTotal("hello", p.first, p.second).get());
		ListDataStream stream = (ListDataStream) storage.getData("world", p.first, p.second, null, null).get();
		assertEquals(1.5, stream.get(0).x());
	}
}
