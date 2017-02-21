package com.winterwell.datalog;

import java.util.regex.Pattern;

import com.winterwell.utils.time.Time;

public final class StatReqFixed<T> extends StatReq<T> {

	public StatReqFixed(T value) {
		super(null, null, null, null, null, null, null);
		this.v = value;
	}	

}
