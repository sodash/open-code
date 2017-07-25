package com.winterwell.data;

import java.util.Map;

import org.eclipse.jetty.util.ajax.JSON;

import com.winterwell.gson.Gson;
import com.winterwell.utils.Dep;

/**
 * wrapper for json objects
 * @author daniel
 *
 */
public final class JThing<T> {

	private String json;
	private Map<String,Object> map;
	private T java;
	
	public JThing() {
		this(null);
	}
	public JThing(String json) {
		this.json = json;
	}

	public String string() {
		if (json==null && map!=null) {
			Gson gson = Dep.get(Gson.class);
			json = gson.toJson(map);
		}
		if (json==null && java!=null) {
			Gson gson = Dep.get(Gson.class);
			json = gson.toJson(java);
		}
		return json;
	}
	
	public Map<String, Object> map() {
		if (map==null && json!=null) map = (Map<String, Object>) JSON.parse(json);
		return map;
	}
	
	public JThing<T> setJava(T java) {
		this.java = java;
		return this;
	}
	
	public T java() {
		return java;
	}
}
