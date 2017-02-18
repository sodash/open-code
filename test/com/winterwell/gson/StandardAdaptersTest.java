package com.winterwell.gson;

import static org.junit.Assert.*;

import java.util.Map;

import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.utils.time.Time;

public class StandardAdaptersTest {

	@Test
	public void testWithMap() {
		Gson gsonWith = new GsonBuilder()
						.registerTypeAdapter(Class.class, new StandardAdapters.ClassTypeAdapter())
						.registerTypeAdapter(Time.class, new StandardAdapters.TimeTypeAdapter())
						.create();
		
		ArrayMap map = new ArrayMap("myclass", getClass(), "mytime", new Time());
		
		System.out.println("MAP WITH");
		String gson1 = gsonWith.toJson(map);
		System.out.println(gson1);
		Map map1 = gsonWith.fromJson(gson1);
		System.out.println(map1);
		System.out.println(map1.get("myclass").getClass());
	}

	@Test
	public void testWithoutMap() {
		Gson gsonwo = new GsonBuilder().create();
		
		ArrayMap map = new ArrayMap("myclass", getClass(), "mytime", new Time());
		
		System.out.println("MAP WITHOUT");
		String gson2 = gsonwo.toJson(map);
		System.out.println(gson2);
		Map map2 = gsonwo.fromJson(gson2);
		System.out.println(map2);
		System.out.println(map2.get("myclass").getClass());
	}


	@Test
	public void testWithObj() {
		Gson gsonWith = new GsonBuilder()
						.registerTypeAdapter(Class.class, new StandardAdapters.ClassTypeAdapter())
						.registerTypeAdapter(Time.class, new StandardAdapters.TimeTypeAdapter())
						.create();
		
		MyObj map = new MyObj(getClass(), new Time());
		
		System.out.println("OBJ WITH");
		String gson1 = gsonWith.toJson(map);
		System.out.println(gson1);
		MyObj map1 = gsonWith.fromJson(gson1);
		System.out.println(map1);
	}

	@Test
	public void testWithoutObj() {
		Gson gsonwo = new GsonBuilder().create();
		
		MyObj map = new MyObj(getClass(), new Time());
		
		System.out.println("WITHOUT");
		String gson2 = gsonwo.toJson(map);
		System.out.println(gson2);
		MyObj map2 = gsonwo.fromJson(gson2);
		System.out.println(map2);
	}
}


class MyObj {

	private Class klass;
	private Time time;

	public MyObj(Class class1, Time time) {
		this.klass = class1;
		this.time = time;
	}
	
}