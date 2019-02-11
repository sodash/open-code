package com.winterwell.gson;

import java.util.List;
import java.util.Map;

import org.junit.Test;

public class ListHandlingTest {

	@Test
	public void testToList() {
		String json = "[1.0, 2.0, 3.0]";
		List obj = new Gson().fromJson(json, List.class);
		assert obj.get(0).equals(1.0) : obj;
	}

	/**
	 * NB: this will fail for vanilla GSON
	 */
	@Test
	public void testIndexKeyedObjectList() {
		String json = "{0:\"a\", 1:\"b\"}";
		List obj = new Gson().fromJson(json, List.class);
		assert obj.get(0).equals("a") : obj;
		assert obj.get(1).equals("b") : obj;
	}
	
	
	@Test
	public void testListOrArray_IsList() {
		String json = "{'list': [1.0, 2.0, 3.0]}";
		Map obj = new Gson().fromJson(json, Map.class);
		Object list = obj.get("list");
		System.out.println(list.getClass());
		assert list instanceof List;
	}
}
