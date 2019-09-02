package com.winterwell.datalog;

import java.util.Arrays;
import java.util.List;

import com.winterwell.es.client.agg.Aggregation;
import com.winterwell.es.client.agg.Aggregations;
import com.winterwell.utils.StrUtils;
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
	 * String[]
	 */
	String[] by;
	String field;
	String op;

	public List<String> getBy() {
		return Arrays.asList(by);
	}
	
	public Breakdown(String field) {
		this(null, field, null);
	}
	
	/**
	 * 
	 * @param by e.g. "pub" or ",pub" for top-level + breakdown-by-pub
	 * NB: a trailing comma will be ignored, but a leading one works.
	 * @param field e.g. "count" or "price"
	 * @param operator e.g. "sum"
	 */
	public Breakdown(String by, String field, String operator) {
		this.by = by==null? new String[]{""} : by.split(",");
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
		StringBuilder sb = new StringBuilder();
		for(String b : by) {
			sb.append(b);
			sb.append("{\""+field+"\":\""+op+"\"}");
			sb.append(",");
		}
		StrUtils.pop(sb, 1);
		return sb.toString();
	}
}
