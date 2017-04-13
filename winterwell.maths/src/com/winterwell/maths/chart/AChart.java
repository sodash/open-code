package com.winterwell.maths.chart;

import java.util.List;

import com.winterwell.json.JSONArray;
import com.winterwell.json.JSONObject;
import com.winterwell.json.JSONString;
import com.winterwell.maths.stats.distributions.d1.IDistribution1D;
import com.winterwell.utils.TodoException;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.web.WebUtils;

import no.uib.cipr.matrix.Vector;

/**
 * The base class for both {@link Chart} and {@link PieChart}.
 * 
 * @author Steven King <steven@winterwell.com>
 *
 */
public abstract class AChart implements JSONString {
	protected Legend legend;
	protected ChartType type;
	/**
	 * The id of the div to render this in.
	 * TODO This doesn't really belong in the chart itself. It should be handled outside.
	 */
	protected String element;
	protected String title;
	protected String subtitle;
	
	@Deprecated // AChart.getData() covers this, and allows for flexible data-storage in e.g. distributions.
	protected Series series;
	protected String seriesURI;
	protected JSONObject options;
	
	/**
	 * The x-axis = 0
	 */
	public static final int X = 0;
	
	/**
	 * The y-axis = 1
	 */
	public static final int Y = 1;
	
	/**
	 * The z-axis = 2 -- if the chart supports this
	 */
	public static final int Z = 2;
	
	public Legend getLegend() {
		return legend;
	}
	
	public void setLegend(Legend legend) {
		this.legend = legend;
	}
	
	public boolean getShowLegend() {
		if (legend == null) {
			return true; // The default visibility for a legend.
		}
		
		return legend.isVisible();
	}
	
	public void setShowLegend(boolean showLegend) {
		if (this.legend == null) {
			this.legend = new Legend();
		}
		
		this.legend.setVisible(showLegend);
	}
	
	public ChartType getType() {
		return type;
	}
	
	public void setType(ChartType type) {
		this.type = type;
	}
	
	public String getElement() {
		return element;
	}
	
	public final void setElement(String element) {
		this.element = element;
	}
	
	public String getTitle() {
		return title;
	}
	
	public void setTitle(String title) {
		this.title = title;
	}
	
	public String getSubtitle() {
		return subtitle;
	}
	
	public void setSubtitle(String subtitle) {
		this.subtitle = subtitle;
	}

	public final Series getSeries() {
		return series;
	}
	

	/**
	 * List of 2D vectors (points on the chart)
	 */
	public List<Vector> getData() {
		if (this.series == null) {
			return null;
		}
		
		return this.series.getData();
	}
	
	@Deprecated
	public void setSeries(Series series) {
		this.series = series;
		
		assert seriesURI == null : this;
	}
	
	public String getSeriesURI() {
		return seriesURI;
	}
	
	/**
	 * Sets the URI to obtain the chart data from. It should return JSON-encoded
	 * data in the same form as required by {@link #setSeries(Series)} or
	 * {@link #setData(Iterable)}. 
	 * 
	 * @param dataURI
	 */
	public void setSeriesURI(String seriesURI) {
		this.seriesURI = seriesURI;
		
		this.series = null;
	}

	public final JSONObject getOptions() {
		return options;
	}
	
	/**
	 * Provide a map of options that are specific to a given render library,
	 * that will be passed verbatim to that library. This provides complete
	 * control over how a chart is rendered with the need to create custom
	 * JavaScript, at the cost of being tied to one rendering library. 
	 *   
	 * @param options A map of options to pass to the rendering library.
	 */
	public final void setOptions(JSONObject options) {
		this.options = options;
	}
	
	/**
	 * Creates a JSON-encoded string, representing this chart object,
	 * that adheres to the Winterwell charting API.
	 * 
	 * @return A JSON-encoded representation of this chart.
	 */
	@Override
	public String toJSONString() {
		JSONObject jsonObject = this.toJsonObject(new JSONObject());
		
		return jsonObject.toString();
	}
	
	/**
	 * Converts this chart to a JSONObject, to be transformed into a JSON-encoded
	 * string. This method should be overridden to implement any custom encoding. 
	 * 
	 * @param jsonObject TODO What is this?
	 * @return
	 */
	protected JSONObject toJsonObject(JSONObject jsonObject)
	{
//		try {
		// If the 'chart' key has been defined previously, preserve any
		// data that was added.
		JSONObject chartJsonObject = jsonObject.optJSONObject("chart");
		
		if (chartJsonObject == null) {
			chartJsonObject = new JSONObject();
		}
		
		if (this.element != null) {
			chartJsonObject.put("renderTo", this.element);
			
			jsonObject.put("chart", chartJsonObject);
		}
		
		if (this.type != null) {
			chartJsonObject.put("type", this.type);
			
			jsonObject.put("chart", chartJsonObject);
		}
		
		if (this.title != null) {
			jsonObject.put("title", new JSONObject().put("text", title));
		} else {
			jsonObject.put("title", new JSONObject().put("text", JSONObject.NULL));
		}
		
		if (subtitle != null) {
			jsonObject.put("subtitle", new JSONObject().put("text", subtitle));
		}
		
		// TODO either we store a Series object XOR we store data.
		if (series != null) {
			jsonObject.put("series", new JSONArray().put(series));
			
			if (series.color != null) {
				jsonObject.put("colors", new JSONArray().put(WebUtils.color2html(series.color)));
			}
		} else if (this.getData() != null) {
			Series seriesFromData = new Series();
			
			seriesFromData.setData(this.getData());
			
			jsonObject.put("series", new JSONArray().put(seriesFromData));
		}
		
		if (seriesURI != null) {
			jsonObject.put("series", seriesURI);
		}
		
		if (legend != null) {
			jsonObject.put("legend", legend);
		}
		
		if (options != null) {
			jsonObject.put("options", options);
		}
//		} catch (JSONException e) {
//			// This should not happen - we have ensured that all calls to JSONObject.put have
//			// a key that is not null.
//			jsonObject = new JSONObject();
//		}		
		return jsonObject;
	}

	/**
	 * Pick a chart. A dumb convenience method.
	 * @param data
	 * @param name Can use {method}, {class}
	 * @return
	 */
	public static AChart chart(Object data, String name) {
		if (name != null) {
			// Pipe name through the formatter
			name = Log.format(name);
		}
		if (data instanceof IDistribution1D) {
			HistogramChart hc = new HistogramChart((IDistribution1D) data);
			hc.setTitle(name);
			return hc;
		}
		// Time series??
		throw new TodoException(data.getClass()+" "+data);
	}
	
}
