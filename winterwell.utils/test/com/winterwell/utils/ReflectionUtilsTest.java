package com.winterwell.utils;

import static org.junit.Assert.*;

import java.math.BigInteger;

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

	@Test
	public void testCPU() {
		System.out.println(ReflectionUtils.getJavaCPU());
		System.out.println(ReflectionUtils.getSystemCPU());
		// now lets use some CPU
		for(int i=0; i<2000; i += 200) {
			BigInteger big = factorial(new BigInteger(""+i));
			System.out.println("Java:	"+ReflectionUtils.getJavaCPU()+"	System:	"+ReflectionUtils.getSystemCPU());			
		}
	}

	private BigInteger factorial(BigInteger bigInteger) {
		BigInteger one = new BigInteger("1");
		if (bigInteger.compareTo(one) < 1) {
			return one;
		}
		return bigInteger.multiply(factorial(bigInteger.subtract(one)));
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
