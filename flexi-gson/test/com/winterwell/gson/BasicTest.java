package com.winterwell.gson;

import org.junit.Test;

import com.winterwell.utils.containers.ArrayMap;

public class BasicTest {	
	
	@Test
	public void testMapFromJsonWithClass() {
		String json = ("{'@class':'"+ArrayMap.class.getName()+"','a':1}")
				.replace('\'', '"');
		Gson gson = new Gson();
		System.out.println(json);
		Object jobj = gson.fromJson(json);
	}
	
	@Test
	public void testMapFromJsonWithBadClass() {
		try {
			String json = ("{'@class':'foo.bar.Nah'}")
					.replace('\'', '"');
			Gson gson = new Gson();
			Object jobj = gson.fromJson(json);
			assert false : jobj;
		} catch(Exception ex) {
			// OK
		}
	}
}
