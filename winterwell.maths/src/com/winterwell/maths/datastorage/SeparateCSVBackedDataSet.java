package com.winterwell.maths.datastorage;

import java.io.File;
import java.util.List;
import java.util.Map;

import com.winterwell.maths.timeseries.DataUtils;
import com.winterwell.maths.timeseries.Datum;
import com.winterwell.maths.timeseries.IDataStream;
import com.winterwell.maths.timeseries.ListDataStream;
import com.winterwell.utils.TodoException;
import com.winterwell.utils.io.FileUtils;
import com.winterwell.utils.time.Time;

/**
 * A dataset where each column is stored as a separate CSV file.
 * 
 * @author daniel
 * 
 */
public class SeparateCSVBackedDataSet extends ADataSet {

	private File dir;

	public SeparateCSVBackedDataSet(File dir, String name,
			List<ColumnInfo> columns) {
		super(name, columns);
		this.dir = dir;
		dir.mkdir();
		assert dir.isDirectory() : dir;
	}

	public Datum addDataPoint(Time time, Map<ColumnInfo, Object> dataPoint,
			Object label) {
		// append to each of the csv files
		for (ColumnInfo ci : dataPoint.keySet()) {
			File file = getCSV(ci);
			ListDataStream uno = new ListDataStream(1);
			Object v = dataPoint.get(ci);
			double x = ci.convertToDouble(v);
			uno.add(new Datum(time, x, label));
			DataUtils.writeAppend(uno, file, -1);
		}
		return makeDatum(time, dataPoint, label);
	}

	private File getCSV(ColumnInfo columnInfo) {
		String fName = getName() + "." + columnInfo.getName() + ".csv";
		fName = FileUtils.safeFilename(fName);
		// TODO test for uniqueness of filename in constructor
		return new File(dir, fName);
	}

	@Override
	public IDataStream getDataStream() {
		throw new TodoException(); // TODO
	}

	@Override
	public IDataStream getDataStream1D(ColumnInfo columnInfo) {
		// ?? cache streams?
		File file = getCSV(columnInfo);
		return DataUtils.read(file);
	}

	@Override
	public final IDataStream getDataStream1D(int column) {
		ColumnInfo info = getColumn(column);
		return getDataStream1D(info);
	}

	@Override
	public final IDataStream getDataStream1D(String column) {
		ColumnInfo info = getColumn(column);
		return getDataStream1D(info);
	}
}
