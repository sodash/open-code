package com.winterwell.maths.datastorage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import com.winterwell.maths.timeseries.BucketedDataStream;
import com.winterwell.maths.timeseries.Datum;
import com.winterwell.maths.timeseries.ExtraDimensionsDataStream.KMatchPolicy;
import com.winterwell.maths.timeseries.IDataStream;
import com.winterwell.maths.timeseries.ListDataStream;
import com.winterwell.maths.timeseries.MixedDataStream;
import com.winterwell.utils.IProperties;
import com.winterwell.utils.Key;
import com.winterwell.utils.Printer;
import com.winterwell.utils.containers.Containers;
import com.winterwell.utils.containers.ListMap;
import com.winterwell.utils.containers.Properties;
import com.winterwell.utils.time.Dt;
import com.winterwell.utils.time.TUnit;
import com.winterwell.utils.time.Time;

import no.uib.cipr.matrix.Vector;

/**
 * Base for implementing {@link IDataSet}
 * 
 * TODO fetch columns by property bag, and sum-over-columns with a
 * {@link KMatchPolicy}
 * 
 * @testedby {@link DataSetTest}
 * @author daniel
 */
public abstract class ADataSet implements IDataSet {

	private static final Key COLUMN_SIZE = new Key("IDataSet.column-size");

	protected IIndex<ColumnInfo> columns = new Index<ColumnInfo>();
	// protected Map<String, ColumnInfo> attInfo = new HashMap<String,
	// ColumnInfo>();

	/**
	 * This is populated by {@link #calculateColumnSizes()}
	 */
	protected ArrayList<ColumnInfo> emptyOrSoloDataPoint;

	private int maxColumnSize;

	private String name;

	/**
	 * Convenience for creating a dataset, then specifying the columns later.
	 * 
	 * @param name
	 */
	protected ADataSet(String name) {
		this(name, new String[0]);
	}

	public ADataSet(String name, List<ColumnInfo> columns) {
		this.name = name;
		for (ColumnInfo att : columns) {
			addColumn(att);
		}
	}

	public ADataSet(String name, String... columns) {
		this.name = name;
		// check uniqueness
		HashSet<String> set = new HashSet<String>(Arrays.asList(columns));
		assert set.size() == columns.length : "Not unique: "
				+ Printer.toString(columns);
		for (String col : columns) {
			this.columns.add(new ColumnInfo(col));
		}
	}

	public void addColumn(ColumnInfo<?> columnInfo) {
		// check uniqueness of name
		String cName = columnInfo.getName();
		assert !hasColumn(cName) : cName;
		columns.add(columnInfo);
	}

	/**
	 * @return the number of data points in each column. Warning: This traverses
	 *         all the data so it is potentially a very expensive method call!
	 */
	public int[] calculateColumnSizes() {
		emptyOrSoloDataPoint = new ArrayList<ColumnInfo>();
		int[] cis = new int[columns.size()];
		int j = 0;
		int max = 0;
		for (ColumnInfo ci : columns) {
			int i = columns.indexOf(ci);
			IDataStream data = getDataStream1D(i);
			int size = Containers.size(data);
			ci.put(COLUMN_SIZE, size);
			cis[j] = size;
			j++;
			// we need at least 2 data points for most stats (e.g. variance)
			if (size < 2) {
				emptyOrSoloDataPoint.add(ci);
			}
			if (size > max) {
				max = size;
			}
		}
		maxColumnSize = max;
		return cis;
	}

	/**
	 * Convenience for accessing a particular attribute in a vector.
	 * 
	 * @param att
	 * @param vector
	 * @return the converted value
	 */
	public <X> X get(ColumnInfo<X> att, Vector vector) {
		double xd = vector.get(columns.indexOf(att));
		return att.convertFromDouble(xd);
	}

	public ColumnInfo getColumn(Integer col) {
		ColumnInfo colInfo = columns.get(col);
		return colInfo;
	}

	/**
	 * @param attributeName
	 * @return
	 * @throws IllegalArgumentException
	 *             if the name is not recognised
	 */
	public ColumnInfo getColumn(String attributeName)
			throws IllegalArgumentException {
		for (ColumnInfo ai : columns) {
			if (attributeName.equals(ai.getName()))
				return ai;
		}
		throw new IllegalArgumentException(attributeName + " not recognised");
	}

	@Override
	public int getColumnIndex(ColumnInfo column) {
		return columns.indexOf(column);
	}

	@Override
	public int getColumnIndex(String columnName)
			throws IllegalArgumentException {
		int i = 0;
		for (ColumnInfo a : columns) {
			if (columnName.equals(a.getName()))
				return i;
			i++;
		}
		throw new IllegalArgumentException(columnName);
	}

	@Override
	public List<String> getColumnNames() {
		ArrayList<String> list = new ArrayList<String>(columns.size());
		for (ColumnInfo a : columns) {
			list.add(a.getName());
		}
		return list;
	}

	public List<ColumnInfo> getColumns() {
		return Containers.getList(columns);
	}

	/**
	 * Get the columns that match the given set of properties
	 * 
	 * @param props
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	public List<ColumnInfo> getColumns(IProperties props) {
		List<ColumnInfo> all = getColumns();
		List<ColumnInfo> cols = new ArrayList<ColumnInfo>();
		for (ColumnInfo col : all) {
			if (Properties.matches(col, props)) {
				cols.add(col);
			}
		}
		return cols;
	}

	/**
	 * For a given key, get the different values (and the columns which have
	 * those values too, why not?).
	 * 
	 * ??Skips over columns which have null (which could be a non-null default
	 * value in practise).
	 * 
	 * @param key
	 * @return all the non-null values for key
	 */
	public <X> ListMap<X, ColumnInfo> getColumnValuesForProperty(Key<X> key) {
		ListMap<X, ColumnInfo> map = new ListMap<X, ColumnInfo>();
		for (ColumnInfo column : columns) {
			X v = (X) column.get(key);
			if (v == null) {
				continue;
			}
			map.add(v, column);
		}
		return map;
	}

	public IDataStream getDataStream1D(ColumnInfo<?> columnInfo) {
		return getDataStream1D(getColumnIndex(columnInfo));
	}

	/**
	 * Get all data for columns matching a set of properties. Values that match
	 * within tolerance will be summed together (the timestamp will be from the
	 * first labels element. Labels will be preserved only if there is no
	 * contention).
	 * 
	 * @param props
	 *            Columns must match this "description". I.e. for every key in
	 *            props, the column must be equals().
	 * @param tolerance
	 * @return 1D stream of aggregated data matching the given properties.
	 */
	public IDataStream getDataStream1D(IProperties props, Dt tolerance) {
		// There will be more efficient ways to do this if the data is in SQL
		// Also, you may wish to cache these streams
		List<ColumnInfo> cols = getColumns(props);
		if (cols.isEmpty())
			// wot no columns?
			return new ListDataStream(1);
		MixedDataStream mix = new MixedDataStream(this, cols);
		BucketedDataStream sum = new BucketedDataStream(mix, tolerance);
		sum.setStepByBaseStream(true);
		sum.setFilterEmptyBuckets(true);
		return sum;
	}

	/**
	 * Convenience method for {@link #getDataStream1D(IProperties, Dt)}.
	 */
	public IDataStream getDataStream1D(IProperties props, TUnit dt) {
		return getDataStream1D(props, dt.dt);
	}

	@Override
	public IDataStream getDataStream1D(String columnName) {
		return getDataStream1D(getColumnIndex(columnName));
	}

	@Override
	public String getName() {
		return name;
	}

	/**
	 * 'cos {@link #getColumnIndex(String)} throws exceptions if the column
	 * doesn't exist.
	 * 
	 * @param cName
	 * @return
	 */
	public boolean hasColumn(String cName) {
		assert cName != null;
		for (ColumnInfo ai : columns) {
			if (cName.equals(ai.getName()))
				return true;
		}
		return false;
	}

	/**
	 * Make a Datum from a map of column values. The values are converted to
	 * numbers by the ColumnInfo objects Not suitable for sparse vectors
	 * 
	 * @param time
	 *            time stamp for the datum
	 * @param dataPoint
	 * @param label
	 *            can be null
	 * @return
	 */
	public Datum makeDatum(Time time, Map<ColumnInfo, Object> dataPoint,
			Object label) {
		if (numColumns() < dataPoint.size()) {
			ArrayList wrongUns = new ArrayList(dataPoint.keySet());
			wrongUns.removeAll(getColumns());
			throw new IllegalArgumentException(numColumns() + " < "
					+ dataPoint.size() + "\t" + Printer.toString(wrongUns));
		}
		Datum x = new Datum(time, new double[numColumns()], label);
		x.setModifiable(true);
		for (ColumnInfo col : dataPoint.keySet()) {
			int i = getColumnIndex(col);
			assert i != -1 : col;
			assert i < x.size() : x.size() + " < " + i;
			Object v = dataPoint.get(col);
			double xi = col.convertToDouble(v);
			x.set(i, xi);
		}
		x.setModifiable(false);
		return x;
	}

	@Override
	public int numColumns() {
		return columns.size();
	}

	public <X> void set(ColumnInfo<X> att, X v, Vector vector) {
		double x = att.convertToDouble(v);
		vector.set(columns.indexOf(att), x);
	}

	@Override
	@Deprecated
	// due to unreliability
	public int size() {
		if (maxColumnSize > 0)
			return maxColumnSize;
		return 1000; // hokum
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[name=" + name + ", columns="
				+ numColumns() + ", rows=" + size() + "?]";
	}

}
