package com.winterwell.datalog;

import static junit.framework.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.winterwell.maths.stats.distributions.d1.IDistribution1D;
import com.winterwell.maths.stats.distributions.d1.MeanVar1D;
import com.winterwell.maths.timeseries.ListDataStream;
import com.winterwell.utils.time.Period;
import com.winterwell.utils.time.TUnit;
import com.winterwell.utils.time.Time;

public class CSVStorageTest extends DatalogTestCase {

	{
		initCSV();
	}
	
	@Test
	public void testIO() {
		Time start = new Time();
		Time end = start.plus(10, TUnit.SECOND);
		Period p = new Period(start, end);

		Map<String, Double> tag2count = new HashMap<String, Double>();
		tag2count.put("hello", 4.0);

		Map<String, IDistribution1D> tag2mean = new HashMap<>();
		MeanVar1D mv = new MeanVar1D();
		mv.train1(1.0);
		mv.train1(2.0);
		tag2mean.put("world", mv);
		
		CSVStorage storage = new CSVStorage();
		storage.save(p, tag2count, tag2mean);
		assertEquals(4.0, storage.getTotal("hello", p.first, p.second).get());
		ListDataStream stream = (ListDataStream) storage.getData("world", p.first, p.second, null, null).get();
		assertEquals(1.5, stream.get(0).x());
	}
}
