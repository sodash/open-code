package com.winterwell.maths.datastorage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
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
		assert index.size() == 10;
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
		HalfLifeMap map = new HalfLifeMap(4);
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

	/**
	 * Conclusion: HalLifeMap has little overhead for get, about twice as slow
	 * for put.
	 */
	@Test
	public void testSpeed() {
		// half-life
		HalfLifeMap map = new HalfLifeMap(100);
		Log.setMinLevel(Level.OFF);
		{
			double perOp1a = speed2_heavyGet(map);
			double perOp1b = speed2_heavyGet(Collections.synchronizedMap(new HashMap()));
			double pc = 100*((perOp1a/perOp1b)-1);
			System.out.println("Mostly Gets: HalfLifeMap "+perOp1a +" vs HashMap "+perOp1b+":\t"+StrUtils.toNSigFigs(pc, 2)+"%");
		}
		{
			double perOp1a = speed2_heavyPut(map);
			double perOp1b = speed2_heavyPut(Collections.synchronizedMap(new HashMap()));
			double pc = 100*((perOp1a/perOp1b)-1);
			System.out.println("Mostly Puts: HalfLifeMap "+perOp1a +" vs HashMap "+perOp1b+":\t"+StrUtils.toNSigFigs(pc, 2)+"%");
		}
		assert map.size() <= map.getIdealSize()*2;
	}
	
	private double speed2_heavyGet(Map map) {
		StopWatch sw = new StopWatch();
		int ops = 0;
		for(int i=0; i<10000; i++) {
			map.put(Utils.getRandomString(2), i);
			ops++;
			for(int j=0; j<1000; j++) {
				map.get(Utils.getRandomString(2));
				ops++;
			}
		}
		return (1.0*sw.getTime())/ops;
	}
	
	private double speed2_heavyPut(Map map) {
		StopWatch sw = new StopWatch();
		int ops = 0;
		for(int i=0; i<100000; i++) {
			map.get(Utils.getRandomString(2));
			ops++;
			for(int j=0; j<10; j++) {
				map.put(Utils.getRandomString(2), j);
				ops++;
			}
		}
		return (1.0*sw.getTime())/ops;
	}

	@Test
	public void testSerialisation() {
		HalfLifeMap<String, Object> index = new HalfLifeMap(10);

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
}
