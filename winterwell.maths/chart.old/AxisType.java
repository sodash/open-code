package com.winterwell.maths.chart;

import com.winterwell.json.JSONString;

/**
 * Defines the type of axis to render a chart with.
 * @author Steven King <steven@winterwell.com>
 *
 */
public enum AxisType implements JSONString {
	LINEAR("linear"),
	LOGARITHMIC("logarithmic"),
	DATETIME("datetime");
	
	private final String jsonString;
	
	private AxisType(String jsonString) {
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
