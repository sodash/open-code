package com.winterwell.utils.web;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.winterwell.utils.Printer;
import com.winterwell.utils.io.SysOutCollectorStream;
import com.winterwell.utils.time.Time;

public class SimpleJsonTest {

	@Test
	public void testToFrom() {
		HashMap time4task = new HashMap();
		time4task.put("testKey", new Time(2020,1,1));
		String j = new SimpleJson().toJson(time4task);
		Printer.out(j);
		Map fj = (Map) new SimpleJson().fromJson(j);
		Printer.out(fj);
	}
}
