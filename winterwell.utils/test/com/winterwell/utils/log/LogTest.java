package com.winterwell.utils.log;

import static org.junit.Assert.*;

import java.util.ArrayList;

import org.junit.Test;

import com.winterwell.datalog.Rate;
import com.winterwell.utils.time.TUnit;

public class LogTest {

	@Test
	public void testThrottle() {
		LogConfig config = new LogConfig();
		ArrayList<Report> reports = new ArrayList();
		ILogListener listener = new ILogListener() {			
			@Override
			public void listen(Report report) {
				if ("test".equals(report.tag)) {
					reports.add(report);
				}
			}
		};
		Log.addListener(listener);
		config.throttleAt = new Rate(2, TUnit.MINUTE);
		Log.setConfig(config);
		
		Log.i("test", "Smoke test");
		
		Log.i("test", "Smoke test2");
		
		Log.i("test", "Smoke test3 - throttle?");
		Log.i("test", "Smoke test4 - throttle?");
		Log.i("test", "Smoke test5 - throttle?");
		
		assert ! reports.isEmpty();
		assert reports.size() < 4;
	}

}
