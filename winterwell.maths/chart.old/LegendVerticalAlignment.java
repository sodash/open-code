package com.winterwell.maths.chart;

import com.winterwell.json.JSONString;

public enum LegendVerticalAlignment implements JSONString {
	TOP("top"),
	MIDDLE("middle"),
	BOTTOM("bottom");
	
	private final String jsonString;
	
	private LegendVerticalAlignment(String jsonString) {
		this.jsonString = "\"" + jsonString + "\"";
	}
	
	@Override
	public String toJSONString() {
		return this.jsonString;
	}
}
