package com.winterwell.datalog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.winterwell.datalog.DataLog.KInterpolate;
import com.winterwell.depot.Desc;
import com.winterwell.maths.timeseries.Datum;
import com.winterwell.maths.timeseries.ListDataStream;
import com.winterwell.utils.Printer;
import com.winterwell.utils.Utils;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.time.Dt;
import com.winterwell.utils.time.TUnit;
import com.winterwell.utils.time.Time;

// TODO
public class StatReqES<X> extends StatReq<X> {

	public StatReqES(String cmd, String tag, Time start, Time end, KInterpolate interpolate, Dt bucketSize) {
		super(cmd, tag, start, end, interpolate, bucketSize);
		// TODO Auto-generated constructor stub
	}



}
