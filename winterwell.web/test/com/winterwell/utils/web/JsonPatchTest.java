package com.winterwell.utils.web;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.utils.containers.Containers;

public class JsonPatchTest {

	@Test
	public void testSimple() {
		Map before = new ArrayMap("a", "Apple");
		Map after = new ArrayMap("b", "Banana");
		JsonPatch jp = new JsonPatch(before, after);
		System.out.println(jp.toJson2());
		
		Map after2 = new ArrayMap(before);
		jp.apply(after2);
		System.out.println(after);
		System.out.println(after2);
		assert Containers.same(after2, after);
	}
	
	@Test
	public void testSimpleList() {
		List before = Arrays.asList("a", "Apple");
		List after = Arrays.asList("a", "Banana");
		JsonPatch jp = new JsonPatch(before, after);
		System.out.println(jp.toJson2());
		
		List after2 = new ArrayList(before);
		jp.apply(after2);
		System.out.println(after);
		System.out.println(after2);
		assert Containers.same(after2, after);
	}
	
	@Test
	public void testMaps2Deep() {
		Map before = new ArrayMap("a", new ArrayMap("fruit", "Apple"), "b", new ArrayMap("fruit", "Breadfruit"), "c", "carrot", "d", "dog");
		Map after = new ArrayMap("a", new ArrayMap("animal", "Antelope"), "b", new ArrayMap("fruit", "Banana"), "c", "carrot", "d", "date");
		JsonPatch jp = new JsonPatch(before, after);
		System.out.println(jp.toJson2());
		
		Map after2 = new ArrayMap(before);
		jp.apply(after2);
		System.out.println(after);
		System.out.println(after2);
		assert Containers.same(after2, after);
	}

}
