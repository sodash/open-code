/**
 *
 */
package com.winterwell.utils.containers;

import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import junit.framework.Assert;

import org.junit.Test;

import com.winterwell.utils.Key;
import com.winterwell.utils.Utils;

import com.winterwell.datalog.Stat;
import com.winterwell.utils.threads.SafeExecutor;
import com.winterwell.utils.time.TUnit;
import com.winterwell.utils.time.Time;

/**
 * @author Joe Halliwell <joe@winterwell.com>
 * 
 */
public class CacheTest {

	// @Test // not actually a test
	public void howDoSoftReferenceBehave() {
		Key key = new Key("hello");
		SoftReference ref1 = new SoftReference(key);
		HashMap map = new HashMap();
		map.put(ref1, "world");
		SoftReference ref2 = new SoftReference(key);
		Object v = map.get(ref2);
		assert ref1 != ref2;
		assert ref1.equals(ref2);
		assert v.equals("world") : v;
	}

	/**
	 * Test exists to document that dicking with a cache during a
	 * preRemovalCheck causes overflow
	 */
	@Test
	public void testCustomCache() {
		int cacheSize = 50;
		Cache<String, String> c = new Cache<String, String>(cacheSize);
		// {
		// @Override
		// protected boolean preRemovalCheck(String key, String value) {
		// this.put(key + "b", value);
		// return true;
		// }
		// };
		for (int j = 0; j < 10; j++) {
			for (int i = 0; i < 200; i++) {
				c.put("key " + i, "val " + i);
			}
		}
		Assert.assertEquals(cacheSize, c.size());
		Assert.assertEquals(null, c.get("key 0"));
	}

	@Test
	public void testEviction() {
		int cacheSize = 50;
		Cache<String, String> c = new Cache<String, String>(cacheSize);
		for (int j = 0; j < 100; j++) {
			for (int i = 0; i < 200; i++) {
				c.put("key " + i, "val " + i);
			}
		}
		Assert.assertEquals(cacheSize, c.size());
		Assert.assertEquals(null, c.get("key 0"));
	}

	// @Test // SLOW
	public void testOverload() {
		// this should eat ~20mb, but sit at that level
		Cache<String, String> c = new Cache<String, String>(1000);
		Time end = new Time().plus(TUnit.DAY);
		Time beep = new Time().plus(TUnit.MINUTE);
		while (new Time().isBefore(end)) {
			String k = Utils.getRandomString(100);
			String v = Utils.getRandomString(10000);
			c.put(k, v);
			Time now = new Time();
			if (now.isAfter(beep)) {
				beep = now.plus(TUnit.MINUTE);
				System.out.println("	Free: "
						+ Runtime.getRuntime().freeMemory());
			}
		}
	}

	@Test
	public void testEvictionMultiThreaded() throws InterruptedException {
		int cacheSize = 50;
		final Cache<String, String> c = new Cache<String, String>(cacheSize);
		// c.setStats("");
		// c.setRateCounters(new RateCounter(TUnit.SECOND.dt), new
		// RateCounter(TUnit.SECOND.dt));
		SafeExecutor ex = new SafeExecutor(Executors.newFixedThreadPool(100));
		for (int j = 0; j < 1000; j++) {
			ex.submit(new Runnable() {
				@Override
				public void run() {
					Random r = new Random();
					for (int i = 0; i < 1000; i++) {
						int ki = r.nextInt(1000);
						c.get("key " + ki);
						c.put("key " + ki, "val " + i);
					}
				}
			});
		}
		ex.shutdown();
		ex.awaitTermination(TUnit.HOUR.getMillisecs(), TimeUnit.MILLISECONDS);
		System.out.println("Hits:\t" + Stat.get("Cache_hit")); // c.getHitCounter());
		System.out.println("Misses:\t" + Stat.get("Cache_miss")); // c.getMissCounter());
		Assert.assertEquals(cacheSize, c.size());
	}

}
