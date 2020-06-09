package com.winterwell.maths.chart;

import com.winterwell.json.JSONString;

/**
 * Defines the type of axis to render a chart with.
 * @author Steven King <steven@winterwell.com>
 *
 */
public enum ChartType implements JSONString {
	LINE("line"),
	AREA("area"),
	COLUMN("column"),
	BAR("bar"),
	SCATTER("scatter"),
	PIE("pie");
	
	private final String jsonString;
	
	private ChartType(String jsonString) {
		this.jsonString = "\"" + jsonString + "\"";
	}
	
	/**
	 * @return The JSON-representation of this axis type.
	 */
	@Override
	public String toJSONString() {
		return this.jsonString;
	}
}
