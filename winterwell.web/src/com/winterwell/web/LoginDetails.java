package com.winterwell.web;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import com.winterwell.utils.IProperties;
import com.winterwell.utils.Key;
import com.winterwell.utils.StrUtils;
import com.winterwell.utils.Utils;
import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.utils.io.FileUtils;
import com.winterwell.utils.web.WebUtils;

/**
 * Login details for a service such as smtp. These objects should contain the
 * info needed to open a connection (e.g. password, port number, etc).
 * 
 * NB: doesnt handle JWT
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
		this(server, null, null, port);
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
		server = server.trim();
//		// Does server specify a port?
//		if (server != null && WebUtils.URI(server).getPort() > 0) {
//			int p = WebUtils.URI(server).getPort();
//			if (port>0 && port!=p) throw new IllegalArgumentException("Port mismatch: "+server+" vs "+port);
//			port = p;
//		}		
		this.server = server;
		if (server==null || server.contains(" ") || server.contains("\n")) {
			throw new IllegalArgumentException("bad server ["+ server+"]"); 
		} 
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
		Object v = properties == null ? null : properties.get(key);
		if (v == null) {
			v = defaultProperties.get(key);
		}
		return (T) v;
	}

	/**
	 * DOes NOT include the fields, eg password!
	 */
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

	/**
	 * Load from a file
	 * @param propertiesFile
	 * @return
	 */
	public static LoginDetails load(File propertiesFile) {
		assert propertiesFile.isFile() : propertiesFile;		
		try {
			Properties props = new Properties();
			props.load(FileUtils.getReader(propertiesFile));
			String _port = props.getProperty("port");
			LoginDetails ld = new LoginDetails(
					props.getProperty("server"),
					// a bit flexible on case 
					Utils.or(props.getProperty("loginName"), props.getProperty("loginname")),
					props.getProperty("password"),
					_port==null? 0 : Integer.valueOf(_port)
			);
			
			// TODO other properties
			
			return ld;
		} catch (IOException e) {
			throw Utils.runtime(e);
		}
	}
}
