package com.winterwell.web.fields;

import static org.junit.Assert.*;

import org.junit.Test;

public class IntFieldTest {

	@Test
	public void testFromStringString() {
		IntField f = new IntField("foo");
		Integer a = f.fromString("1");
		Integer b = f.fromString("1.0");
		assert b==1 : b;
		
		f.setRounding(true);
		Integer c = f.fromString("1.01");
	}

}
