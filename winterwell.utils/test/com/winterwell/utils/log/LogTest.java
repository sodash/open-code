package com.winterwell.utils.log;

import java.util.ArrayList;

import org.junit.Test;

import com.winterwell.datalog.Rate;
import com.winterwell.utils.Utils;
import com.winterwell.utils.time.TUnit;

public class LogTest {

	@Test
	public void testThrottle() {
		LogConfig config = new LogConfig();
		ArrayList<Report> reports = new ArrayList();
		final String ttag = "test"+Utils.getRandomString(4);
		ILogListener listener = new ILogListener() {			
			@Override
			public void listen(Report report) {
				if (ttag.equals(report.tag)) {
					reports.add(report);
				}
			}
		};
		Log.addListener(listener);
		config.throttleAt = new Rate(2, TUnit.MINUTE);
		Log.setConfig(config);
		
		Log.i(ttag, "Smoke test");
		
		Log.i(ttag, "Smoke test2");
		
		Log.i(ttag, "Smoke test3 - throttle?");
		Log.i(ttag, "Smoke test4 - throttle?");
		Log.i(ttag, "Smoke test5 - throttle?");
		
		assert ! reports.isEmpty();
		assert reports.size() < 4;
	}

}
