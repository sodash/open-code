package com.winterwell.datalog;

public class Callback {

	public final String dataspace;
	public final String evt;
	public final String url;

	public Callback(String dataspace, String eventType, String url) {
		this.dataspace= dataspace;
		this.evt = eventType;
		this.url = url;
	}

	@Override
	public String toString() {
		return "Callback [dataspace=" + dataspace + ", evt=" + evt + ", url=" + url + "]";
	}

}
