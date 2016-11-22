package com.winterwell.web.fields;

import static org.junit.Assert.*;

import org.junit.Test;

public class DoubleFieldTest {

	@Test
	public void testFromStringString() {
		DoubleField df = new DoubleField("d");
		assert df.fromString("10.02%") == 0.1002;
	}

}
