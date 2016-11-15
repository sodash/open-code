package com.winterwell.utils.web;

import java.util.Collection;
import java.util.Map;

import winterwell.utils.IFn;
import winterwell.utils.containers.Containers;

/**
 * I can has dynamic ajax action! Call toJson() to get a JSON String
 * @author daniel
 */
public interface IHasJson {

	void appendJson(StringBuilder sb);
	
	/**
	 * Matches JSONString#toJSONString()
	 * @return A JSON String
	 */
	String toJSONString();
	
	/**
	 * @return An object suitable for conversion to JSON using a standard library (such as Jetty's JSON class).
	 * Probably a Map, with nested Maps, Lists, and primitives/Strings. 
	 * @throws UnsupportedOperationException
	 */
	Object toJson2() throws UnsupportedOperationException;
	

	/**
	 * Use this with Containers.applyToValues()
	 */
	public static final IFn RECURSE_TOJSON2 = new IFn() {
		@Override
		public Object apply(Object value) {
			if (value==null) return null;
			if (value instanceof com.winterwell.utils.web.IHasJson) {
				return ((com.winterwell.utils.web.IHasJson) value).toJson2();
			}
			if (value instanceof Collection) {
				return Containers.apply(this, (Collection)value);
			}
			if (value instanceof Map) {
				return Containers.applyToValues(this, (Map)value);
			}
			// leave as is and hope
			return value;
		}
	};
}
