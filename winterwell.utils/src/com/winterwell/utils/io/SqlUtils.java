package com.winterwell.utils.io;

import java.io.InputStream;
import java.io.ObjectInputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.winterwell.utils.IFn;
import com.winterwell.utils.Printer;
import com.winterwell.utils.Proc;
import com.winterwell.utils.ReflectionUtils;
import com.winterwell.utils.StrUtils;
import com.winterwell.utils.Utils;
import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.utils.containers.Cache;
import com.winterwell.utils.containers.Containers;
import com.winterwell.utils.containers.Pair;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.time.Dt;
import com.winterwell.utils.time.TUnit;
import com.winterwell.utils.time.Time;
import com.winterwell.utils.time.TimeUtils;

class RSIterator implements Iterator<Object[]> {

	private int cols;
	private Boolean hasNext;
	private final ResultSet rs;
	private Runnable closeFn;

	public RSIterator(ResultSet rs, Runnable closeFn) {
		assert rs != null;
		this.rs = rs;
		this.closeFn = closeFn;
		try {
			cols = rs.getMetaData().getColumnCount();
		} catch (SQLException e) {
			throw Utils.runtime(e);
		}
	}

	private void advanceIfNeeded() {
		if (hasNext != null)
			return;
		try {
			hasNext = rs.next();
			// Close?
			if (!hasNext && closeFn != null) {
				rs.close();
				closeFn.run();
			}
		} catch (SQLException e) {
			throw Utils.runtime(e);
		}
	}

	@Override
	public boolean hasNext() {
		// handle repeated calls without repeated advances
		advanceIfNeeded();
		return hasNext;
	}

	@Override
	public Object[] next() {
		// do we need to advance? Not if #hasNext() was called just before
		advanceIfNeeded();
		// Either next or hasNext will now trigger an advance
		hasNext = null;
		try {
			Object[] row = new Object[cols];
			for (int i = 0; i < row.length; i++) {
				row[i] = rs.getObject(i + 1); // not zero indexed
			}
			return row;
		} catch (SQLException e) {
			throw Utils.runtime(e);
		}
	}

	@Override
	public void remove() {
		try {
			rs.deleteRow();
		} catch (SQLException e) {
			throw Utils.runtime(e);
		}
	}

}

/**
 * Static general purpose SQL utils
 *
 * @see JpaUtils
 * @author daniel
 * @testedby {@link SqlUtilsTest}
 */
public class SqlUtils {

	public synchronized static void shutdown() {
		init = false;
		if (pool != null) {
			pool.close();
			pool = null;
		}
	}
	
	/**
	 * HACK for debugging SoDash -- how many postgres threads are we running? How many are idle?
	 * @param name
	 * @return
	 */
	public static int[] getPostgresThreadInfo(String name) {
		Proc proc = new Proc("ps -ef "+name);
		proc.run();
		proc.waitFor(new Dt(10, TUnit.SECOND));
		String out = proc.getOutput();
		proc.close();
		String[] lines = StrUtils.splitLines(out);
		int cnt=lines.length, idlers=0;
		for (String line : lines) {
			if (line.contains("idle in transaction")) idlers++;
		}
		return new int[]{cnt,idlers};
	}

	public static DBOptions options;

	/**
	 * Convenience for {@link #getConnection(String, String, String)} using the static options.
	 * @return a connection with auto-commit OFF (this is needed for streaming
	 *         mode)
	 * @see #setDBOptions(DBOptions)
	 */
	public static Connection getConnection() {
		assert options != null && options.dbUrl != null : "No connection info! "
				+ options;
		return getConnection(options);
	}

	/**
	 *
	 * @return a connection with auto-commit OFF (this is needed for streaming
	 *         mode)
	 */
	public static Connection getConnection(DBOptions dboptions) {
		Utils.check4null(dboptions.dbUrl, dboptions.dbUser, dboptions.dbPassword);
		try {			
			String dbUrl = dboptions.dbUrl;
			initCommonJdbcDriver(dbUrl);

			Connection con;
			if (pool != null && dbUrl.equals(pool.getURL()) && Utils.equals(dboptions.dbUser, pool.getUsername())) {
				// Use the pool (which matches our setup)
				con = pool.getConnection();
			} else {
				// see https://jdbc.postgresql.org/documentation/head/connect.html#connection-parameters
				Properties props = new Properties();
				props.setProperty("user", dboptions.dbUser);
				props.setProperty("password", dboptions.dbPassword);
				if (Utils.yes(dboptions.ssl)) {
					props.setProperty("ssl", "true");
					props.setProperty("sslkey", dboptions.sslkey);
					props.setProperty("sslcert", dboptions.sslcert);
				}
				if (dboptions.loginTimeout!=0) props.setProperty("loginTimeout", ""+dboptions.loginTimeout);
				if (dboptions.connectTimeout!=0) props.setProperty("connectTimeout", ""+dboptions.connectTimeout);
				if (dboptions.socketTimeout!=0) props.setProperty("socketTimeout", ""+dboptions.socketTimeout);
				con = DriverManager.getConnection(dbUrl, props);
			}

			// This is needed for streaming mode, so set it so by default
			// -- you must then explicitly call commit!
			con.setAutoCommit(false);

			return con;
		} catch (Exception e) {
			throw Utils.runtime(e);
		}
	}

	/**
	 * @param select
	 * @param con
	 *            Can be null if options have been setup -- in which case a new
	 *            connection will be created & closed.
	 * @param max
	 *            0 for unlimited
	 * @return results
	 */
	public static Iterable<Object[]> executeQuery(String select,
			Connection con, int max) {
		boolean autoClose = false;
		try {
			if (con == null) {
				con = getConnection();
				autoClose = true;
			}
			Statement stmnt = con.createStatement();
			if (max > 0)
				stmnt.setMaxRows(max);
			ResultSet rs = stmnt.executeQuery(select);
			Iterable<Object[]> rsit = asIterable(rs, null);
			if (autoClose) {
				// copy out now, so we can close the connection
				return Containers.getList(rsit);
			}
			return rsit;
		} catch (Exception e) {
			Log.w("db", select+" -> "+Utils.getRootCause(e));
			throw Utils.runtime(e);
		} finally {
			if (autoClose)
				SqlUtils.close(con);
		}
	}

	/**
	 * @param select
	 * @param con
	 *            Can be null if options have been setup -- in which case a new
	 *            connection will be created & closed.
	 *            Will be committed, unless this connection has auto-commit on.
	 * @return results
	 */
	public static int executeUpdate(String sql, Connection con) {
		boolean autoClose = false;
		Statement stmnt = null;
		try {
			if (con == null) {
				con = getConnection();
				autoClose = true;
			}
			stmnt = con.createStatement();
			int rs = stmnt.executeUpdate(sql);
			if ( ! con.getAutoCommit()) {
				con.commit();
			}
			Log.i("sql", sql);
			return rs;
		} catch (Exception e) {
			Log.report("db", Utils.getRootCause(e), Level.WARNING);
			throw Utils.runtime(e);
		} finally {
			SqlUtils.close(stmnt);
			if (autoClose)
				SqlUtils.close(con);
		}
	}

	/**
	 * @param select
	 * @param con
	 *            Can be null if options have been setup -- in which case a new
	 *            connection will be created & closed.
	 * @return results
	 */
	public static boolean executeCommand(String sql, Connection con,
			boolean swallowExceptions) 
	{
		boolean autoClose = false;
		Statement stmnt = null;
		try {
			if (con == null) {
				con = getConnection();
				autoClose = true;
			}			
			stmnt = con.createStatement();
			boolean rs = stmnt.execute(sql);
			if (!con.getAutoCommit())
				con.commit();
			return rs;
		} catch (Exception e) {
			Log.w("db", sql+" -> "+Utils.getRootCause(e));
			if (swallowExceptions) {
				// close so the next command can start afresh
				if ( ! autoClose) {
					SqlUtils.close(con);	
				}			
				return false;
			}
			throw Utils.runtime(e);
		} finally {
			if (autoClose)
				SqlUtils.close(con);
		}
	}

	/**
	 * table name is lower-cased.
	 *
	 * @see SqlUtils#getTableColumns(String)
	 */
	static final Cache<String, List<Pair<String>>> table2columns = new Cache(
			100);
	
	private static final String LOGTAG = "sql";

	/**
	 * Provides streaming row-by-row access, if the resultset was created for
	 * that. Or just convenient iteration.<br>
	 * To get streaming: <br>
	 * - connection.setAutoCommit(false); (NB: getConnection() does this for
	 * you) <br>
	 * - statement.setFetchSize(1);<br>
	 *
	 * WARNING: This iterable can only be looped over once! WARNING: You must
	 * close the database resources after use!
	 *
	 * @param rs
	 * @return
	 */
	public static Iterable<Object[]> asIterable(final ResultSet rs) {
		return asIterable(rs, null);
	}

	/**
	 * Provides streaming row-by-row access, if the resultset was created for
	 * that. Or just convenient iteration.<br>
	 * To get streaming: <br>
	 * - connection.setAutoCommit(false); (NB: getConnection() does this for
	 * you) <br>
	 * - statement.setFetchSize(1);<br>
	 *
	 * WARNING: This iterable can only be looped over once!
	 *
	 * @param rs
	 * @param closeFn
	 *            Called when the resultset reaches the end.
	 * @return
	 */
	public static Iterable<Object[]> asIterable(final ResultSet rs,
			final Runnable closeFn) {
		return new Iterable<Object[]>() {			
			boolean fresh = true;

			@Override
			public Iterator<Object[]> iterator() {
				// You can only loop once!
				assert fresh;
				fresh = false;
				return new RSIterator(rs, closeFn);
			}
		};
	}

	public static void close(Statement statement) {
		if (statement == null)
			return;
		try {
			statement.close();
		} catch (SQLException e) {
			throw Utils.runtime(e);
		}
	}

	/**
	 *
	 * @param col
	 * @param cols
	 * @return -1 if not found
	 */
	static int getColumnIndex(String col, List<Pair<String>> cols) {
		assert cols != null;
		for (int i = 0; i < cols.size(); i++) {
			Pair<String> pair = cols.get(i);
			if (pair.first.equalsIgnoreCase(col))
				return i;
		}
		return -1;
	}

	/**
	 * Set a connection pool (e.g. use {@link BoneCPPool})
	 *
	 * @param pool
	 */
	public synchronized static void setPool(IPool pool) {
		if (pool != null) {
			pool.close();
		}
		SqlUtils.pool = pool;
	}

	/**
	 * TODO ??How to use the connection pool?? (we favour bonecp over c3p0)
	 *
	 * @param dbUrl
	 * @param user
	 * @param password
	 * @return a connection with auto-commit OFF (this is needed for streaming
	 *         mode)
	 */
	public static Connection getConnection(String dbUrl, String user,
			String password) 
	{
		DBOptions dbo = new DBOptions();
		dbo.dbUrl = dbUrl;
		dbo.dbUser = user;
		dbo.dbPassword = password;
		return getConnection(dbo);
	}

	private static boolean init;
	private static IPool pool;

	/**
	 * Load the Postgres/MySQL/etc driver if needed
	 *
	 * @param dbUrl
	 * @throws Exception
	 */
	public static void initCommonJdbcDriver(String dbUrl) throws Exception {
		if (dbUrl == null) throw new NullPointerException();
		if (dbUrl.startsWith("jdbc:postgresql:")) { // PostgreSQL
			Class.forName("org.postgresql.Driver");
		} else if (dbUrl.startsWith("jdbc:mysql:")) { // MySQL
			Class.forName("com.mysql.jdbc.Driver");
		} else if (dbUrl.startsWith("jdbc:h2:")) { // H2
			// H2 is a lightweight pure Java db. Useful for testing.
			Class.forName("org.h2.Driver");
		} else {
			Log.report("sql", "Unrecognised DB " + dbUrl, Level.WARNING);
		}
	}

	/**
	 * @param con
	 * @param tbl
	 * @return List of (name,type)s
	 */
	public static List<Pair<String>> getTableColumns(Connection con, String tbl) {
		Statement statement = null;
		try {
			statement = con.createStatement();
			statement.setMaxRows(1);
			ResultSet rs = statement.executeQuery("select * from " + tbl);
			ResultSetMetaData meta = rs.getMetaData();
			List<Pair<String>> list = new ArrayList();
			for (int i = 0; i < meta.getColumnCount(); i++) {
				String name = meta.getColumnName(i + 1);
				String type = meta.getColumnTypeName(i + 1);
				Pair p = new Pair(name, type);
				list.add(p);
			}
			return list;
		} catch (SQLException e) {
			throw Utils.runtime(e);
		} finally {
			close(statement);
		}
	}



	/**
	 * A crude imitation of what Hibernate does: mapping database rows into Java
	 * objects.
	 * @author daniel
	 *
	 * @param <X> The output type, e.g. DBText
	 */
	static class InflateFn<X> implements IFn<Object[], X> {
		final Constructor<X> con;
		final Field[] fields;

		public InflateFn(Class<X> klass, Field[] fields) throws NoSuchMethodException, SecurityException {
			con = klass.getDeclaredConstructor();
			con.setAccessible(true);
			this.fields = fields;
		}

		@Override
		public X apply(Object[] row) {
			try {
				X x = con.newInstance();
				for (int i = 0; i < row.length; i++) {
					Field f = fields[i];
					if (f == null) {
						continue;
					}
					Object val = row[i];
					// conversions!
					val = inflate2_convert(val, f.getType());
					f.set(x, val);
				}
				return x;
			} catch (Exception e) {
				throw Utils.runtime(e);
			}
		}
	}

	/**
	 * A crude imitation of what Hibernate does: mapping database rows into Java
	 * objects.
	 *
	 * @param select
	 *            The select clause. E.g. "x.xid,x.contents". This will be split
	 *            to WARNING: assumes you use x. to refer to the main object.
	 * @param rs
	 *            Database rows
	 * @param klass
	 * @return
	 */
	public static <X> Iterable<X> inflate(String select, Iterable<Object[]> rs,
			final Class<X> klass) {
		// x.id, x.contents, etc.
		final Field[] fields = inflate2_whichColumns(select, klass);
		try {
			return Containers.applyLazy(rs, new InflateFn<X>(klass, fields));

		} catch (Exception e1) {
			throw Utils.runtime(e1);
		}
	}

	private static Object inflate2_convert(Object val, Class type)
			throws Exception {
		if (val == null)
			return null;
		Class<? extends Object> vc = val.getClass();
		if (vc == type)
			return val;
		// numbers
		if (vc == BigInteger.class) {
			if (type == Long.class)
				return ((BigInteger) val).longValue();
			if (type == Double.class)
				return ((BigInteger) val).doubleValue();
		}
		// enums
		if (ReflectionUtils.isa(type, Enum.class)) {
			Object[] ks = type.getEnumConstants();
			assert ks != null : type;
			if (val instanceof Integer || val instanceof Long) {
				int i = (Integer) val;
				return ks[i];	
			}
			String sval = (String) val;
			for (Object k : ks) {
				if (k.toString().equals(sval)) {
					return k;
				}
			}
			throw new IllegalArgumentException("Unrecognised enum value: "+val+" for "+type);
		}
		// time
		if (type==Time.class) {
			return new Time(val.toString());
		}
		// exceptions
		if (ReflectionUtils.isa(type, Throwable.class)) {
			byte[] bytes = (byte[]) val;
			InputStream in = new FastByteArrayInputStream(bytes, bytes.length);
			ObjectInputStream objIn = new ObjectInputStream(in);
			Object data = objIn.readObject();
			objIn.close();
			return data;
		}
		// How to handle JPA entities? We can't :(
		return val;
	}

	/**
	 * Assume column-names = field-names, and we use "x.field" to refer to them
	 * in the select!
	 *
	 * @param select
	 * @param klass
	 * @return
	 */
	static <X> Field[] inflate2_whichColumns(String select, Class<X> klass) {
		// We just want the top-level selected columns
		Pattern pSelectColsFrom = Pattern.compile("^select\\s+(.+?)\\s+from",
				Pattern.CASE_INSENSITIVE);
		Matcher m = pSelectColsFrom.matcher(select);
		if (m.find()) {
			select = m.group(1);
		}

		// NB: these are "x.field"
		// Build map of column-num to Field
		String[] cols = select.split(",");
		Field[] fields = new Field[cols.length];
		for (int i = 0; i < fields.length; i++) {
			if (!cols[i].startsWith("x.")) {
				// ignore it?!
				// This allows for other columns to be mixed in
			}
			String col = cols[i].substring(2);
			Field field = ReflectionUtils.getField(klass, col);
			if (field == null) {
				// ignore it?!
				// This allows for other columns to be mixed in
				continue;
			}
			fields[i] = field;
		}
		// open up the security
		if (fields.length != 0) {
			Field.setAccessible(fields, true);
		}
		return fields;
	}

	/**
	 * Encode the text to escape any SQL characters, and add surrounding 's.
	 * This is for use as String constants -- not for use in LIKE statements
	 * (which need extra escaping).
	 *
	 * @param text
	 *            Can be null (returns "null").
	 * @return e.g. don't => 'don''t'
	 */
	public static String sqlEncode(String text) {
		if (text == null)
			return "null";
		text = "'" + text.replace("'", "''") + "'";
		return text;
	}

	public static String sqlEncodeNoQuote(String text) {
		if (text == null)
			return "null";
		text = text.replace("'", "''");
		return text;
	}

	static void upsert2_insert(String table, Map<String, ?> col2val,
			boolean specialCaseId, List<Pair<String>> columnInfo,
			CharSequence whereClause, StringBuilder upsert) {
		upsert.append("insert into " + table + " select ");
		// Build what we put into each column
		for (Pair<String> colInfo : columnInfo) {
			// SoDash/Hibernate HACK: ignore any given id value & use the local
			// sequence? NB: this always sets a not-null id
			if (specialCaseId && "id".equals(colInfo.first)) {
				upsert.append("nextval('hibernate_sequence'),");
				continue;
			}

			// Keep Hibernate happy - avoid oid and setParameter with null
			if ("oid".equals(colInfo.second)
					|| col2val.get(colInfo.first) == null) {
				upsert.append("null,");
				continue;
			}
			// a normal insert
			upsert.append(":" + colInfo.first + ",");
		}
		StrUtils.pop(upsert, 1);
		// block if already present
		upsert.append(" where not exists (select 1 from " + table + whereClause
				+ ");");
	}

	/**
	 * @param idColumns
	 * @param col2val
	 * @return the identifying where clause
	 */
	static StringBuilder upsert2_where(String[] idColumns,
			Map<String, ?> col2val) {
		StringBuilder whereClause = new StringBuilder(" where (");
		for (int i = 0; i < idColumns.length; i++) {
			String col = idColumns[i];
			// SQL hack: " is null" not "=null"
			if (col2val.get(col) == null) {
				whereClause.append(col + " is null and ");
				continue;
			}
			whereClause.append(col + "=:" + col + " and ");
		}
		StrUtils.pop(whereClause, 4);
		whereClause.append(")");
		return whereClause;
	}

	/**
	 * Does NOT explicitly commit
	 *
	 * @param con
	 *            Can be null
	 */
	public static void close(Connection con) {
		if (con == null)
			return;
		try {
			con.close();
		} catch (SQLException e) {
			// swallow
			throw Utils.runtime(e);
		}
	}

	/**
	 * @param rs
	 *            Can be null
	 */
	public static void close(ResultSet rs) {
		if (rs == null)
			return;
		try {
			rs.close();
		} catch (SQLException e) {
			throw Utils.runtime(e);
		}
	}

	/**
	 * Set static database connection options.
	 * Obviously this won't do if you need to connect to multiple databases, but for most programs, that's fine.
	 * @param options Can be null to clear the options
	 */
	public static void setDBOptions(DBOptions options) {
		assert (SqlUtils.options == null 
				|| ReflectionUtils.equalish(SqlUtils.options, options)) : "Incompatible setup for SqlUtils static db connection.";
		SqlUtils.options = options;
		if (options!=null) {
			assert options.dbUrl != null : options;
		}
	}

	/**
	 * Update where exists.
	 *
	 * @param table
	 * @param col2val
	 * @param specialCaseId
	 * @param columnInfo
	 * @param whereClause
	 * @param upsert
	 * @param leaveMissingAlone 
	 * @return "update table set stuff where whereClause;"
	 */
	static void upsert2_update(String table, Map<String, ?> col2val,
			boolean specialCaseId, List<Pair<String>> columnInfo,
			StringBuilder whereClause, StringBuilder upsert, boolean leaveMissingAlone) 
	{
		// ... create a=:a parameters
		upsert.append("update " + table + " set ");
		for (Pair<String> colInfo : columnInfo) {
			// SoDash/Hibernate HACK: don't change the id!
			if (specialCaseId && "id".equals(colInfo.first)) {
				continue;
			}
			
			Object v = col2val.get(colInfo.first);
			if (v == null && leaveMissingAlone) {
				continue;
			}
			// Keep Hibernate happy - avoid oid and setParameter with null
			if ("oid".equals(colInfo.second) || v == null) {				
				upsert.append(colInfo.first + "=null,");
				continue;
			}

			// a normal update
			upsert.append(colInfo.first + "=:" + colInfo.first + ",");
		}
		// lose the trailing ,
		if (upsert.charAt(upsert.length() - 1) == ',') {
			StrUtils.pop(upsert, 1);
		}
		upsert.append(whereClause);
		upsert.append(";\n");
	}

	/**
	 * @param time
	 * @return 'timestamp' for direct injection into an sql query. If you're
	 *         using Query.setParameter, just use a Date, e.g. Time.getDate().
	 */
	public static String timestamp(Time time) {
		long l = time.getTime();
		Timestamp timestampRaw = new Timestamp(l);
		String timestamp = timestampRaw.toString();
		return '\'' + timestamp + '\'';
	}

	/**
	 * Create a partial index which only covers ~ a month. Call this every month
	 * to drop the old indexes and create a new one.
	 *
	 * @param table
	 * @param indexedColumn
	 * @param timestampColumn
	 */
	public static void createMonthlyIndex(String table, String indexedColumn,
			String timestampColumn) {
		// Go back a month, and then to the start of that month
		Time start = TimeUtils.getStartOfMonth(new Time()).minus(TUnit.MONTH);
		// drop some earlier ones TODO get a list of indexes instead
		Time old = start;
		for (int i = 0; i < 3; i++) {
			old = old.minus(TUnit.MONTH); // do this first, so we don't nuke the
											// current index
			String mmyy = old.format("MM_yy");
			Log.d("sql.index", "Dropping if it exists: monthly index for "
					+ table + " " + mmyy);
			boolean ok = executeCommand("drop index " + table + "_monthly_"
					+ mmyy + "_idx", null, true);
		}
		Connection con = getConnection();
		try {
			// What type of timestamp? a Java Long or an SQL Timestamp?
			Class type = null;
			List<Pair<String>> cols = getTableColumns(con, table);
			for (Pair<String> pair : cols) {
				if (!pair.first.equalsIgnoreCase(timestampColumn))
					continue;
				String _type = pair.second.toLowerCase();
				if (_type.startsWith("timestamp")) {
					type = Timestamp.class;
				} else if (_type.equals("long")) {
					type = Long.class;
				} else if (_type.equals("bigint")) {
					type = Long.class;
				}
				break;
			}

			String _start = type == Timestamp.class ? timestamp(start) : Long
					.toString(start.getTime());
			String mmyy = start.format("MM_yy");
			boolean ok = executeCommand("create index " + table + "_monthly_"
					+ mmyy + "_idx on " + table + " (" + indexedColumn
					+ ") where " + timestampColumn + ">" + _start, con, true);
			if (ok)
				Log.i("sql.index", "Created monthly index for " + table + " "
						+ mmyy);
			else
				Log.i("sql.index", "monthly index for " + table + " " + mmyy
						+ " already exists");
		} finally {
			close(con);
		}
	}

	public static <X> Iterable<X> inflate(Iterable<Object[]> rows,
			Class<X> klass) {
		String select = selectAllFields(klass, "x");
		return inflate(select, rows, klass);
	}

	/**
	 * The guts of what columns are selected ("select" not included, nor the
	 * "from ... where..." stuff)
	 *
	 * @param klass
	 * @param var
	 *            e.g. "x" if using "from DBText x"
	 * @return e.g. "x.name,x.id"
	 */
	public static String selectAllFields(Class klass, String var) {
		List<Field> fields = ReflectionUtils.getAllFields(klass);
		StringBuilder select = new StringBuilder();
		for (Field field : fields) {
			if (ReflectionUtils.isTransient(field))
				continue;
			if (ReflectionUtils.hasAnnotation(field.getType(), "Entity")) {
				select.append(var + "." + field.getName() + "_id,");
			} else {
				select.append(var + "." + field.getName() + ",");
			}
		}
		StrUtils.pop(select, 1);
		String s = select.toString();
		return s;
	}

	
	public static int insert(Map<String,Object> item, String table, Connection con) {
		// half-hearted anti-injection check
		assert !table.contains(";") && !table.contains("--") : table;
		boolean autoClose = false;
		try {
			if (con == null) {
				con = getConnection();
				autoClose = true;
			}
			Statement stmnt = con.createStatement();

//			List<Field> fields = ReflectionUtils.getAllFields(row.getClass());
//			fields = Containers.filter(fields, f -> ! ReflectionUtils.isTransient(f));
//			StringBuilder select = new StringBuilder();
			ArrayList<String> keys = new ArrayList(item.keySet());
			String cols = StrUtils.join(keys, ",");
			StringBuilder values = new StringBuilder();
			for(String f : keys) {
				Object v = item.get(f);
				if (v == null) { values.append("null,"); continue; }
				if (v instanceof Number) values.append(v+",");
				else if (v instanceof Boolean) values.append(v+",");
				else if (v instanceof String) values.append(SqlUtils.sqlEncode((String)v)+",");
				else {
					values.append(SqlUtils.sqlEncode(v.toString())+",");
				}
			}
			StrUtils.pop(values, 1);
			String sql = "insert into " + table + " (" + cols + ") values ("
					+ values + ");";
//			if (true)
//				throw new TodoException();
			int rs = stmnt.executeUpdate(sql);
			if (!con.getAutoCommit())
				con.commit();
			return rs;
		} catch (Exception e) {
			Log.report("db", Utils.getRootCause(e), Level.WARNING);
			throw Utils.runtime(e);
		} finally {
			if (autoClose)
				SqlUtils.close(con);
		}
	}

	public static interface IPool {

		String getURL();
		String getUsername();
		
		Connection getConnection() throws SQLException;

		void close();
	}

	/**
	 * Convenience for single-result result sets.
	 *
	 * @param rs
	 * @return The first row, or null if empty
	 */
	public static Object[] getOneRow(ResultSet rs) {
		Iterator<Object[]> rit = asIterable(rs).iterator();
		return rit.hasNext() ? rit.next() : null;
	}

	/**
	 * Commit and close.
	 *
	 * @param conn
	 *            Can be null (does nothing)
	 * @throws RuntimeException
	 *             If either operation fails. But close is always called, even
	 *             if commit fails.
	 */
	public static void commmitAndClose(Connection conn) throws RuntimeException {
		if (conn == null)
			return;
		Exception ex = null;
		try {
			conn.commit();
		} catch (Exception e) {
			ex = e;
		}
		try {
			conn.close();
		} catch (Exception e) {
			if (ex == null)
				ex = e;
			// forget this 2nd exception -- report the first one
		}
		if (ex != null)
			throw Utils.runtime(ex);
	}

	/**
	 * Convenience wrapper for {@link Connection#isClosed()})
	 * @param _connection Can be null (returns true).
	 * @return true if closed (or throws an exception), false if _probably_ alive and well
	 * (but no guarantees -- see {@link Connection#isClosed()}).
	 * 
	 */
	public static boolean isClosed(Connection _connection) {
		if (_connection==null) return true;
		try {
			return _connection.isClosed();
		} catch (SQLException e) {
			Log.w(LOGTAG, e);
			return true;
		}
	}
	
	
	

	/**
	 * upsert = update if exists + insert if new
	 * 
	 * @param table
	 * @param idColumns
	 *            The columns which identify the row. E.g. the primary key. Must
	 *            not contain any nulls. Should be a subset of columns.
	 * @param col2val
	 *            The values to set for every non-null column. Any missing
	 *            columns will be set to null.
	 * @param specialCaseId
	 *            If true, the "id" column is treated specially --
	 *            nextval(hibernate_sequence) is used for the insert, and no
	 *            change is made on update. This is a hack 'cos row ids don't
	 *            travel across servers.
	 * @return An upsert query, with it's parameters set.
	 */
	public static Object upsert(Connection em, String table,
			String[] idColumns, Map<String, ?> col2val, boolean specialCaseId) 
	{
		return upsert(em, table, idColumns, col2val, specialCaseId, false, null);
	}
		

	/**
	 * upsert = update if exists + insert if new
	 * 
	 * @param table
	 * @param idColumns
	 *            The columns which identify the row. E.g. the primary key. Must
	 *            not contain any nulls. Should be a subset of columns.
	 * @param col2val
	 *            The values to set for every non-null column. Any missing
	 *            columns will be set to null.
	 * @param specialCaseId
	 *            If true, the "id" column is treated specially --
	 *            nextval(hibernate_sequence) is used for the insert, and no
	 *            change is made on update. This is a hack 'cos row ids don't
	 *            travel across servers.
	 * @param leaveMissingAlone If true, then update will only affect those columns which are specified in col2val
	 * (by default a missing column sets the database value to null).  
	 * @param insertOnlyCol2val Allow for initial defaults to be set on insert. Can be null.
	 * @return output from {@link #executeUpdate(String, Connection)}
	 */
	public static int upsert(Connection em, String table,
			String[] idColumns, Map<String, ?> col2val, boolean specialCaseId, 
			boolean leaveMissingAlone, Map<String, ?> insertOnlyCol2val) 
	{	
		List<Pair<String>> columnInfo = upsert2_columnInfo(em, table,
				idColumns, col2val, specialCaseId);

		StringBuilder whereClause = SqlUtils.upsert2_where(idColumns, col2val);

		StringBuilder upsert = new StringBuilder();

		// 1. update where exists
		SqlUtils.upsert2_update(table, col2val, specialCaseId, columnInfo,
				whereClause, upsert, leaveMissingAlone);

		// 2. insert where not exists
		// Allow for initial defaults to be set on insert
		Map<String, Object> insertCol2val = (Map) col2val;
		if (insertOnlyCol2val != null) {
			insertCol2val = new HashMap(insertOnlyCol2val);
			insertCol2val.putAll(col2val);
		}
		SqlUtils.upsert2_insert(table, insertCol2val, specialCaseId, columnInfo,
				whereClause, upsert);

		// HACK sub vars TODO refactor to not use vars 
		// This code here is not 100% safe! 
		String sql = upsert.toString();
		for(String k : insertCol2val.keySet()) {
			Object v = insertCol2val.get(k);
			String vs = sqlEncode(v);
			sql = sql.replaceAll(":\\b"+k+"\\b", vs);
		}
		
		// do it		
		return executeUpdate(sql, em);
	}

	/**
	 * Check the type of object, and encode appropriately.
	 * 
	 * See also: {@link #sqlEncode(String)} which this will use for Strings only.
	 *
	 * @param v Can be null (returns "null").
	 * @return e.g. don't => 'don''t', 7 => 7
	 */
	public static String sqlEncode(Object v) {
		if (v instanceof String) return SqlUtils.sqlEncode((String)v);
		if (v==null) return "null";
		if (v instanceof Number) return v.toString();
		if (v.getClass().isEnum()) return SqlUtils.sqlEncode(v.toString());
		// Not a String / number? :( Hopefully a boolean or something
		// TODO (sep 2019) block this (but old code risk)
		Log.w(LOGTAG, "sqlEncode non-String "+v.getClass()+" = "+v);
		return v.toString();		
	}

//	static void upsert2_setParams(Map<String, ?> col2val,
//			boolean specialCaseId, List<Pair<String>> columnInfo, Query q) {
//		for (Pair<String> colInfo : columnInfo) {
//			Object v = col2val.get(colInfo.first);
//			// did this column get ignored?
//			if ("oid".equals(colInfo.second) || v == null) {
//				continue;
//			}
//			if (specialCaseId && "id".equals(colInfo.first)) {
//				continue;
//			}
//			// set param
//			q.setParameter(colInfo.first, v);
//		}
//	}

//	/**
//	 * Half an upsert: update-if-exists Does nothing if the row does not exist.
//	 * 
//	 * Yes this is hacky. Got any better ideas?
//	 * 
//	 * @param idColumns
//	 *            The columns which identify the row. E.g. the primary key. Must
//	 *            not contain any nulls. Should be a subset of columns.
//	 * @param col2val
//	 *            The values to set for every non-null column. Any missing
//	 *            columns will be set to null.
//	 * @param specialCaseId
//	 *            If true, the "id" column is treated specially --
//	 *            nextval(hibernate_sequence) is used for the insert, and no
//	 *            change is made on update. This is a hack 'cos row ids don't
//	 *            travel across servers. Normally false.
//	 * @return An update-where query, with it's parameters set.
//	 */
//	public static Query update(Connection em, String table,
//			String[] idColumns, Map<String, ?> col2val, boolean specialCaseId) {
//		List<Pair<String>> columnInfo = upsert2_columnInfo(em, table,
//				idColumns, col2val, specialCaseId);
//
//		StringBuilder whereClause = SqlUtils.upsert2_where(idColumns, col2val);
//
//		StringBuilder upsert = new StringBuilder();
//
//		// 1. update
//		SqlUtils.upsert2_update(table, col2val, specialCaseId, columnInfo,
//				whereClause, upsert);
//
//		// create query
//		Query q = em.createNativeQuery(upsert.toString());
//
////		// set params
////		upsert2_setParams(col2val, specialCaseId, columnInfo, q);
//
//		return q;
//	}

//	/**
//	 * Half an upsert: insert-if-not-there. Does nothing if the row already
//	 * exists.
//	 * 
//	 * @param idColumns
//	 *            The columns which identify the row. E.g. the primary key. Must
//	 *            not contain any nulls. Should be a subset of columns.
//	 * @param col2val
//	 *            The values to set for every non-null column. Any missing
//	 *            columns will be set to null.
//	 * @param specialCaseId
//	 *            If true, the "id" column is treated specially --
//	 *            nextval(hibernate_sequence) is used for the insert, and no
//	 *            change is made on update. This is a hack 'cos row ids don't
//	 *            travel across servers. Normally false.
//	 * @return An insert-if-absent query, with it's parameters set.
//	 */
//	public static Query insertIfAbsent(Connection em, String table,
//			String[] idColumns, Map<String, ?> col2val, boolean specialCaseId) {
//		List<Pair<String>> columnInfo = upsert2_columnInfo(em, table,
//				idColumns, col2val, specialCaseId);
//
//		StringBuilder whereClause = SqlUtils.upsert2_where(idColumns, col2val);
//
//		StringBuilder upsert = new StringBuilder();
//
//		// 1. insert where not exists
//		SqlUtils.upsert2_insert(table, col2val, specialCaseId, columnInfo,
//				whereClause, upsert);
//
//		// create query
//		Query q = em.createNativeQuery(upsert.toString());
//
////		// set params
////		upsert2_setParams(col2val, specialCaseId, columnInfo, q);
//
//		return q;
//	}

	private static List<Pair<String>> upsert2_columnInfo(Connection con,
			String table, String[] idColumns, Map<String, ?> col2val,
			boolean specialCaseId) {
		List<Pair<String>> columnInfo = getTableColumns(con, table);
		// safety check inputs
		assert idColumns.length <= columnInfo.size();
		for (String idc : idColumns) {
			assert idc != null : Printer.toString(idColumns);
			int i = SqlUtils.getColumnIndex(idc, columnInfo);
			assert i != -1 : idc + " vs " + columnInfo;
		}
		if (specialCaseId) {
			assert ! Containers.contains("id", idColumns);
//			assert getColumnIndex(em, "id", table) != -1 : table + " = "
//					+ getTableColumns(em, table);
		} else {
//			assert ! col2val.containsKey("id") : col2val;
		}
		return columnInfo;
	}

	/**
	 * 
	 * @param conn
	 * @param table
	 * @param where
	 * @param props
	 * @param max 0 for unlimited
	 * @return
	 * @throws SQLException
	 */
	public static List<Map<String, Object>> select(
			Connection conn, String table, String where, List<String> props,
			int max
			) throws SQLException 
	{
		String sql = "select "+StrUtils.join(props, ",")+" from "+table+" where "+where;
		// call the DB
		Iterable<Object[]> rit = executeQuery(sql, conn, max);
		List<Map<String, Object>> maps = new ArrayList();
		for (Object[] row : rit) {
			ArrayMap map = new ArrayMap();
			for(int i=0; i<props.size(); i++) {
				String prop = props.get(i);
				map.put(prop, row[i]);
			}
			maps.add(map);
		}		
		return maps;
	}


}
