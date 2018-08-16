package com.winterwell.datalog;

import org.junit.Test;

import com.winterwell.utils.time.Time;

public class CallbackManagerTest {

	@Test
	public void test() {
		DataLogEvent dle = new DataLogEvent("minview", 1);
		dle.time = new Time();
		
		CallbackManager cm = new CallbackManager();
		Callback callback = new Callback("gl", "minview", "http://localas.good-loop.com");
		cm.consume2_doCallback(dle, callback);
	}

}
