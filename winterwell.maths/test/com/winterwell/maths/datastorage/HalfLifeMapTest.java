package com.winterwell.maths.datastorage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;

import org.junit.Test;

import com.winterwell.utils.Printer;
import com.winterwell.utils.StrUtils;
import com.winterwell.utils.Utils;
import com.winterwell.utils.containers.Containers;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.time.StopWatch;
import com.winterwell.utils.web.XStreamUtils;

public class HalfLifeMapTest {


	@Test
	public void testEntrySetSet() {
		HalfLifeMap<String,Double> map = new HalfLifeMap<String, Double>(10);
		map.put("first", 1.0);
		map.put("second", 2.0);
		Set<Entry<String, Double>> set = map.entrySet();
		Entry<String, Double> e = null;;
		Iterator<Entry<String, Double>> itr = set.iterator();
		while (itr.hasNext()) {
			e = itr.next();
			if (e.getKey().equals("first")) break;
		}
		assert e.getValue().equals(1.0);
		
		e.setValue(5.0);
		assert e.getValue().equals(5.0) : map;
		assert map.get("first") == 5.0 : map;
	}


	@Test
	public void testForgetting() {
		String[] sa = "1 2 3 4 5 6 7 8 9 10".split(" ");
		String[] sb = "6 7 8 9 10 11 12 13 14 15".split(" ");
		HalfLifeMap<String,String> index = new HalfLifeMap<String,String>(10);
		// see LOTS of sa		
		for (int i = 0; i < 1000; i++) {
			for (String x : sa) {
				index.put(x, x);
				index.get(x);
			}
		}
		// see some of sb
		for (int i = 0; i < 5; i++) {
			for (String x : sb) {
				index.put(x, x);
				index.get(x);
			}
		}

		// Let's see where we stand
		System.out.println("\nKey-strengths");
		for(String k : index.keySet()) {
			System.out.println("\t"+k+"\t"+index.getCount(k));
		}
		System.out.println("");
		
		assert index.size() == 15 : index;
		assert index.containsKey("2");
		assert index.containsKey("6");
		assert index.containsKey("11");
		// prune: keep sa
		index.prune();
		assert index.size() == 10 : index.size();
		assert index.containsKey("2") : index;
		assert index.containsKey("6");
		assert ! index.containsKey("11") : index;

		// see lots of sb
		for (int i = 0; i < 100; i++) {
			for (String x : sb) {
				index.put(x, x);
				index.get(x);
			}
		}
		
		// prune: keep sb
		index.prune();
		Printer.out(Containers.getList(index.keySet()));
		assert index.size() == 10;
		assert ! index.containsKey("2");
		assert index.containsKey("6");
		assert index.containsKey("11");
		
		// Let's see where we stand now
		System.out.println("\nKey-strengths");
		for(String k : index.keySet()) {
			System.out.println("\t"+k+"\t"+index.getCount(k));
		}
		System.out.println("");
	}
	

	/**
	 * Conclusion: HalLifeMap has little overhead for get, about twice as slow
	 * for put.
	 */
	@Test
	public void testValues() {
		// half-life
		HalfLifeMap map = new HalfLifeMap<>(4);
		Collection vs = map.values();
		assert vs.isEmpty();
		for(int i=0; i<10; i++) {
			map.put(i, i);
		}
		
		Collection vs2 = map.values();
		for (Object object : vs2) {
			assert object != null && object instanceof Integer;
			System.out.println("value: "+object);
		}
		
		assert map.containsKey(9);
		
		map.remove(9);
		
		assert ! map.containsKey(9);
		
		Collection vs3 = map.values();
		for (Object object : vs3) {
			assert object != null && object instanceof Integer;
			System.out.println("value: "+object);
		}
		
	}

	@Test
	public void testGet() {
		HalfLifeMap<String, String> map = new HalfLifeMap<>(1);
		map.put("Fred", "Wilma");

		assert map.get("Fred") == "Wilma";
	}

	@Test
	public void testGetKeyNotPresent() {
		HalfLifeMap<String, String> map = new HalfLifeMap<>(1);
		map.put("Fred", "Wilma");

		assert map.get("Barney") == null;
	}

	@Test
	public void testGetMapEmpty() {
		HalfLifeMap<String, String> map = new HalfLifeMap<>(1);

		assert map.get("Barney") == null;
	}

	@Test
	public void testRemove() {
		HalfLifeMap<String, String> map = new HalfLifeMap<>(1);
		map.put("Fred", "Wilma");

		String retVal = map.remove("Fred");

		assert map.get("Fred") == null;
		assert retVal == "Wilma";
		assert map.size() == 0;
	}

	@Test
	public void testRemoveKeyNotPresent() {
		HalfLifeMap<String, String> map = new HalfLifeMap<>(1);
		map.put("Fred", "Wilma");

		String retVal = map.remove("Barney");

		assert map.get("Barney") == null;
		assert retVal == null;
		assert map.size() == 1;
	}

	@Test
	public void testRemoveEmpty() {
		HalfLifeMap<String, String> map = new HalfLifeMap<>(1);

		String retVal = map.remove("Barney");

		assert map.get("Barney") == null;
		assert retVal == null;
		assert map.size() == 0;
	}

	@Test
	public void testRemoveNotLastItem() {
		HalfLifeMap<String, String> map = new HalfLifeMap<>(1);
		map.put("Fred", "Wilma");
		map.put("Barney", "Betty");

		String retVal = map.remove("Fred");

		assert map.get("Fred") == null;
		assert retVal == "Wilma";
		assert map.size() == 1;
	}

	/**
	 * Conclusion: HalLifeMap has little overhead for get, about twice as slow
	 * for put.
	 */
	@Test
	public void testSpeed() {
		// half-life
		HalfLifeMap map = new HalfLifeMap<>(25000);
		Log.setMinLevel(Level.OFF);
		{
			double perOp1a = speed2_heavyGet(map);
			double perOp1b = speed2_heavyGet(Collections.synchronizedMap(new HashMap()));
			double pc = 100*((perOp1a/perOp1b)-1);
			System.out.println("Mostly Gets: HalfLifeMap "+perOp1a +" vs HashMap "+perOp1b+":\t"+StrUtils.toNSigFigs(pc, 2)+"%");
			System.out.println("Prunes: " + map.getPrunedCount());
			System.out.println("Map size: " + map.size());
			System.out.println("Total count: " + map.getTotalCount());
		}
		{
			double perOp1a = speed2_heavyPut(map);
			double perOp1b = speed2_heavyPut(Collections.synchronizedMap(new HashMap()));
			double pc = 100*((perOp1a/perOp1b)-1);
			System.out.println("Mostly Puts: HalfLifeMap "+perOp1a +" vs HashMap "+perOp1b+":\t"+StrUtils.toNSigFigs(pc, 2)+"%");
			System.out.println("Prunes: " + map.getPrunedCount());
			System.out.println("Map size: " + map.size());
			System.out.println("Total count: " + map.getTotalCount());
		}
		assert map.size() <= map.getIdealSize()*2;
	}
	
	private double speed2_heavyGet(Map map) {
		Utils.PowerLawDistribution dist = new Utils.PowerLawDistribution(0.1);
		StopWatch sw = new StopWatch();
		int ops = 0;
		for(int i=0; i<100000; i++) {
			int key_length = 4;
			map.put(dist.getRandomString(key_length), i);
			ops++;
			for(int j=0; j<1000; j++) {
				map.get(dist.getRandomString(key_length));
				ops++;
			}
		}
		return (1.0*sw.getTime())/ops;
	}

	private double speed2_heavyPut(Map map) {
		Utils.PowerLawDistribution dist = new Utils.PowerLawDistribution(0.1);
		StopWatch sw = new StopWatch();
		int ops = 0;
		for(int i=0; i<100000; i++) {
			int key_length = 4;
			map.get(dist.getRandomString(key_length));
			ops++;
			for(int j=0; j<10; j++) {
				map.put(dist.getRandomString(key_length), j);
				ops++;
			}
		}
		return (1.0*sw.getTime())/ops;
	}

	@Test
	public void testConcurrency() {
		HalfLifeMap map = new HalfLifeMap<>(25000);
		// Log.setMinLevel(Level.OFF);
		Thread[] threads = new Thread[10];
		for (int i = 0; i < 10; i++) {
			threads[i] = new Thread(() -> speed2_heavyPut(map));
			threads[i].start();
		}
		for (Thread t: threads) {
			try {
				t.join();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	@Test
	public void testSerialisation() {
		HalfLifeMap<String, Object> index = new HalfLifeMap<>(10);

		index.put("A", 1);
		index.put("B", 2);
		index.put("C", 3);
		index.get("B");

		String xml = XStreamUtils.serialiseToXml(index);

		assert !xml.toLowerCase().contains("concurrent");

		HalfLifeMap index2 = XStreamUtils.serialiseFromXml(xml);
		assert index2.get("C").equals(3);

		// wrapped
		ArrayList wrap = new ArrayList();
		wrap.add(index);
		String wxml = XStreamUtils.serialiseToXml(index);

		assert !wxml.toLowerCase().contains("concurrent");

	}

	@Test
	public void testToString() {
		HalfLifeMap<String, Object> map = new HalfLifeMap<>(10);

		map.put("A", 1);
		map.put("B", 2);
		String str = map.toString();

		assert str.equals("HalfLifeMap[A,B... 2]");
	}

	@Test
	public void testItemToString() {
		HLEntry<String, Object> entry = new HLEntry<>("A", 3);
		String str = entry.toString();

		assert str.equals("HLEntry [count=1.0, val=3]") : str;
	}

	@Test
	public void testItemClone() {
		HLEntry<String, Double> hle = new HLEntry<>("Fred", 7.3);
		HLEntry<String, Double> hle2 = hle.clone();

		assert hle2.getKey() == "Fred";
		assert hle2.getValue() == 7.3;
		assert hle2.count == hle.count;
	}

	@Test
	public void testClone() {
		HalfLifeMap<String, Double> map = new HalfLifeMap<>(10);
		map.put("Fred",  7.0);
		map.put("Barney", 9.3);
		map.put("Fred", 39.4);
		HalfLifeMap<String, Double> map2 = map.clone();

		assert map2.get("Fred") == 39.4;
		assert map2.get("Barney") == 9.3;
		assert map2.getCount("Fred") == 3;
	}

	@Test
	public void testCloneCreatesNewObjects() {
		HalfLifeMap<String, Double> map = new HalfLifeMap<>(10);
		map.put("Fred",  7.0);
		map.put("Barney", 9.3);
		HalfLifeMap<String, Double> map2 = map.clone();
		map.put("Fred", 39.4);
		map.remove("Barney");

		assert !map.containsKey("Barney");
		assert map2.containsKey("Barney");
		assert map2.get("Fred") == 7.0;
	}

	@Test
	public void testTotalCount() {
		HalfLifeMap<String, Double> map = new HalfLifeMap<>(10);
		map.put("Fred",  7.0);
		map.put("Barney", 9.3);
		map.put("Fred", 39.4);

		assert map.getTotalCount() == 3.0;
	}

	@Test
	public void testDevalue() {
		HalfLifeMap<String, Double> map = new HalfLifeMap<>(1);
		map.put("Fred",  7.0);
		int count = 1;
		for (int i = 0; i < map.devalueInterval + 1; i++) {
			map.get("Fred");
			count++;
		}

		assert map.devalueCount == 0; // reset after every devaluation
		assert map.getTotalCount() == 0.9 * count;
	}

	@Test
	public void testDevalueRaceCondition() {
		HalfLifeMap<String, Double> map = new HalfLifeMap<>(1);
		map.put("Fred",  7.0);
		double previousTotal = map.getTotalCount();

		assert map.devalueCount < map.devalueInterval;
		map.devalue();  // should be ignored
		assert map.getTotalCount() == previousTotal;
	}

	@Test
	public void testGetCount() {
		HalfLifeMap<String, Double> map = new HalfLifeMap<>(1);
		map.put("Fred",  7.0);

		assert map.getCount("Fred") == 1;
	}

	@Test
	public void testGetCountNotPresent() {
		HalfLifeMap<String, Double> map = new HalfLifeMap<>(1);
		map.put("Fred",  7.0);

		assert map.getCount("Barney") == 0;
	}

	@Test
	public void testPrunedValueDefaults() {
		HalfLifeMap<String, Double> map = new HalfLifeMap<>(1);
		// by default, pruned value is untracked (represented by NaN)
		assert Double.isNaN(map.getPrunedValue());
		map.setTrackPrunedValue(true);
		// set it to track, but don't prune any value
		assert map.getPrunedValue() == 0;
		// now set it to stop tracking
		map.setTrackPrunedValue(false);
		assert Double.isNaN(map.getPrunedValue());
	}

	@Test
	public void testGetPrunedValue() {
		HalfLifeMap<String, Double> map = new HalfLifeMap<>(1);
		map.setTrackPrunedValue(true);

		// Only Fred will survive the upcoming prune
		map.put("Fred", 9.0);
		map.get("Fred"); map.get("Fred");
		assert map.getCount("Fred") == 3;

		// trigger a prune by inserting > 2 * ideal_size elements
		map.put("Barney", 7.4);
		map.put("Wilma", 34.6);

		assert map.getPrunedCount() == 2;
		assert !map.containsKey("Barney");
		assert !map.containsKey("Wilma");
		assert map.getPrunedValue() == 42.0;
	}

	@Test
	public void testClear() {
		HalfLifeMap<String, Double> map = new HalfLifeMap<>(1);
		map.setTrackPrunedValue(true);
		map.put("Fred", 9.0);
		map.get("Fred"); map.get("Fred");
		map.put("Barney", 7.4);
		map.prune();
		assert map.getPrunedCount() == 1;

		map.clear();

		assert map.getPrunedCount() == 0;
		assert map.getPrunedValue() == 0.0;
	}

	public class KeyListener<K, V> implements IPruneListener<K, V> {
		List<K> prunedKeys;

		public KeyListener() {
			prunedKeys = new ArrayList<K>(1);
		}

		@Override
		public void pruneEvent(List<Entry<K, V>> pruned) {
			for (Map.Entry<K, V> entry: pruned) {
				prunedKeys.add(entry.getKey());
			}
		}
	}

	@Test
	public void testListeners() {
		HalfLifeMap<String, Double> map = new HalfLifeMap<>(1);
		map.put("Fred", 9.0);
		KeyListener<String, Double> listener = new KeyListener<>();
		map.addListener(listener);
		map.put("Barney", 31337.0);
		map.prune();

		assert map.listeners.contains(listener);
		assert listener.prunedKeys.contains("Barney");
	}

	@Test
	public void testRemoveListeners() {
		HalfLifeMap<String, Double> map = new HalfLifeMap<>(1);
		map.put("Fred", 9.0);
		KeyListener<String, Double> listener = new KeyListener<>();
		map.addListener(listener);
		map.removeListener(listener);
		map.put("Barney", 31337.0);
		map.prune();

		assert !map.listeners.contains(listener);
		assert !listener.prunedKeys.contains("Barney");
	}

	@Test
	public void testListenersAreNotCloned() {
		HalfLifeMap<String, Double> map = new HalfLifeMap<>(1);
		KeyListener<String, Double> listener = new KeyListener<>();
		map.addListener(listener);
		HalfLifeMap<String, Double> map2 = map.clone();

		assert map2.listeners == null;
	}
}
