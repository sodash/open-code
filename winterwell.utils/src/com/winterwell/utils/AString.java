package com.winterwell.utils;

import java.io.Serializable;

import com.winterwell.utils.log.Log;
import com.winterwell.utils.web.IHasJson;

/**
 * Wraps a String - to provide some type safety in interface methods, eg for ids.
 * 
 * Usage: Create a custom class that extends this. Voila - a "type safe" String.
 *  
 * @author daniel
 *
 */
public class AString implements IHasJson, Serializable, CharSequence  {

	@Override
	public final int hashCode() {
		return name.hashCode();
	}

	/**
	 * Needed for reflection-based de-serialisation, eg from json
	 */
	public AString(String name) {
		this((CharSequence)name);
	}
	
	/**
	 * 
	 * @param name String or an AString wrapper. Cannot be null.
	 * If it is an AString, the class must match -- this provides type-safety
	 * (e.g. a User-id cannot be turned into a Document-id)
	 */
	public AString(CharSequence name) {
		this.name = name.toString();
		if (name instanceof AString && ! name.getClass().equals(getClass())) {
			throw new IllegalArgumentException("Cannot change "+name.getClass().getSimpleName()
					+" to "+getClass().getSimpleName()+": "+name);
		}
	}

	/**
	 * Only equals to its own class! 
	 * 
	 * Because we could not make String.equals(AString) return true, we opt for symmetric behaviour.
	 * It will log a warning if compared against a String.
	 * 
	 *  @see #equiv(CharSequence)
	 */
	@Override
	public final boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if ( ! (obj instanceof CharSequence)) return false;
		// match or not?
		if ( ! name.equals(obj.toString())) {
			return false;
		}
		if (getClass().equals(obj.getClass())) return true;
		// fail, but log a warning -- this could be a bug
		Log.w(getClass().getName(), "Same String but != as classes mismatch: "+obj.getClass()+" string: "+name);
		return false;
	}
	

	/**
	 * @param b
	 * @return true if this and b have equals string values. Like equals() but ignores class.
	 */
	public boolean equiv(CharSequence b) {
		if (b==null) return false;
		return toString().equals(b.toString());
	}

	private static final long serialVersionUID = 1L;
	public final String name;

	@Override
	public final String toJson2() throws UnsupportedOperationException {
		return name;
	}

	@Override
	public final String toString() {
		return name;
	}

	@Override
	public final int length() {
		return name.length();
	}

	@Override
	public final char charAt(int index) {
		return name.charAt(index);
	}

	@Override
	public final CharSequence subSequence(int start, int end) {
		return name.subSequence(start, end);
	}
	
}
