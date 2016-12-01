package com.winterwell.datalog;

import org.junit.Test;

import com.winterwell.utils.time.TUnit;
import com.winterwell.utils.time.Time;

public class RemoteStatTest extends DatalogTestCase {
	
	@Test
	public void testRemoteFetch() {
		IStatReq<Double> req = Stat.getTotal(new Time().minus(TUnit.DAY), new Time(), "Cache_hit");
		req.setServer("egan.sodash.sh");
		Double v = req.get();
		System.out.println(v);
		
		IStatReq<Double> req2 = Stat.getTotal(new Time().minus(TUnit.DAY), new Time(), "Cache_hit");
		req2.setServer("bear.soda.sh");
		Double v2 = req2.get();
		System.out.println(v2);
		
		assert v != v2;
	}

}
