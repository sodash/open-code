package com.winterwell.utils.io;

import java.lang.reflect.Constructor;

/**
 * Convert String-to-POJO via a constructor
 * @author daniel
 *
 */
public class SerializeViaConstructor implements ISerialize {

	private Class type;
	private Constructor cons;

	public SerializeViaConstructor(Class type) throws NoSuchMethodException, SecurityException {
		cons = type.getConstructor(String.class);
		this.type = type;
	}

	@Override
	public boolean canConvert(Class klass) {
		return type.equals(klass);
	}

	@Override
	public Object fromString(String v) throws Exception {
		return cons.newInstance(v);
	}

	@Override
	public String toString(Object value) {
		return value.toString();
	}

	@Override
	public String toString() {
		return "SerializeViaConstructor[type=" + type + "]";
	}

}
