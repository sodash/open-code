package com.winterwell.web.fields;

import java.util.Collection;
import java.util.Map.Entry;

import com.winterwell.utils.Utils;
import com.winterwell.utils.containers.BiDiMap;

/**
 * A hidden field for encoding Java classes.
 * 
 * @author daniel
 * 
 */
public class ClassField extends AField<Class> {

	private static final long serialVersionUID = 1L;

	final BiDiMap<Class, String> class2name = new BiDiMap<Class, String>();

	public ClassField(String name) {
		super(name, "hidden");
	}

	/**
	 * Convert Strings to Classes, using a pretty-name mapping if one has been
	 * set. This is case insensitive i.e. "group" and "Group" will both return
	 * the same class. Otherwise needs a full class name for use with
	 * {@link Class#forName(String)}.
	 */
	@Override
	public Class fromString(String v) throws ClassNotFoundException {
		// pretty name?
		if (class2name != null) {
			Class c = class2name.getInverse(v);
			if (c != null)
				return c;
		}
		for (Entry<Class, String> e : class2name.entrySet()) {
			if (e.getValue().toLowerCase().equals(v))
				return e.getKey();
		}
		return Class.forName(v);
	}

	public Collection<Class> getPrettyClasses() {
		return class2name.keySet();
	}

	/**
	 * Map a class to a shorter nicer String (for human readable urls)
	 * 
	 * @param klass
	 * @param prettyName
	 * @throws IllegalStateException
	 *             if klass or prettyName already have a mapping set.
	 */
	public void setPrettyName(Class klass, String prettyName) {
		// check for conflicts
		if (class2name.containsKey(klass)) {
			if (Utils.equals(prettyName, class2name.get(klass)))
				return;
			throw new IllegalStateException("Cannot change the mapping for "
					+ klass);
		}
		if (class2name.containsValue(prettyName)) {
			if (Utils.equals(klass, class2name.getInverse(prettyName)))
				return;
			throw new IllegalStateException("Cannot change the mapping for "
					+ prettyName);
		}
		// set it
		class2name.put(klass, prettyName);
	}

	/**
	 * Convert Classes to Strings, using a pretty-name mapping if one is set.
	 * Otherwise returns a full class name.
	 */
	@Override
	public String toString(Class value) {
		String cName = class2name.get(value);
		if (cName != null)
			return cName;
		cName = value.getCanonicalName();
		if (cName == null)
			throw new UnsupportedOperationException(value
					+ " is an anonymous or local class.");
		return cName;
	}

	@Override
	public Class<Class> getValueClass() {
		return Class.class;
	}

}
