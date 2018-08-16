package com.winterwell.maths.stats.distributions.d1;

import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.winterwell.utils.Printer;
import com.winterwell.utils.containers.Containers;

public class HistogramDataTest {

	@Test
	public void testToJson2() {
		HistogramData hd = new HistogramData(0, 100, 100);
		// lets train a triangle
		for(int i=0; i<100; i++) {
			for(int j=0; j<i; j++) hd.count(i);
		}
		Map<String, Object> jobj = hd.toJson2();
		assert ((Double)jobj.get("min")) == 0;
		assert ((Double)jobj.get("max")) == 100;
		List<Double> counts = Containers.asList(jobj.get("counts"));
		assert counts.size() == 100;
		assert counts.get(0) == 0;
		assert counts.get(10) == 10;
		Printer.out(hd.toJSONString());
	}

}
