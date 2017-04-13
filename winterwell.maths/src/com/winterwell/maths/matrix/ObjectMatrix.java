package com.winterwell.maths.matrix;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.winterwell.maths.datastorage.Index;
import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.utils.containers.Containers;
import com.winterwell.utils.containers.Pair2;
import com.winterwell.utils.web.IHasJson;
import com.winterwell.utils.web.SimpleJson;

import no.uib.cipr.matrix.Matrix;

/**
 * A matrix indexed by objects. null is allowed as an index. Designed for
 * low-intensity and/or sparse use.
 * 
 * ??Using an {@link Index} with a {@link Matrix} may be better.
 * 
 * Not thread safe (uses HashMap)
 * 
 * @author Daniel
 * @testedby {@link ObjectMatrixTest}
 */
public class ObjectMatrix<Row, Col> implements Serializable, IHasJson {

	
	
	/**
	 * @param klass A row name
	 * @return the total true instances of klass (sum the values in the row)
	 */
	public double getRowTotal(Row klass) {
		double total = 0;
		Map<Col, Double> row = getRow(klass);
		for (Double d : row.values()) {
			total += d;
		}
		return total;
	}

	public double getColumnTotal(Col klass) {
		double t = 0;
		for (Row k : getRowValues()) {
			t += get(k, klass);
		}
		return t;
	}

	
	private static final long serialVersionUID = 1L;

	private final Map<Pair2<Row, Col>, Double> backing = new HashMap<Pair2<Row, Col>, Double>();

	
	// TODO
	Map<Row,Double> mult(Map<Col,Double> objectVector) {
		Map ax = new HashMap();
		for(Map.Entry<Col, Double> vi : objectVector.entrySet()) {
			Map<Row, Double> col = getColumn(vi.getKey());
			// TODO
		}
		return ax;
	}
	
	public ObjectMatrix() {
	}

	/**
	 * @return Iterable over (row-key, col-key) pairs
	 */
	public Iterable<Pair2<Row, Col>> entries() {
		return Containers.iterable(backing.keySet().iterator());
	}

	public double get(Row row, Col col) {
		Double d = backing.get(new Pair2<Row, Col>(row, col));
		return d == null ? 0 : d;
	}

	/**
	 * @param col
	 * @return NB: This is a fresh Map which can be edited without affect.
	 */
	public Map<Row, Double> getColumn(Col col) {
		Set<Row> rvs = getRowValues();
		Map<Row, Double> colmn = new HashMap();
		for (Row row : rvs) {
			double cell = get(row, col);
			colmn.put(row, cell);
		}
		return colmn;
	}

	public Set<Col> getColumnValues() {
		Set<Col> cols = new HashSet<Col>();
		for (Pair2<Row, Col> k : backing.keySet()) {
			cols.add(k.second);
		}
		return cols;
	}

	public Map<Col, Double> getRow(Row row) {
		Set<Col> cols = getColumnValues();
		HashMap<Col, Double> r = new HashMap<Col, Double>();
		for (Col col : cols) {
			r.put(col, get(row, col));
		}
		return r;
	}

	/**
	 * The order is not guaranteed to remain the same.
	 * 
	 * @return
	 */
	public Set<Row> getRowValues() {
		Set<Row> cols = new HashSet<Row>();
		for (Pair2<Row, Col> k : backing.keySet()) {
			cols.add(k.first);
		}
		return cols;
	}

	/**
	 * Update the matrix
	 * 
	 * @param row Can be null
	 * @param col Can be null
	 * @param dx
	 *            The weighted update.
	 * @return
	 */
	public double plus(Row row, Col col, double dx) {
		Pair2<Row, Col> k = new Pair2<Row, Col>(row, col);
		return Containers.plus(backing, k, dx);
	}

	public void removeColumn(Col c) {
		Pair2[] keys = backing.keySet().toArray(new Pair2[0]);
		for (Pair2<Row, Col> k : keys) {
			if (c.equals(k.second)) {
				backing.remove(k);
			}
		}
	}

	public void removeRow(Row r) {
		Pair2[] keys = backing.keySet().toArray(new Pair2[0]);
		for (Pair2<Row, Col> k : keys) {
			if (r.equals(k.first)) {
				backing.remove(k);
			}
		}
	}

	public void set(Row row, Col col, double value) {
		Pair2<Row, Col> k = new Pair2<Row, Col>(row, col);
		if (value == 0) {
			backing.remove(k);
		} else {
			backing.put(k, value);
		}
	}

	@Override
	public String toString() {
		return toString(6);
	}

	public String toString(int maxItems) {
		StringBuilder sb = new StringBuilder();
		List<Col> cols = new ArrayList<Col>(getColumnValues());
		List<Row> rows = new ArrayList<Row>(getRowValues());
		boolean cdots = false, rdots = false;
		// Protect against giant matrices
		if (cols.size() > maxItems) {
			cols = cols.subList(0, 6);
			cdots = true;
		}
		if (rows.size() > maxItems) {
			rows = rows.subList(0, 6);
			rdots = true;
		}
		// header
		for (Col c : cols) {
			sb.append(c);
			sb.append('\t');
		}
		if (cdots) {
			sb.append("...");
		}
		sb.append('\n');
		// body
		for (Row r : rows) {
			for (Col c : cols) {
				sb.append(get(r, c));
				sb.append('\t');
			}
			sb.append(r);
			sb.append('\n');
		}
		if (rdots) {
			sb.append("...\n");
		}
		return sb.toString();
	}

	@Override
	public void appendJson(StringBuilder sb) {
		sb.append(toJSONString());
	}

	@Override
	public String toJSONString() {
		return new SimpleJson().toJson(toJson2());
	}

	/**
	 * Note: _not_ good for large sparse matrices.
	 * 
	 * @return row -&gt; col -&gt; value
	 */
	@Override
	public Map<Row,Map<Col,Double>> toJson2() {
		Set<Row> rows = getRowValues();
		Set<Col> cols = getColumnValues();
		Map<Row, Map<Col, Double>> map = new ArrayMap();
		for (Row row : rows) {
			Map<Col, Double> rowMap = new ArrayMap();
			for(Col col : cols) {
				rowMap.put(col, get(row, col));
			}
			map.put(row, rowMap);
		}
		return map;
	}

}
