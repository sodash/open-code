package com.winterwell.datalog;

public final class StatReqFixed<T> extends StatReq<T> {

	public StatReqFixed(T value) {
		super(null, null, null, null, null, null, null);
		this.v = value;
	}	

}
