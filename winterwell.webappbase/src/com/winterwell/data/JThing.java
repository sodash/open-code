package com.winterwell.data;

import java.util.Collections;
import java.util.Map;

import org.eclipse.jetty.util.ajax.JSON;

import com.winterwell.depot.IInit;
import com.winterwell.gson.Gson;
import com.winterwell.utils.Dep;
import com.winterwell.utils.StrUtils;

/**
 * wrapper for json objects
 * @author daniel
 *
 */
public final class JThing<T> {

	private String json;
	private Map<String,Object> map;
	private T java;
	private Class<T> type;
	
	/**
	 * Usually needed (depending on the gson setup) for {@link #java()} to deserialise json.
	 * @param type
	 * @return
	 */
	public JThing<T> setType(Class<T> type) {
		this.type = type;
		return this;
	}
	
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
		return Collections.unmodifiableMap(map);
	}
	
	public JThing<T> setJava(T java) {
		this.java = java;
		return this;
	}
	
	public T java() {
		if (java==null && string() != null) {
			assert type != null : this;
			java = Dep.get(Gson.class).fromJson(string(), type);
			if (java instanceof IInit) {
				((IInit) java).init();
			}
		}
		return java;
	}
	
	@Override
	public String toString() {	
		return "JThing"+StrUtils.ellipsize(string(), 100)+"";
	}
	/**
	 * Modify the map() view, and force an update of the string() view + null the java() view
	 * @param k
	 * @param v
	 */
	public void put(String k, Object v) {
		map();
		map.put(k, v);
		java = null;
		json = null;
	}
	public JThing<T> setMap(Map<String, Object> obj) {
		this.map = obj;
		java = null;
		json = null;
		return this;
	}
}
