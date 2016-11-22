package winterwell.bob.tasks;


import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import winterwell.bob.BuildTask;
import winterwell.utils.reporting.Log;

/**
 * Create a PS database.
 * Assumes: the user worker exists with the winterwell password. Otherwise
 * we can't connect.
 * @author daniel
 *
 */
public class CreatePostgresDatabase extends BuildTask {
	
	private String driverClass = "org.postgresql.Driver";
	private final String databaseName;
	
	static String username = "worker";
	static String password = "winterwell";

	
	private boolean destroyIfExists = false;
	
	public CreatePostgresDatabase(String databaseName) {
		this.databaseName = databaseName;
	}
	
	@Override
	public void doTask() throws Exception {		
		Class.forName(driverClass).newInstance();
		Connection con = null;
		try {
			con = getConnection();
			// Does the database already exist?
			PreparedStatement s = con.prepareStatement("select * from pg_database");
			s.execute();
			ResultSet rs = s.getResultSet();
			while (rs.next()) {
				Object obj = rs.getObject(1);
				if (databaseName.equals(obj)) {
					Log.report(databaseName+" already exists");
					if ( ! destroyIfExists) return;
					destroyDatabase(con);					
				}
			}
			// create database
			PreparedStatement s2 = con.prepareStatement(
				"CREATE DATABASE "+databaseName +" WITH OWNER = worker ENCODING = 'UTF8';\n"
				+"GRANT ALL ON DATABASE "+databaseName+" TO public;\n"
				+"GRANT ALL ON DATABASE "+databaseName+" TO worker;\n");
			s2.execute();
		} finally {
			if (con!=null) con.close();
		}
	}
	
	
	public void setDestroyIfExists(boolean destroyIfExists) {
		this.destroyIfExists = destroyIfExists;
	}
	
	private void destroyDatabase(Connection con) throws SQLException {
		Log.report("DROPPING DATABASE: "+databaseName);
		PreparedStatement s2 = con.prepareStatement(
				"DROP DATABASE "+databaseName +";");
		s2.execute();
	}

	/**
	 * Return a new connection.
	 */
	Connection getConnection() throws SQLException {
		Connection con = DriverManager.getConnection(getUrl());		
		return con;
	}

	private String getUrl() {
		return "jdbc:postgresql:template1?user="+username+"&password="+password;
	}


}
