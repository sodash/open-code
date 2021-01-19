package com.winterwell.utils;

/**
 * Indicates a parameter or return value which can only be null.
 * 
 * @author daniel
 */
public final class Null {

	
	private Null() {
	}
	
	

	@Override
	public String toString() {
		return "Null";
	}
	
	@Override
	public int hashCode() {
		return 1;
	}
	
	@Override
	public boolean equals(Object obj) {
		return obj.getClass() == Null.class;
	}

	/**
	 * A non-null dummy value, which should be interpreted as null.
	 * Use-case: for places where you absolutely do have to give a value, e.g. some maps wont accept null. 
	 */
	public static final Null DUMMY_NULL = new Null();
	
}
