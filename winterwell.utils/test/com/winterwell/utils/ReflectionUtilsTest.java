package com.winterwell.utils;

import static org.junit.Assert.*;

import org.junit.Test;

public class ReflectionUtilsTest {

	@Test
	public void testSetPrivateField() {
		Foo foo = new Foo();
		ReflectionUtils.setPrivateField(foo, "barString", "hello :)");
		ReflectionUtils.setPrivateField(foo, "barInt", 7);
		assert foo.getBarInt() == 7;
		
		ReflectionUtils.setPrivateField(foo, "barInteger", 6);
		ReflectionUtils.setPrivateField(foo, "barInt", 6.0);
		assert foo.getBarInt() == 6;
		assert foo.getBarInteger() == 6;
		
		ReflectionUtils.setPrivateField(foo, "barInteger", "5");
		assert foo.getBarInteger() == 5;
	}

}


class Foo {
	
	private String barString;
	
	private int barInt;
	
	public int getBarInt() {
		return barInt;
	}
	
	private Integer barInteger;
	
	public Integer getBarInteger() {
		return barInteger;
	}
}
