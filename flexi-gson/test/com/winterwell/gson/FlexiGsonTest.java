package com.winterwell.gson;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.winterwell.utils.containers.ArrayMap;

public class FlexiGsonTest {
	
	@Test
	public void testESBug() {
		{
			Gson gson = new GsonBuilder().create();
			ArrayMap map = new ArrayMap("a", "Hello");
			String json = gson.toJson(map);		
			assert json.contains("Hello");
		}
		{
			Gson gson = new GsonBuilder().create();
			ArrayMap map = new ArrayMap("a", new RawJson("\"Hello\""));
			String json = gson.toJson(map);		
			assert json.contains("Hello");
			System.out.println(json);
		}
	}
	
	@Test
	public void testConvert() {
		Gson gson = new GsonBuilder().create();
		DummyObject dummy1 = new DummyObject();
		dummy1.a = "Hello";
		dummy1.b = 7;
		dummy1.cs.add("C");
		String json = gson.toJson(dummy1);
		DummyObject dummy2 = gson.fromJson(json, DummyObject.class);
		assert dummy2.equals(dummy1);
		
		ArrayMap map = new ArrayMap(
			"a", "Hello",
			"b", 7,
			"cs", new String[]{"C"}
		);
		DummyObject dummy3 = gson.convert(map, DummyObject.class);
		System.out.println(dummy3);
		assert dummy1.equals(dummy3);
	}

}


class DummyObject {
	@Override
	public String toString() {
		return "DummyObject [a=" + a + ", b=" + b + ", cs=" + cs + "]";
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((a == null) ? 0 : a.hashCode());
		result = prime * result + b;
		result = prime * result + ((cs == null) ? 0 : cs.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DummyObject other = (DummyObject) obj;
		if (a == null) {
			if (other.a != null)
				return false;
		} else if (!a.equals(other.a))
			return false;
		if (b != other.b)
			return false;
		if (cs == null) {
			if (other.cs != null)
				return false;
		} else if (!cs.equals(other.cs))
			return false;
		return true;
	}
	String a;
	int b;
	List<String> cs = new ArrayList();
}