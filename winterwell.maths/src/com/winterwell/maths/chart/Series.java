package com.winterwell.maths.chart;

import java.awt.Color;
import java.util.List;

import com.winterwell.json.JSONArray;
import com.winterwell.json.JSONObject;
import com.winterwell.json.JSONString;

import no.uib.cipr.matrix.Vector;

/**
 * A series to render in a {@link Chart}.
 * 
 * @deprecated merge with AChart??
 * Using AChart.getData() allows for more flexible data storage. 
 * 
 * @author Steven King <steven@winterwell.com>
 *
 */
class Series implements JSONString {
	protected String name;
	protected ChartType type;
	protected List<Vector> data;
	
	protected Axis xAxis;
	protected Axis yAxis;
	
	protected Color color;
	
	public String getName() {
		return this.name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public ChartType getType() {
		return this.type;
	}
	
	public void setType(ChartType type) {
		this.type = type;
	}
	
	public List<Vector> getData() {
		return this.data;
	}
	
	public void setData(List<Vector> data) {
		this.data = data;
	}
	
	/**
	 * 
	 * @return The x-axis on which this series is to be plotted.
	 */
	public Axis getxAxis() {
		return this.xAxis;
	}
	
	/**
	 * Set the x-axis for the series. This is required in the case of a chart
	 * with multiple axes, so that the series will be plotted on the correct
	 * axis.
	 * 
	 * If the x-axis does not have its id defined, an IllegalStateException
	 * will be thrown.
	 * 
	 * @param axis
	 * @return
	 * @throws IllegalStateException
	 */
	public void setxAxis(Axis axis) throws IllegalStateException {
		if (axis == null) {
			this.xAxis = null;
			
			return;
		}
		
		if (axis.id == null) {
			throw new IllegalStateException("Axis must have defined id to be set for series");
		}
		
		this.xAxis = axis;
	}
	
	/**
	 * 
	 * @return The y-axis on which this series is to be plotted.
	 */
	public Axis getyAxis() {
		return this.yAxis;
	}
	
	/**
	 * Set the y-axis for the series. This is required in the case of a chart
	 * with multiple axes, so that the series will be plotted on the correct
	 * axis.
	 * 
	 * If the y-axis does not have its id defined, an IllegalStateException
	 * will be thrown.
	 * 
	 * @param axis
	 * @return
	 * @throws IllegalStateException
	 */
	public void setyAxis(Axis axis) throws IllegalStateException {
		if (axis == null) {
			this.yAxis = null;
			
			return;
		}
		
		if (axis.id == null) {
			throw new IllegalStateException("Axis must have defined id to be set for series");
		}
		
		this.yAxis = axis;
	}
	
	public Color getColor() {
		return this.color;
	}
	
	public void setColor(Color color) {
		this.color = color;
	}
	
	/**
	 * Encodes the series into a JSON string that adheres to the charting API.
	 * If either axis a null id field, they will not be included in the result. 
	 * 
	 * @return A JSON-encoded string that adheres to the charting API.
	 */
	public String toJSONString() {
		return this.toJsonObject().toString();
	}
	
	protected JSONObject toJsonObject() {
		JSONObject jsonObject = new JSONObject();
		
		if (this.name != null) {
			jsonObject.put("name", this.name);
		}
		
		if (this.type != null) {
			jsonObject.put("type", this.type);
		}
		
		if (this.data != null) {
			JSONArray jsonArray = new JSONArray();
			
			for (Vector point : this.data) {
				JSONObject pointJsonObject = new JSONObject();
				
				if (point.size() == 1) {
					pointJsonObject.put("y", point.get(0));
				} else if (point.size() > 1) {
					pointJsonObject.put("x", point.get(0));
					
					pointJsonObject.put("y", point.get(1));
				}
				
				jsonArray.put(pointJsonObject);
			}
			
			jsonObject.put("data", jsonArray);
		}
		
		if (this.xAxis != null && this.xAxis.id != null) {
			jsonObject.put("xaxis", this.xAxis.id);
		}
		
		if (this.yAxis != null && this.yAxis.id != null) {
			jsonObject.put("yaxis", this.yAxis.id);
		}
		
		return jsonObject;
	}
}
