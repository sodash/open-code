package com.winterwell.nlp.chat;

import java.util.Collection;
import java.util.Map;

import com.winterwell.utils.IProperties;
import com.winterwell.utils.Key;
import com.winterwell.utils.Printer;
import com.winterwell.utils.containers.ArrayMap;

public final class Thingy implements IProperties {

	private final Map<Key, Object> properties = new ArrayMap();

	@Override
	public <T> boolean containsKey(Key<T> key) {
		return get(key) != null;
	}

	@Override
	public <T> T get(Key<T> key) {
		Object v = properties.get(key);
		return (T) v;
	}

	@Override
	public Collection<Key> getKeys() {
		return properties.keySet();
	}

	@Override
	public boolean isTrue(Key<Boolean> key) {
		Boolean v = get(key);
		return v != null && v;
	}

	@Override
	public <T> T put(Key<T> key, T value) {
		if (value == null)
			return (T) properties.remove(key);
		else
			return (T) properties.put(key, value);
	}

	@Override
	public String toString() {
		return "Thingy[" + Printer.toString(properties) + "]";
	}
}
