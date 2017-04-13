package com.winterwell.maths.datastorage;

import java.util.List;

import com.winterwell.maths.timeseries.IDataStream;
import com.winterwell.utils.NotUniqueException;

/**
 * Add some info to an {@link IDataStream}. Provide for column names and
 * mappings from Objects to doubles via the {@link ColumnInfo}s. This is
 * inherently a row-based storage, where rows are accessed sequentially.
 * 
 * @see DataTable
 * @author Daniel
 * 
 */
public interface IDataSet {

	/**
	 * @param column
	 * @return index to access this column in vectors
	 */
	int getColumnIndex(ColumnInfo column);

	/**
	 * @param column
	 * @return index to access this column in vectors
	 */
	int getColumnIndex(String columnName) throws NotUniqueException;

	List<String> getColumnNames();

	IDataStream getDataStream();

	IDataStream getDataStream1D(int column);

	IDataStream getDataStream1D(String columnName);

	/**
	 * @return name of the dataset
	 */
	String getName();

	/**
	 * The number of columns.
	 */
	int numColumns();

	/**
	 * @return an *estimate* of the total number of data-points. Do NOT rely on
	 *         this figure.
	 * @Deprecated Be careful if using this - it is likely to be inaccurate.
	 */
	@Deprecated
	int size();
}
