package com.winterwell.web.ajax;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.eclipse.jetty.util.ajax.JSON;

import com.winterwell.depot.IInit;
import com.winterwell.depot.INotSerializable;
import com.winterwell.gson.Gson;
import com.winterwell.utils.Dep;
import com.winterwell.utils.ReflectionUtils;
import com.winterwell.utils.StrUtils;
import com.winterwell.utils.web.IHasJson;

/**
 * Wrapper for json objects
 * @author daniel
 *
 */
public final class JThing<T> 
implements INotSerializable, IHasJson // serialize the json not this wrapper 
{

	private String json;
	private Map<String,Object> map;
	private T java;
	private Class<T> type;
	
	/**
	 * Optionally used for ES version control
	 */
	public Object version;
	
	/**
	 * Equivalent to new JThing().setJava(item)
	 * @param item the Java POJO
	 */
	public JThing(T item) {
		setJava(item);
	}
	
	/**
	 * Usually needed (depending on the gson setup) for {@link #java()} to deserialise json.
	 * Note: Once set, you cannot change the type (repeated calls with the same type are fine).
	 * @param type
	 * @return this
	 */
	public JThing<T> setType(Class<T> type) {
		// Once set, you cannot change the type (repeated calls with the same type are fine).
		assert this.type==null || this.type.equals(type) : this.type+" != "+type;
		this.type = type;
		assert java==null || type==null || ReflectionUtils.isa(java.getClass(), type) : type+" vs "+java.getClass();
		return this;
	}
	
	public Class<T> getType() {
		return type;
	}
	
	public JThing() {		
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
	
	/**
	 * @return An unmodifiable map view.
	 * @see #put(String, Object)
	 */
	public Map<String, Object> map() {
		if (map==null && string()!=null) {
			map = (Map<String, Object>) JSON.parse(json);
		}
		if (map==null) {
			//
			return null;
		}
		return Collections.unmodifiableMap(map);
	}
	
	public JThing<T> setJava(T java) {
		this.java = java;
		if (java==null) return this;
		map = null;
		json = null;		
		if (type==null) type = (Class<T>) java.getClass();
		assert ReflectionUtils.isa(java.getClass(), type) : type+" vs "+java.getClass();		
		return this;
	}
	
	public JThing<T> setJson(String json) {
		this.json = json;
		this.java = null;
		this.map = null;
		return this;
	}
	
	public T java() {
		if (java!=null) return java;
		String sjson = string();
		if (sjson != null) {
			assert type != null : "Call setType() first "+this;
			Gson gson = Dep.get(Gson.class);			
			T pojo = gson.fromJson(sjson, type);
			if (pojo instanceof IInit) {
				((IInit) pojo).init();				
			}
			// this will null out the json/map
			// ...which is good, as extra json from the front-end can cause bugs with ES mappings.
			setJava(pojo);		
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

	public Object toJson2() {
		// is it an object? (the normal case)
		if (map!=null) return map();
		if (json!=null && json.startsWith("{")) {
			return map();
		}
		if (string()==null) return null;
		return JSON.parse(string());		
	}
}
