package com.winterwell.utils;

import static org.junit.Assert.*;

import org.junit.Test;

public class DepTest {

	@Test
	public void testGetSimple() {
		MyThingy mt = Dep.get(MyThingy.class);
		MyThingy mt2 = Dep.get(MyThingy.class);
		assert mt == mt2;
	}

}

class MyThingy {
	
}
