package com.winterwell.utils.io;

import java.io.InputStream;
import java.io.ObjectInputStream;
import java.lang.reflect.Field;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import com.winterwell.utils.Printer;
import com.winterwell.utils.ReflectionUtils;
import com.winterwell.utils.Utils;
import com.winterwell.utils.containers.Containers;
import com.winterwell.utils.containers.Pair;

/**
 * Static general purpose SQL / JPA utils.
 * 
 * @see SqlUtils This is separate to avoid the JPA dependency if not needed.
 * 
 * @author daniel
 * @testedby {@link SqlUtilsTest}
 */
public class JpaUtils {

	public static boolean existsTable(String table, EntityManager em) {
		try {
			List<Pair<String>> cols = getTableColumns(em, table);
			assert !cols.isEmpty();
			return true;
		} catch (IllegalArgumentException ex) {
			// it doesn't exist
			return false;
		}
	}

	/**
	 * 
	 * @param col
	 *            Case insensitive
	 * @param table
	 *            Case insensitive
	 * @param em
	 * @return -1 if not found
	 * @throws IllegalArgumentException
	 *             if the table does not exist
	 */
	public static int getColumnIndex(EntityManager em, String col, String table) {
		assert table != null && col != null;
		List<Pair<String>> cols = getTableColumns(em, table);
		return SqlUtils.getColumnIndex(col, cols);
	}

	/**
	 * Get info on a table scheme. This uses a cache, so repeated calls are
	 * cheap.
	 * 
	 * @param table
	 *            Case insensitive
	 * @return List of (column_name, data_type) pairs
	 * @throws IllegalArgumentException
	 *             if the table does not exist
	 */
	public static List<Pair<String>> getTableColumns(EntityManager em,
			String table) {
		table = table.toLowerCase();

		// cached?
		List<Pair<String>> cols = SqlUtils.table2columns.get(table);
		if (cols != null)
			return cols;

		Query q = em
				.createNativeQuery("SELECT column_name, data_type FROM information_schema.columns WHERE lower(table_name) = '"
						+ table + "' order by ordinal_position;");
		List<Object[]> rs = q.getResultList();

		if (rs.isEmpty())
			throw new IllegalArgumentException("No such table: " + table);

		cols = new ArrayList();
		for (Object[] objects : rs) {
			Pair p = new Pair(objects[0], objects[1]);
			cols.add(p);
		}

		// cache it
		SqlUtils.table2columns.put(table, cols);
		return cols;
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
	public static <X> List<X> inflate(String select, List<Object[]> rs,
			Class<X> klass) {
		try {
			// x.id, x.contents, etc.
			Field[] fields = inflate2_whichColumns(select, klass);

			// process rows
			List<X> rows = new ArrayList(rs.size());
			for (Object[] row : rs) {
				X x = klass.newInstance();
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
				rows.add(x);
			}

			return rows;
		} catch (Exception ex) {
			throw Utils.runtime(ex);
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
			assert val instanceof Integer : val;
			int i = (Integer) val;
			return ks[i];
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
			field.setAccessible(true);
			fields[i] = field;
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
	public static Query upsert(EntityManager em, String table,
			String[] idColumns, Map<String, ?> col2val, boolean specialCaseId) {
		List<Pair<String>> columnInfo = upsert2_columnInfo(em, table,
				idColumns, col2val, specialCaseId);

		StringBuilder whereClause = SqlUtils.upsert2_where(idColumns, col2val);

		StringBuilder upsert = new StringBuilder();

		// 1. update where exists
		SqlUtils.upsert2_update(table, col2val, specialCaseId, columnInfo,
				whereClause, upsert, false);

		// 2. insert where not exists
		SqlUtils.upsert2_insert(table, col2val, specialCaseId, columnInfo,
				whereClause, upsert);

		// create query
		Query q = em.createNativeQuery(upsert.toString());

		// set params
		upsert2_setParams(col2val, specialCaseId, columnInfo, q);

		return q;
	}

	static void upsert2_setParams(Map<String, ?> col2val,
			boolean specialCaseId, List<Pair<String>> columnInfo, Query q) {
		for (Pair<String> colInfo : columnInfo) {
			Object v = col2val.get(colInfo.first);
			// did this column get ignored?
			if ("oid".equals(colInfo.second) || v == null) {
				continue;
			}
			if (specialCaseId && "id".equals(colInfo.first)) {
				continue;
			}
			// set param
			q.setParameter(colInfo.first, v);
		}
	}

	/**
	 * Half an upsert: update-if-exists Does nothing if the row does not exist.
	 * 
	 * Yes this is hacky. Got any better ideas?
	 * 
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
	 *            travel across servers. Normally false.
	 * @return An update-where query, with it's parameters set.
	 */
	public static Query update(EntityManager em, String table,
			String[] idColumns, Map<String, ?> col2val, boolean specialCaseId) {
		List<Pair<String>> columnInfo = upsert2_columnInfo(em, table,
				idColumns, col2val, specialCaseId);

		StringBuilder whereClause = SqlUtils.upsert2_where(idColumns, col2val);

		StringBuilder upsert = new StringBuilder();

		// 1. update
		SqlUtils.upsert2_update(table, col2val, specialCaseId, columnInfo,
				whereClause, upsert, false);

		// create query
		Query q = em.createNativeQuery(upsert.toString());

		// set params
		upsert2_setParams(col2val, specialCaseId, columnInfo, q);

		return q;
	}

	/**
	 * Half an upsert: insert-if-not-there. Does nothing if the row already
	 * exists.
	 * 
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
	 *            travel across servers. Normally false.
	 * @return An insert-if-absent query, with it's parameters set.
	 */
	public static Query insertIfAbsent(EntityManager em, String table,
			String[] idColumns, Map<String, ?> col2val, boolean specialCaseId) {
		List<Pair<String>> columnInfo = upsert2_columnInfo(em, table,
				idColumns, col2val, specialCaseId);

		StringBuilder whereClause = SqlUtils.upsert2_where(idColumns, col2val);

		StringBuilder upsert = new StringBuilder();

		// 1. insert where not exists
		SqlUtils.upsert2_insert(table, col2val, specialCaseId, columnInfo,
				whereClause, upsert);

		// create query
		Query q = em.createNativeQuery(upsert.toString());

		// set params
		upsert2_setParams(col2val, specialCaseId, columnInfo, q);

		return q;
	}

	private static List<Pair<String>> upsert2_columnInfo(EntityManager em,
			String table, String[] idColumns, Map<String, ?> col2val,
			boolean specialCaseId) {
		List<Pair<String>> columnInfo = getTableColumns(em, table);
		// safety check inputs
		assert idColumns.length <= columnInfo.size();
		for (String idc : idColumns) {
			assert idc != null : Printer.toString(idColumns);
			int i = SqlUtils.getColumnIndex(idc, columnInfo);
			assert i != -1 : idc + " vs " + columnInfo;
		}
		if (specialCaseId) {
			assert !Containers.contains("id", idColumns);
			assert getColumnIndex(em, "id", table) != -1 : table + " = "
					+ getTableColumns(em, table);
		} else {
			assert !col2val.containsKey("id") : col2val;
		}
		return columnInfo;
	}

}