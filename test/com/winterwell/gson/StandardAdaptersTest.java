package com.winterwell.gson;

import static org.junit.Assert.*;

import java.lang.reflect.Type;
import java.util.Map;

import org.junit.Test;

import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.utils.time.Time;

public class StandardAdaptersTest {

	
	@Test
	public void testTime() {
		Gson gsonWith = new GsonBuilder()
						.registerTypeAdapter(Class.class, new StandardAdapters.ClassTypeAdapter())
						.registerTypeAdapter(Time.class, new StandardAdapters.TimeTypeAdapter())
						.create();
		
		Time now = new Time();
		String gson1 = Gson.toJSON(now);
		System.out.println(gson1);
		Time now2 = gsonWith.fromJson(gson1, Time.class);
		assert now.equals(now2);
	}
	

	@Test
	public void testCharSequence() {
		Gson gsonWith = new GsonBuilder()
						.registerTypeAdapter(StringBuilder.class, new StandardAdapters.CharSequenceTypeAdapter(StringBuilder.class))
						.create();
				
		String gson1 = Gson.toJSON(new StringBuilder("foo"));		
		System.out.println(gson1);
		
		String gson2 = gsonWith.toJSON(new StringBuilder("foo"));		
		System.out.println(gson2);
		
		StringBuilder n2 = gsonWith.fromJson(gson1, StringBuilder.class);
	}
	
	@Test
	public void testLenientLong() {
		Gson gsonWith = new GsonBuilder()
						.registerTypeAdapter(Long.class, new StandardAdapters.LenientLongAdapter())
						.create();
		
		NumGen n1 = new NumGen();
		n1.a = 17.0000001;
		
		String gson1 = Gson.toJSON(n1);		
		System.out.println(gson1);
		
		NumLong n2 = gsonWith.fromJson(gson1, NumLong.class);
		assert n2.a == 17;
	}
	
	
	@Test
	public void testLenientlong() {
		Type longType = long.class;
		Gson gsonWith = new GsonBuilder()
						.registerTypeAdapter(longType, new StandardAdapters.LenientLongAdapter())
						.create();
		
		NumGen n1 = new NumGen();
		n1.a = 17.0000001;
		
		String gson1 = Gson.toJSON(n1);		
		System.out.println(gson1);
		
		Numlong n2 = gsonWith.fromJson(gson1, Numlong.class);
		assert n2.a == 17;
		
		// fails
//		NumLong n3 = gsonWith.fromJson(gson1, NumLong.class);
//		assert n3.a == 17;
	}
	
	@Test
	public void testLenientlongnull() {
		Type longType = long.class;
		Gson gsonWith = new GsonBuilder()
						.registerTypeAdapter(longType, new StandardAdapters.LenientLongAdapter(0L))
						.create();
		
		NumGen n1 = new NumGen();
		
		String gson1 = Gson.toJSON(n1);		
		System.out.println(gson1);
		// avoids an NPE
		Numlong n2 = gsonWith.fromJson(gson1, Numlong.class);
		assert n2.a == 0;		
	}
	
	@Test
	public void testLenientLonglong() {
		Gson gsonWith = new GsonBuilder()
						.registerTypeAdapter(long.class, new StandardAdapters.LenientLongAdapter())
						.registerTypeAdapter(Long.class, new StandardAdapters.LenientLongAdapter())
						.create();
		
		NumGen n1 = new NumGen();
		n1.a = 17.0000001;
		
		String gson1 = Gson.toJSON(n1);		
		System.out.println(gson1);
		
		Numlong n2 = gsonWith.fromJson(gson1, Numlong.class);
		assert n2.a == 17;
		
		NumLong n3 = gsonWith.fromJson(gson1, NumLong.class);
		assert n3.a == 17;
	}
	
	@Test
	public void testISOTime() {
		{
			Gson gsonWith = new GsonBuilder()
							.registerTypeAdapter(Time.class, new StandardAdapters.TimeTypeAdapter())
							.create();
						
			Time now2 = gsonWith.fromJson("\"2017-09-13T11:55:26Z\"", Time.class);
			
			assert now2 != null;
		}
		if (false) {	// without ""s it will fail, cos its not valid json
			Gson gsonWith = new GsonBuilder()
							.registerTypeAdapter(Time.class, new StandardAdapters.TimeTypeAdapter())
							.create();
						
			Time now2 = gsonWith.fromJson("2017-09-13T11:55:26Z", Time.class);
		}
	}
	
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


class NumGen {
	public Number a;
}

class Numlong {
	public long a;
}

class NumLong {
	public Long a;
}

class MyObj {

	private Class klass;
	private Time time;

	public MyObj(Class class1, Time time) {
		this.klass = class1;
		this.time = time;
	}
	
}