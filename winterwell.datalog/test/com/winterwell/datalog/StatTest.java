package com.winterwell.datalog;

import java.io.IOException;
import java.util.concurrent.Future;

import org.junit.Test;

import com.winterwell.utils.Utils;
import com.winterwell.utils.time.TUnit;
import com.winterwell.utils.time.Time;

public class StatTest extends DatalogTestCase {

	public StatTest() {
		StatConfig config = new StatConfig();
		config.interval = TUnit.MINUTE.dt;
		Stat.setConfig(config);
	}
	
	@Test
	public void testTest() {
		Stat.test();
	}
	
	@Test
	public void testGetTotal() throws IOException {
		Time s = new Time();
		for(int i=0; i<10; i++) {
			Utils.sleep(300);
			Stat.count(1, "testTotal");
		}
		IStatReq<Double> total = Stat.getTotal(s, new Time(), "testTotal");
		Double v = total.get();
		assert v == 10 : v;
	}
	
	/**
	 * This is meaningful for the CSV-backed Stat
	 * where a Cache is used for batch operations 
	 * and we have Cache_hit and Cache_miss statistics.
	 * @throws IOException
	 */
	@Deprecated
	@Test
	public void testGet() throws IOException {
//		assert Stat.saveThread != null;
		Time start = new Time();
		Utils.sleep(1000); // wait for it to start saving
		String salt = Utils.getRandomString(4);
		for(int i=0; i<10; i++) {
			Stat.count(1, "test", salt);
			Utils.sleep(300);
		}
		Time end = new Time();
		// flush the data
		Stat.flush();

		for(int i=0; i<5; i++) {
			String[] tagBits = {"test", salt};
			Future<Double> ttl1 = Stat.getTotal(null, null, tagBits);
			Rate h = Stat.get("Cache_hit", "Stat");
			System.out.println(ttl1+"\thits:"+h+"\tmisses:"+Stat.get("Cache_miss", "Stat"));
			Utils.sleep(i*10);
		}
		Rate h = Stat.get("Cache_hit", "Stat");
		Rate m = Stat.get("Cache_miss", "Stat");
		assert h.x == 0; // > 2 : h;
		assert m.x == 0; // < h.x : m;
	}

}
