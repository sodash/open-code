package com.winterwell.depot.merge;

import org.junit.Test;

public class NumMergerTest {

	@Test
	public void testDoMerge() {
		NumMerger nm = new NumMerger();
		Number x = nm.doMerge(1, 2, 3.5);
		assert x.doubleValue() == 4.5 : x;
	}

}
