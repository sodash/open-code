package com.winterwell.datalog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import com.winterwell.datalog.Stat.KInterpolate;
import com.winterwell.depot.Desc;
import com.winterwell.maths.timeseries.Datum;
import com.winterwell.maths.timeseries.ListDataStream;
import com.winterwell.utils.Printer;
import com.winterwell.utils.Utils;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.time.Dt;
import com.winterwell.utils.time.TUnit;
import com.winterwell.utils.time.Time;

/**
 * An SQL implementation of the StatReq.
 *
 * @testedby {@link StatReqSQLTest}
 * @author Agis Chartsias <agis@winterwell.com>
 *
 * @param <X>
 */
public class StatReqSQL<X> extends StatReq<X> {

	public StatReqSQL(String cmd, String tag, Time start, Time end, KInterpolate interpolate, Dt bucketSize) {
		super(cmd, tag, start, end, interpolate, bucketSize);
	}

	public StatReqSQL(String cmd, Pattern tagMatcher, Time start, Time end) {
		super(cmd, tagMatcher, start, end);
	}
	
	@Override
	public X get() {
		X value = getValue();
		if (value != null) return value;
		else if (error != null) {
			// remote fetches can fail
			throw Utils.runtime(error);
		}

		run();

		return getValue();
	}



	public void run() {
		stat = (StatImpl) Stat.dflt;
		String srv = server == null? Desc.MY_SERVER() : server; // MY_SERVER() not LOCAL!
		SQLStorage storage = (SQLStorage) stat.storage;

		// Find period
		Time s = start;
		Time e = end;
		if (start == null) s = new Time(0);
		if (end == null) e = new Time().plus(TUnit.HOUR);

		if (s.getTime() < 0) s = new Time(0);
		assert e.isAfter(s) : "Start time after end: " + s + " vs " + e;

		// Prepare object value
		initV(this);

		if (cmd.equals(KStatReq.TOTAL)) {
			double count = storage.selectSum(srv, s, e, tagMatcher, tag);
			addDatum(s, count, tag);
		}
		else {
			Iterator<Object[]> reader = storage.getReader(srv, s, e, tagMatcher, tag);
			readData(reader);
		}

		// Add in the last bucket
		// (JH) This condition looks obsolete and also pointless -- remove it?
		// I think it's needed (NB: this is AG's code). Consider: You request data up to the present moment, 
		// then the last data-point is not in the database -- so we add it from memory. ^DW
		if (Desc.LOCAL_SERVER.equals(srv) || Desc.MY_SERVER().equals(srv)) {
			addLatestHistoricBucket();
		}
	}

	/**
	 * Grab latest data from recent buckets, merge and sort it.
	 * @param batchGet
	 */
	private void addLatestHistoricBucket() {
		Datum latest = stat.currentBucket(tag, end);
		List<Datum> curHistData = stat.currentHistoric(tag, start, end);
		
		if (latest == null && curHistData == null) return;
		
		if (latest != null && curHistData != null) {
			curHistData.add(latest);
		} 
		else if (latest != null && curHistData == null) {
			curHistData = new ArrayList<Datum>();
			curHistData.add(latest);
		}
		
		// Case of a total count
		if (v instanceof Double) {
			for (Datum d : curHistData) add(this, d);
			return;
		} 
		
		// Case of a datastream
		// data *must* be sorted, or else ListDataStream will explode
		assert v instanceof ListDataStream : v.getClass();
		ListDataStream lds = (ListDataStream) v;
		List<Datum> data = lds.getList();
		List<Datum> histData = new ArrayList<Datum>(); 
		
		// Merge data from database and cache and update values if needed. 
		for (Datum hd : curHistData) {
			if (interpolate==KInterpolate.SKIP_ZEROS && hd.isZero()) {
				continue;
			}
			Datum d = removeDatum(data, hd);
			if (d == null) {
				histData.add(hd);
				continue;
			}
			
			Datum sum = new Datum(d.getTime(), d.x() + hd.x(), d.getLabel());
			histData.add(sum);
		}
				
		data.addAll(histData);
		Collections.sort(data);
		lds = new ListDataStream(data);
		v = (X) lds;		
	}
	
	/**
	 * Remove from the list a datum with the same time and label as
	 * the specified one.
	 *  
	 * @param list
	 * @param d
	 * @return the datum from the list if found, or null
	 */
	private Datum removeDatum(List<Datum> list, Datum d) {
		for (Datum datum : list) {
			if (datum.getTime().equals(d.getTime())) {
				list.remove(datum);
				return datum;
			}
		}
		return null;
	}

	/**
	 * Parse every line that comes from an SQL return statement into data
	 * and add it to the request.
	 * @param r
	 */
	private void readData(Iterator<Object[]> r) {
		while (r.hasNext()) {
			Object[] row = r.next();
			String timestamp = row[0].toString();
			String mean_count = row[2].toString();
			String tag = row[1].toString();

			if (row.length < 3) {
				Log.w(Stat.LOGTAG, "getData bogus row: "+Printer.toString(row));
				continue; // an error??
			}
			addDatum(new Time(timestamp), Double.valueOf(mean_count), tag);
		}
	}

	private void addDatum(Time timestamp, double count, String tag) {
		Datum datum = new Datum(timestamp, count, tag);
		add(this, datum);
	}
}
