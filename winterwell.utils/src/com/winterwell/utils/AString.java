package com.winterwell.utils;

import java.io.Serializable;

import com.winterwell.utils.web.IHasJson;

/**
 * Wraps a String - to provide some type safety in interface methods.
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
	 * 
	 * @param name String or an AString wrapper. Cannot be null
	 */
	public AString(CharSequence name) {
		this.name = name.toString();		
	}

	@Override
	public final boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AString other = (AString) obj;
		return name.equals(other.name);
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
