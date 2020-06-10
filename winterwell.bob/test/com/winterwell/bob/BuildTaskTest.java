package com.winterwell.bob;

import org.junit.Test;

import com.winterwell.bob.wwjobs.BuildUtils;
import com.winterwell.depot.Desc;

public class BuildTaskTest {

	@Test
	public void testGetDesc() {
		BuildUtils a = new BuildUtils();
		Desc desca = a.getDesc();
		String id = desca.getId();
		System.out.println(desca);
	}
		
	@Test
	public void testEqualsObject() {
		BuildUtils a = new BuildUtils();
		BuildUtils b = new BuildUtils();
		assert a != b;
		assert a.equals(b);
		assert a.hashCode() == b.hashCode();		
	}
	
	
	@Test
	public void testEqualsAfterRun() {
		BuildUtils a = new BuildUtils();
		Desc desca = a.getDesc();
		a.run();
		
		BuildUtils b = new BuildUtils();
		Desc descb = b.getDesc();
		
		assert a != b;
		assert desca.equals(descb) : desca.getId() +" vs "+descb.getId();
		assert a.equals(b);
		assert a.hashCode() == b.hashCode();		
	}
}
