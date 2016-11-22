package com.winterwell.utils.containers;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.winterwell.utils.StrUtils;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.log.Log.KErrorPolicy;

/**
 * A simple data table, for when a List or HashMap isn't quite good enough. Has
 * an index on the first column. It is designed to be close to a drop-in
 * replacement for HashMap and List.
 * <p>
 * This is a replacement (and base) for {@link DataTable}. It has the advantage of
 * supporting typed rows, which is better for long term maintenance.
 * <p>
 * Probably thread safe.
 * 
 * @author daniel
 * @param <C1>
 *            type of the 1st column
 */
public class Table<C1, Row> implements Serializable, Iterable<Row> {


	private static final long serialVersionUID = 1L;
	
	static Log.KErrorPolicy exceptionPolicy = KErrorPolicy.THROW_EXCEPTION;
	/**
	 * Hokey method to make it slightly nicer to write data-processing
	 * "scripts". Since it's shared across all DataTables, avoid like the plague
	 * in real code.
	 * 
	 * @param policy
	 */
	@Deprecated
	public static void setExceptionPolicy(Log.KErrorPolicy policy) {
		exceptionPolicy = policy;
	}

	/**
	 * If there are multiple rows with the same first column, then the last one
	 * in wins. Hey ho. TODO use a ListMap instead to avoid this. So we can have
	 * remove
	 */
	final Map<C1, Integer> column1toRow = new HashMap();

	final protected List<Row> rows = new ArrayList();

	private Class<Row> rowClass;
	
	public Class<Row> getRowClass() {
			return rowClass;
	}
	
	/**
	 * Create an empty DataTable with no serialisers.
	 */
	public Table(Class<Row> rowClass) {
		this.rowClass = rowClass;
	}
	

	/**
	 * Add a row. The 0th item (picked out via {@link #getRowKey(Object)}) will be used as the index for
	 * {@link #get(Object)}.
	 * 
	 * @param row
	 *            All rows must be the same length.
	 */
	public synchronized void add(Row row) {
		// assert rows.isEmpty() || row.length == rows.get(0).length :
		// row.length+" vs "+rows.get(0).length;
		if (index) {
			column1toRow.put(getRowKey(row), rows.size());
		}
		rows.add(row);
	}
	
	/**
	 * USe to switch off indexing
	 * @param index
	 */
	public void setIndex(boolean index) {
		this.index = index;
	}
	
	boolean index = true;	

	public Row remove(int rowNum) {
		Row row = rows.remove(rowNum);
		// remove the index mapping, if it was pointing at this row
		C1 rkey = getRowKey(row);		
		Integer irow = column1toRow.get(rkey);
		if (irow!=null && irow==rowNum) {
			column1toRow.remove(rkey);
		}
		return row;
	}

	
	/**
	 * Most users should over-ride this! 
	 * Uses toString() by default.
	 * @param row
	 * @return The "column 1" key for this row. 
	 */
	protected C1 getRowKey(Row row) {
		return (C1) row.toString();
	}

	/**
	 * @param key
	 * @return true if there is a row where column 1 = key
	 */
	public boolean containsKey(C1 key) {
		return column1toRow.containsKey(key);
	}

	/**
	 * @param column1
	 * @return row where the first(zeroeth) column equals column1, or null
	 */
	public Row get(C1 column1) {
		Integer r = column1toRow.get(column1);
		return r == null ? null : rows.get(r);
	}

	@Deprecated
	public final List<Row> getLowLevel() {
		return rows;
	}

	public Row getRow(int row) {
		return rows.get(row);
	}


	@Override
	public Iterator<Row> iterator() {
		return rows.iterator();
	}

	public Set<C1> keySet() {
		return Collections.unmodifiableSet(column1toRow.keySet());
	}

	/**
	 * @return Number of rows
	 */
	public int size() {
		return rows.size();
	}

	@Override
	public String toString() {
		StringBuilder sample = new StringBuilder();
		for(int i=0, n=Math.min(6, size()); i<n; i++) {
			sample.append(getRow(i));
			sample.append(", ");
		}
		StrUtils.pop(sample, 2);
		if (size()>6) sample.append("...");
		return "Table[" + size() + " rows: "+sample+"]";
	}

}
