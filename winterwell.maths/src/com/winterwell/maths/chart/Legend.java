package com.winterwell.maths.chart;

import com.winterwell.json.JSONObject;
import com.winterwell.json.JSONString;

/**
 * A legend for a {@link Chart}. This should be subclassed to set default values
 * for legend types. The visibility of the legend defaults to true.
 * 
 * @author Steven King <steven@winterwell.com>
 *
 */
public class Legend implements JSONString {
	protected boolean isVisible = true; 
	protected String title;
	protected LegendAlignment alignment;
	protected LegendVerticalAlignment verticalAlignment;
	
	public boolean isVisible() {
		return isVisible;
	}

	public void setVisible(boolean isVisible) {
		this.isVisible = isVisible;
	}

	public String getTitle() {
		return this.title;
	}
	
	public void setTitle(String title) {
		this.title = title;
	}
	
	public LegendAlignment getAlignment() {
		return this.alignment;
	}
	
	public void setAlignment(LegendAlignment alignment) {
		this.alignment = alignment;
	}
	
	public LegendVerticalAlignment getVerticalAlignment() {
		return this.verticalAlignment;
	}
	
	public void setVerticalAlignment(LegendVerticalAlignment verticalAlignment) {
		this.verticalAlignment = verticalAlignment;
	}
	
	/**
	 * The JSON-encoded data for this legend. The visibility is only returned
	 * if set to false.
	 * 
	 * @return A JSON-encoded string that adheres to the charting API.
	 */
	@Override
	public String toJSONString() {
		JSONObject jsonObject = new JSONObject();
		
//		try {
			if (!this.isVisible) {
				jsonObject.put("enabled", this.isVisible);
			}
			
			if (this.title != null) {
				jsonObject.put("title", new JSONObject().put("text", this.title));
			}
			
			if (this.alignment != null) {
				jsonObject.put("align", this.alignment);
			}
			
			if (this.verticalAlignment != null) {
				jsonObject.put("verticalAlign", this.verticalAlignment);
			}
//		} catch (JSONException e) {
//			// This should not happen - we have ensured that all calls to JSONObject.put have
//			// a key that is not null.
//			jsonObject = new JSONObject();
//		}
		
		return jsonObject.toString();
	}
}
