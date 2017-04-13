package com.winterwell.maths.chart;

import com.winterwell.json.JSONString;

public enum LegendAlignment implements JSONString {
	LEFT("left"),
	CENTER("center"),
	RIGHT("right");
	
	private final String jsonString;
	
	private LegendAlignment(String jsonString) {
		this.jsonString = "\"" + jsonString + "\"";
	}
	
	@Override
	public String toJSONString() {
		return this.jsonString;
	}
}
