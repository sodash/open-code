package com.winterwell.utils;
import static org.junit.Assert.*;

import org.junit.Test;

import com.winterwell.utils.Intern;
import com.winterwell.utils.Utils;

public class InternTest {

	@Test
	public void testGet() {
		String foo1 = "foo";
		String foo2 = new StringBuilder("f").append("oo").toString();
		assert foo1 != foo2;
		String if1 = Intern.get(foo1);
		String if2 = Intern.get(foo2);
		String if3 = Intern.get(foo2);
		String if4 = Intern.get(foo1);
		assert if1 == if2;
		assert if1 == if3;
		assert if3 == if4;
	}

	
//	@Test 
	// Uncomment this to run the test
	// BUT: this can legitimately fail (it depends on the behaviour of the GC, which isn't fixed)
	// So do NOT include this test in automated acceptance testing.
	public void testOverload() {
		// try to force a GC to hit Intern with loads of data
		// I can't think of a good way to make this a reliable test. ^Dan W
		StringBuilder rem = null;
		for(int j=0; j<10000; j++) {
			StringBuilder s1 = new StringBuilder();
			for(int i=0; i<10000; i++) {
				s1.append(Utils.getRandomString(3));
			}			
			String is = Intern.get(s1.toString());
			if (rem==null) {
				rem = s1;
			}
		}		
		Utils.sleep(5000);
		System.gc();
		Utils.sleep(5000);
		// NB: hopefully rem's string has been dropped from Intern		
		String s1 = rem.toString();
		String s2 = new String(rem.toString());
		
		assert ! Intern.contains(s1) : "oh well - it didnt get flushed";
		
		String if1 = Intern.get(s1);
		String if2 = Intern.get(s2);
		assert if1 == if2;
	}
	
}
