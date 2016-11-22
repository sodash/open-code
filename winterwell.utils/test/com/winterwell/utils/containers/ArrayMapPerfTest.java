package com.winterwell.utils.containers;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.winterwell.utils.time.StopWatch;

public class ArrayMapPerfTest {

	@Test
	public void testMostlyGets() throws InstantiationException,
			IllegalAccessException {
		int total = 0;
		for (int n = 1; n < 20; n++) {
			StopWatch swa = new StopWatch().pause();
			StopWatch swh = new StopWatch().pause();
			for (Class k : new Class[] { ArrayMap.class, HashMap.class }) {
				Map map = (Map) k.newInstance();
				if (k == ArrayMap.class)
					swa.start();
				else
					swh.start();
				mostlyGets(n, map);
				swa.pause();
				swh.pause();
			}
			if (swa.getTime() > swh.getTime()) {
				System.out.println(n + ": HashMap");
			} else {
				System.out.println(n + ": ArrayMap!");
			}
		}
	}

	private void mostlyGets(int n, Map<Integer, Integer> map) {
		int sum = 0;
		for (int i = 0; i < 10000; i++) {
			int k = i % n;
			map.put(k, i);
			for (int j = 0; j < 1000; j++) {
				k = j % n;
				Integer v = map.get(k);
				if (v != null)
					sum += v;
			}
		}
	}

	private void mostlyPuts(int n, Map<Integer, Integer> map) {
		int sum = 0;
		for (int i = 0; i < 10000; i++) {
			for (int j = 0; j < 1000; j++) {
				int k = j % n;
				map.put(k, i);
			}
			for (int j = 0; j < 100; j++) {
				int k = j % n;
				Integer v = map.get(k);
				if (v != null)
					sum += v;
			}
		}
	}

	@Test
	public void testMostlyPuts() throws InstantiationException,
			IllegalAccessException {
		int total = 0;
		for (int n = 1; n < 20; n++) {
			StopWatch swa = new StopWatch().pause();
			StopWatch swh = new StopWatch().pause();
			for (Class k : new Class[] { ArrayMap.class, HashMap.class }) {
				Map map = (Map) k.newInstance();
				if (k == ArrayMap.class)
					swa.start();
				else
					swh.start();
				mostlyPuts(n, map);
				swa.pause();
				swh.pause();
			}
			if (swa.getTime() > swh.getTime()) {
				System.out.println(n + ": HashMap");
			} else {
				System.out.println(n + ": ArrayMap!");
			}
		}
	}

}
