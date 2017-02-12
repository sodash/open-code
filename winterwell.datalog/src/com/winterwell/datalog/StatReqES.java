package com.winterwell.datalog;

import com.winterwell.datalog.DataLog.KInterpolate;
import com.winterwell.utils.time.Dt;
import com.winterwell.utils.time.Time;

public class StatReqES<X> extends StatReq<X> {

	public StatReqES(String cmd, String tag, Time start, Time end, KInterpolate interpolate, Dt bucketSize) {
		super(cmd, tag, start, end, interpolate, bucketSize);
		// TODO Auto-generated constructor stub
	}

}
