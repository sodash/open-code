package com.winterwell.datalog;

import com.winterwell.es.client.agg.Aggregation;
import com.winterwell.es.client.agg.Aggregations;
import com.winterwell.utils.TodoException;

/**
 * Should this just be done via {@link Aggregation}??
 * 
 * e.g. pub{"count":"sum"}
 * @author daniel
 *
 */
public class Breakdown {

	/**
	 * String[]??
	 */
	String by;
	String field;
	String op;

	public Breakdown(String field) {
		this(null, field, null);
	}
	
	/**
	 * 
	 * @param by e.g. "pub"
	 * @param field e.g. "count" or "price"
	 * @param operator e.g. "sum"
	 */
	public Breakdown(String by, String field, String operator) {
		this.by = by;
		this.field =field;
		this.op = operator;
	}

	/**
	 * @return make an aggregation for this breakdown
	 */
	public Aggregation getAggregation() {
		if (by==null) {
			// HACK top-level total stat
			return Aggregations.stats(field, field);
		}
		throw new TodoException();
	}
	
	@Override
	public String toString() {
		return by+"{\""+field+"\":\""+op+"\"}";
	}
}
