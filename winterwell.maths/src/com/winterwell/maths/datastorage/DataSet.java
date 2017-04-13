package com.winterwell.maths.datastorage;

import java.util.List;

import com.winterwell.maths.timeseries.DataUtils;
import com.winterwell.maths.timeseries.IDataStream;
import com.winterwell.maths.timeseries.ListDataStream;
import com.winterwell.utils.Utils;

/**
 * {@link ListDataStream} based in-memory default implementation of
 * {@link IDataSet}
 * 
 * 
 * @testedby {@link DataSetTest}
 * @author daniel
 */
public class DataSet extends ADataSet {

	private static String[] DataSet2_columns(int dim) {
		String[] names = new String[dim];
		for (int i = 0; i < names.length; i++) {
			names[i] = "col " + i;
		}
		return names;
	}

	ListDataStream data;

	public DataSet(IDataStream data) {
		super("anon-" + Utils.getRandomString(4), DataSet2_columns(data
				.getDim()));
		setData(data);
	}

	public DataSet(String name, List<ColumnInfo> columns) {
		super(name, columns);
	}

	public DataSet(String name, String... columns) {
		super(name, columns);
	}

	@Override
	public IDataStream getDataStream() {
		ListDataStream copy = data.clone();
		return copy;
	}

	@Override
	public IDataStream getDataStream1D(int column) {
		// create a stream with an independent pointer
		ListDataStream list = data.clone();
		return DataUtils.get1D(list, column);
	}

	public void setData(IDataStream data) {
		this.data = new ListDataStream(data);
	}

	@Override
	public int size() {
		return data.size();
	}

}
