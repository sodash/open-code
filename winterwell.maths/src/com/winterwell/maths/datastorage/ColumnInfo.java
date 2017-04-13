package com.winterwell.maths.datastorage;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.winterwell.utils.IProperties;
import com.winterwell.utils.Key;
import com.winterwell.utils.ReflectionUtils;
import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.utils.containers.Range;

/**
 * @deprecated Merge with IColumnModel
 * 
 * Describe an attribute/column/dimension in a dataset. Sub-classes can provide
 * support for handling non-numerical data.
 * 
 * @param <X>
 *            The type of data this column stores, usually Double
 * @author Daniel
 * 
 */
public class ColumnInfo<X> implements IProperties {

	private final static Map<Key, Object> defaultProperties = new HashMap<Key, Object>();

	public static <T> void putDefault(Key<T> key, T value) {
		if (value == null) {
			defaultProperties.remove(key);
		} else {
			defaultProperties.put(key, value);
		}
	}

	private final String name;
	private final Map<Key, Object> properties = new ArrayMap<Key, Object>();

	/**
	 * No defined use. TODO sanity check columns for bad data
	 */
	Range range;

	private final Class<X> type;

	/**
	 * A numerical (Double) attribute.
	 * 
	 * @param name
	 */
	public ColumnInfo(String name) {
		this(name, (Class<X>) Double.class);
	}

	public ColumnInfo(String name, Class<X> klass) {
		this.name = name;
		this.type = klass;
		assert klass != double.class : "Use Double instead";
		assert klass == Double.class || getClass() != ColumnInfo.class : "override the convert methods";
	}

	@Override
	public boolean containsKey(Key key) {
		return get(key) != null;
	}

	/**
	 * Override if type != Double
	 * 
	 * @param x
	 * @return
	 */
	public X convertFromDouble(double x) {
		assert type == Double.class : type;
		return (X) Double.valueOf(x);
	}

	/**
	 * Override if type != Double
	 * 
	 * @param x
	 * @return
	 */
	public double convertToDouble(X x) {
		assert type == Double.class : type;
		return ((Number) x).doubleValue();
	}

	@Override
	public <T> T get(Key<T> key) {
		Object v = properties.get(key);
		if (v == null) {
			v = defaultProperties.get(key);
		}
		return (T) v;
	}

	@Override
	public Collection<Key> getKeys() {
		return properties.keySet();
	}

	/**
	 * @return name for this attribute. Can be null.
	 */
	public final String getName() {
		return name;
	}

	/**
	 * @return the class of raw data. Double by default. E.g. suppose we wish to
	 *         create vectors from String valued data, then this would return
	 *         String.
	 */
	public final Class<X> getType() {
		return type;
	}

	/**
	 * @return true if the attribute is numerical
	 */
	public boolean isNumerical() {
		return ReflectionUtils.isa(type, Number.class);
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

	/**
	 * No defined use as yet
	 * 
	 * @param range
	 */
	public void setRange(Range range) {
		this.range = range;
	}

	@Override
	public String toString() {
		return name + ":" + type.getSimpleName();
	}

}
