package winterwell.web;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import winterwell.utils.IProperties;
import winterwell.utils.Key;
import com.winterwell.utils.containers.ArrayMap;

/**
 * Login details for a service such as smtp. These objects should contain the
 * info needed to open a connection (e.g. password, port number, etc).
 * 
 * @author daniel
 * 
 */
public class LoginDetails implements IProperties, Serializable {
	@SuppressWarnings("rawtypes")
	private final static Map<Key, Object> defaultProperties = new HashMap<Key, Object>();

	private static final long serialVersionUID = 1L;

	public final String loginName;

	public final String password;

	/**
	 * 0 for unset (use the service default)
	 */
	public final int port;

	@SuppressWarnings("rawtypes")
	private final Map<Key, Object> properties = new ArrayMap<Key, Object>();

	/**
	 * Server or service (e.g. twitter.com)
	 */
	public final String server;

	public LoginDetails(String server) {
		this(server, 0);
	}

	public LoginDetails(String server, int port) {
		assert server != null;
		this.server = server;
		loginName = null;
		password = null;
		this.port = port;
	}

	/**
	 * @param server
	 *            Server or service (e.g. twitter.com)
	 */
	public LoginDetails(String server, String loginName, String password) {
		this(server, loginName, password, 0);
	}

	/**
	 * @param server
	 * @param loginName
	 * @param password
	 * @param port
	 */
	public LoginDetails(String server, String loginName, String password,
			int port) {
		// actually this isn't always required
		// if ( ! "localhost".equalsIgnoreCase(server)) {
		// Utils.check4null(server, loginName, password);
		// }
		this.server = server;
		assert !server.contains(" ") && !server.contains("\n") : server; // whitespace
																			// is
																			// bad
		this.loginName = loginName;
		this.password = password;
		this.port = port;
	}

	@Override
	public <T> boolean containsKey(Key<T> key) {
		return get(key) != null;
	}

	@Override
	public <T> T get(Key<T> key) {
		// TODO older versions of LoginDetails did not have a property bag
		Object v = properties == null ? null : properties.get(key);
		if (v == null) {
			v = defaultProperties.get(key);
		}
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
	@SuppressWarnings("unchecked")
	public <T> T put(Key<T> key, T value) {
		if (value == null)
			return (T) properties.remove(key);
		else
			return (T) properties.put(key, value);
	}

	public <T> void putDefault(Key<T> key, T value) {
		if (value == null) {
			defaultProperties.remove(key);
		} else {
			defaultProperties.put(key, value);
		}
	}

	@Override
	public String toString() {
		String s = loginName + "@" + server;
		if (port > 0) {
			s += ":" + port;
		}
		return s;
	}
}
