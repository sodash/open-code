package com.winterwell.utils.io;

import java.sql.Connection;
import java.sql.SQLException;

import com.jolbox.bonecp.BoneCP;
import com.jolbox.bonecp.BoneCPConfig;
import com.winterwell.utils.log.Log;

/**
 * For using the BoneCP database connection pool with {@link SqlUtils}
 *
 * @author daniel
 */
public class BoneCPPool implements SqlUtils.IPool {

	private BoneCP connectionPool;
	private BoneCPConfig config;

	/**
	 * Any edits to the config must be made *before* {@link #getConnection()} is
	 * first called.
	 *
	 * @return
	 */
	public BoneCPConfig getConfig() {
		return config;
	}

	@Override
	public String getURL() {
		return config.getJdbcUrl();
	}

	@Override
	public String getUsername() {
		return config.getUsername();
	}

	public BoneCPPool(String dbUrl, String userName, String password) {
		config = new BoneCPConfig(); // create a new configuration object
		config.setJdbcUrl(dbUrl); // set the JDBC url
		if (userName != null)
			config.setUsername(userName); // set the username
		if (password != null)
			config.setPassword(password); // set the password
		// Defaults may be sensible: https://github.com/wwadge/bonecp/blob/master/bonecp/src/main/java/com/jolbox/bonecp/BoneCPConfig.java
		// But just in case...
		Log.d("BoneCP", "Paritions: " + config.getPartitionCount());
		Log.d("BoneCP", "MaxConnectionsPerPartition: " + config.getMaxConnectionsPerPartition());
	}

	public BoneCPPool(DBOptions dboptions) {
		this(dboptions.dbUrl, dboptions.dbUser, dboptions.dbPassword);
	}

	@Override
	public Connection getConnection() throws SQLException {
		if (connectionPool == null) {
			connectionPool = new BoneCP(config); // setup the connection pool
		}
		return connectionPool.getConnection();
	}

	@Override
	public void close() {
		if (connectionPool != null) connectionPool.shutdown();
	}
}
