package com.winterwell.utils.log;
///**
// * 
// */
//package com.winterwell.utils.reporting;
//
//import java.util.logging.Level;
//
//import org.junit.Test;
//
//import com.winterwell.utils.log.Log;
//
///**
// * @author daniel
// * 
// */
//public class LogTest {
//
//
//	@Test
//	public void testConfig() {
//		LogCollector lc = new LogCollector();
//		Log.addListener(lc);
//		Level lvl1 = Log.getMinLevel("skip");
//		Level lvl2 = Log.getMinLevel("important");
//		assert lvl1 == Level.OFF;
//		assert lvl2 == Log.VERBOSE;
//		
//		Log.e("skip", "skip this - no report");
//		Log.v("important", "report this it matters");
//		Log.w("ignore", "ignore this - no report");
//		Log.v("focus", "we focus on this");
//		
//		String logged = lc.toString();
//		System.out.println(logged);
//		assert logged.contains("important");
//		assert ! logged.contains("skip");
//		assert ! logged.contains("ignore");
//		lc.close();
//	}
//
//
//	@Test
//	public void testFormat() {
//		String s = Log.format("Hello {class} world {method} class.method");
//		assert "Hello LogTest world testFormat class.method".equals(s) : s;
//	}
//
//	/**
//	 * Test method for
//	 * {@link com.winterwell.utils.log.Log#report(java.lang.String, java.lang.Object, java.util.logging.Level)}
//	 * .
//	 */
//	@Test
//	public void testNullTag() {
//		LogCollector lc = new LogCollector();
//		Log.addListener(lc);
//
//		Log.w(null, "Hello World");
//
//		System.out.println(lc.toString());
//		assert lc.toString().contains("LogTest");
//
//		lc.close();
//	}
//
//}
