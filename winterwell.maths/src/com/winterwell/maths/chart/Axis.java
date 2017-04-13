/**
 *
 */
package com.winterwell.maths.chart;

import java.util.ArrayList;
import java.util.List;

import com.winterwell.json.JSONObject;
import com.winterwell.json.JSONString;
import com.winterwell.utils.containers.Range;

/**
 * An axis for a {@link Chart}. This should be subclassed to set default values
 * for axis types.
 * 
 * @author Steven King <steven@winterwell.com>
 *
 */
public class Axis implements JSONString {
	protected String id;
	protected String title;
	protected AxisType type;
	protected Range range;
	protected List<String> categories;
	
	public String getId() {
		return this.id;
	}
	
	public void setId(String id) {
		this.id = id;
	}
	
	public String getTitle() {
		return this.title;
	}
	
	public void setTitle(String title) {
		this.title = title;
	}
	
	public AxisType getType() {
		return this.type;
	}
	
	public Axis setType(AxisType type) {
		this.type = type;
		return this;
	}
	
	public Range getRange() {
		return this.range;
	}
	
	/**
	 * Sets the range of the axis. Also removes any previously set category labels.
	 * @param range
	 * @return This axis, for chaining. 
	 */
	public void setRange(Range range) {
		this.range = range;
		this.categories = null;
	}
	
	public List<String> getCategories() {
		return this.categories;
	}
	
	public String getCategory(int index) {
		return categories.get(index);
	}
	
	/**
	 * Sets the category labels for this axis. Also removes any previously set range.
	 * @param categories
	 * @return This axis, for chaining.
	 */
	public void setCategories(Iterable<?> categories) {
		ArrayList<String> parsedCategories = new ArrayList<String>();
		
		for (Object category : categories) {
			parsedCategories.add(category.toString());
		}
		
		this.categories = parsedCategories;
		this.range = null;
	}
	
	/**
	 * 
	 * @return A JSON-encoded string that adheres to the charting API.
	 */
	@Override
	public String toJSONString() {
		JSONObject jsonObject = new JSONObject();
		
//		try {
			if (this.id != null) {
				jsonObject.put("id", this.id);
			}
			
			if (this.title != null) {
				jsonObject.put("title", new JSONObject().put("text", this.title));
			}
			
			if (this.type != null) {
				jsonObject.put("type", this.type);
			}
			
			if (this.range != null) {
				jsonObject.put("min", this.range.low);
				jsonObject.put("max", this.range.high);
			}
			
			if (this.categories != null) {
				jsonObject.put("categories", this.categories);
			}
//		} catch (JSONException e) {
//			// This should not happen - we have ensured that all calls to JSONObject.put have
//			// a key that is not null.
//			jsonObject = new JSONObject();
//		}
		
		return jsonObject.toString();
	}

	
}