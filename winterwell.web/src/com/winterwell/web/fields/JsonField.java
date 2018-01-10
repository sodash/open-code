/**
 * 
 */
package com.winterwell.web.fields;

import java.util.Arrays;

import org.eclipse.jetty.util.ajax.JSON;


/**
 * Handle a JSON-encoded parameter.
 * @author daniel
 */
public class JsonField extends AField<Object> {
	
	public JsonField(String name) {
		super(name);
	}
	
	@Override
	public String toString(Object value) {
		String json = JSON.toString(value);
		return json;
	}
	
	@Override
	public Object fromString(String v) throws Exception {
		Object jobj = JSON.parse(v);
		// prefer List to Object[]
		if (jobj instanceof Object[]) {
			Object[] jarr = (Object[]) jobj;
			return Arrays.asList(jarr);
		}
		return jobj;
	}

}
