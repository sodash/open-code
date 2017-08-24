package com.winterwell.depot.merge;

import java.util.Map;

import org.junit.Test;

import com.winterwell.utils.containers.ArrayMap;

public class MapMergerTest {


	@Test
	public void testMapMergerNumber() {
		ArrayMap before = new ArrayMap("a", 0);
		ArrayMap after = new ArrayMap("a", 1);
		ArrayMap latest = new ArrayMap("a", 2);
		
		Merger m = new Merger();
		MapMerger mm = (MapMerger) m.getMerger(Map.class);
		
		Map merged = (Map) mm.doMerge(before, after, latest);
		
		assert merged.equals(new ArrayMap("a", 3)) : merged;
	}


	@Test
	public void testMapMergerNewNumber() {
		ArrayMap before = new ArrayMap();
		ArrayMap after = new ArrayMap("a", 1);
		ArrayMap latest = new ArrayMap("a", 2);
		
		Merger m = new Merger();
		MapMerger mm = (MapMerger) m.getMerger(Map.class);
		
		Map merged = (Map) mm.doMerge(before, after, latest);
		
		assert merged.equals(new ArrayMap("a", 3)) : merged;
	}

	@Test
	public void testMapMergerWithKid() {
		ArrayMap before = new ArrayMap("kid", new ArrayMap("b", 0));
		ArrayMap after = new ArrayMap("kid", new ArrayMap("a", 1, "b", 2));
		ArrayMap latest = new ArrayMap("kid", new ArrayMap("b", 4, "c", 5));
		
		Merger m = new Merger();
		MapMerger mm = (MapMerger) m.getMerger(Map.class);
		
		Map merged = (Map) mm.doMerge(before, after, latest);
		
		System.out.println(merged);
		Map mkid = (Map) merged.get("kid");
		assert mkid.equals(new ArrayMap("a", 1, "b", 6, "c", 5)) : mkid;
	}

	@Test
	public void testMapMergerWithNewKid() {
		ArrayMap before = new ArrayMap();
		ArrayMap after = new ArrayMap("kid", new ArrayMap("a", 1, "b", 2));
		ArrayMap latest = new ArrayMap("kid", new ArrayMap("b", 4, "c", 5));
		
		Merger m = new Merger();
		MapMerger mm = (MapMerger) m.getMerger(Map.class);
		
		Map merged = (Map) mm.doMerge(before, after, latest);
		
		System.out.println(merged);
		Map mkid = (Map) merged.get("kid");
		assert mkid.equals(new ArrayMap("a", 1, "b", 6, "c", 5)) : mkid;
	}

}
