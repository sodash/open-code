package com.winterwell.bob;

import static org.junit.Assert.*;

import org.junit.Test;

import jobs.BuildUtils;

public class BuildTaskTest {

	@Test
	public void testEqualsObject() {
		BuildUtils a = new BuildUtils();
		BuildUtils b = new BuildUtils();
		assert a != b;
		assert a.equals(b);
		assert a.hashCode() == b.hashCode();		
	}

}
