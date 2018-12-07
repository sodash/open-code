package com.winterwell.web.test;

import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionContext;

import com.winterwell.utils.ReflectionUtils;
import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.utils.log.Log;

public class TestHttpSession implements HttpSession {

	final Map<String, Object> attributes = new ArrayMap<String, Object>();

	@Override
	public Object getAttribute(String arg0) {
		return attributes.get(arg0);
	}

	@Override
	public Enumeration<String> getAttributeNames() {
		Set<String> anames = attributes.keySet();
		Iterator<String> anit = anames.iterator();
		return new Enumeration<String>() {
			@Override
			public boolean hasMoreElements() {
				return anit.hasNext();
			}

			@Override
			public String nextElement() {
				return anit.next();
			}
		};
	}

	@Override
	public long getCreationTime() {
		Log.report("Not implemented: " + ReflectionUtils.getCaller()); // TODOAuto-generated
																// method stub
		return 0;
	}

	@Override
	public String getId() {
		Log.report("Not implemented: " + ReflectionUtils.getCaller()); // TODOAuto-generated
																// method stub
		return null;
	}

	@Override
	public long getLastAccessedTime() {
		Log.report("Not implemented: " + ReflectionUtils.getCaller()); // TODOAuto-generated
																// method stub
		return 0;
	}

	@Override
	public int getMaxInactiveInterval() {
		Log.report("Not implemented: " + ReflectionUtils.getCaller()); // TODOAuto-generated
																// method stub
		return 0;
	}

	@Override
	public ServletContext getServletContext() {
		Log.report("Not implemented: " + ReflectionUtils.getCaller()); // TODOAuto-generated
																// method stub
		return null;
	}

	@Override
	public HttpSessionContext getSessionContext() {
		Log.report("Not implemented: " + ReflectionUtils.getCaller()); // TODOAuto-generated
																// method stub
		return null;
	}

	@Override
	public Object getValue(String arg0) {
		Log.report("Not implemented: " + ReflectionUtils.getCaller()); // TODOAuto-generated
																// method stub
		return null;
	}

	@Override
	public String[] getValueNames() {
		Log.report("Not implemented: " + ReflectionUtils.getCaller()); // TODOAuto-generated
																// method stub
		return null;
	}

	@Override
	public void invalidate() {
		Log.report("Not implemented: " + ReflectionUtils.getCaller()); // TODOAuto-generated
																// method stub

	}

	@Override
	public boolean isNew() {
		Log.report("Not implemented: " + ReflectionUtils.getCaller()); // TODOAuto-generated
																// method stub
		return false;
	}

	@Override
	public void putValue(String arg0, Object arg1) {
		Log.report("Not implemented: " + ReflectionUtils.getCaller()); // TODOAuto-generated
																// method stub

	}

	@Override
	public void removeAttribute(String arg0) {
		attributes.remove(arg0);
	}

	@Override
	public void removeValue(String arg0) {
		Log.report("Not implemented: " + ReflectionUtils.getCaller()); // TODOAuto-generated
																// method stub

	}

	@Override
	public void setAttribute(String arg0, Object arg1) {
		attributes.put(arg0, arg1);
	}

	@Override
	public void setMaxInactiveInterval(int arg0) {
		Log.report("Not implemented: " + ReflectionUtils.getCaller()); // TODOAuto-generated
																// method stub
	}

}
