/**
 *
 */
package winterwell.utils;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import winterwell.utils.containers.Properties;

/**
 * For doing type-safe multi-class maps. Can be sorted by key name.
 *
 * @see Properties
 * @see IProperties
 * @author daniel
 */
public class Key<T> implements Serializable, Comparable<Key> {

	/**
	 * A {@link Key} with a bit more information.
	 *
	 * @author daniel
	 *
	 * @param <T>
	 */
	public static class RichKey<T> extends Key<T> {
		private static final long serialVersionUID = 1L;
		public final String description;
		public final Class valueClass;

		public RichKey(String name, Class valueClass, String description) {
			super(name);
			assert valueClass != null;
			this.valueClass = valueClass;
			this.description = description;
		}

	}

	static final boolean ENFORCE_UNIQUENESS = false;
	private static final Set<String> names = new HashSet<String>();
	private static final long serialVersionUID = 1L;

	public final String name;

	public Key(String name) {
		assert name != null;
		this.name = name;
		if (ENFORCE_UNIQUENESS) {
			if (names.contains(name))
				throw new NotUniqueException(name);
			names.add(name);
		}
	}

	@Override
	public final int compareTo(Key o) {
		return name.compareTo(o.name);
	}

	/**
	 * Matches by name. Allows different sub-classes to match.
	 */
	@Override
	public final boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		// Allow different sub-classes to match
		if (!(obj instanceof Key))
			return false;
		final Key other = (Key) obj;
		return name.equals(other.name);
	}

	/**
	 * @param map
	 *            A map with String keys
	 * @return the value for the name of this key
	 */
	public T getFromMap(Map<String, ?> map) {
		return (T) map.get(name);
	}

	/**
	 * @return the name of this key
	 */
	public final String getName() {
		return name;
	}

	@Override
	public final int hashCode() {
		final int prime = 31;
		int result = prime + name.hashCode();
		return result;
	}

	/**
	 * The name of the Key.
	 */
	@Override
	public final String toString() {
		return name;
	}

}
