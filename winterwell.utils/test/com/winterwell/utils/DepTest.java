package com.winterwell.utils;

import static org.junit.Assert.*;

import org.junit.Test;

public class DepTest {

	@Test
	public void testGetSimple() {
		Dep.set(MyThingy.class, new MyThingy());
		MyThingy mt = Dep.get(MyThingy.class);
		assert mt != null;
		MyThingy mt2 = Dep.get(MyThingy.class);
		assert mt == mt2;
	}

	@Test
	public void testContext() {
		Dep.set(MyThingy.class, new MyThingy());
		MyThingy mt = Dep.get(MyThingy.class);
		
		DepContext ctxt = Dep.setContext("test");
		MyThingy mt2 = Dep.get(MyThingy.class);
		assert mt == mt2;
		
		Dep.set(MyThingy.class, new MyThingy());
		mt2 = Dep.get(MyThingy.class);
		assert mt != mt2;
		
		ctxt.close();
		MyThingy mt3 = Dep.get(MyThingy.class);
		assert mt == mt3;		
	}

}

class MyThingy {
	
}
