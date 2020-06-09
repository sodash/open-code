package com.winterwell.maths.chart;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import com.winterwell.json.JSONArray;
import com.winterwell.json.JSONObject;
import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.utils.containers.Range;
import com.winterwell.utils.web.WebUtils;

/**
 * Plot several things on one set of axes
 * 
 * @author daniel
 * @testedby {@link GetAxisTest}
 */
public class CombinationChart extends Chart {

	final List<Chart> charts;

	private Map<Object, Chart> labelToChartMap = new ArrayMap<Object, Chart>();

	private TimeZone timeZone;

	/**
	 * The total line (if there is one)
	 */
	public static final String ALL = "all";

	public CombinationChart() {
		this.charts = new ArrayList<Chart>();
	}

	public CombinationChart(Chart... charts) {
		this(Arrays.asList(charts));
	}

	/**
	 * 
	 * @param charts
	 *            Takes a copy of this.
	 */
	public CombinationChart(Collection<? extends Chart> charts) {
		this.charts = new ArrayList<Chart>(charts);
	}

	public void add(Chart chart) {
		if (chart instanceof CombinationChart) {
			List<Chart> cs = ((CombinationChart) chart).getCharts();
			charts.addAll(cs);
			return;
		}
		charts.add(chart);
		// settings
		if (timeZone!=null) {
			chart.setTimeZone(timeZone);
		}
	}

	@Override
	public Axis getAxis(int index) {
		Axis axis = super.getAxis(index);
		if (axis != null)
			return axis;
		if (charts == null || charts.isEmpty())
			return null;
		// merge axes
		for (Chart c : charts) {
			Axis cAxis = c.getAxis(index);
			if (cAxis == null) {
				continue;
			}
			if (axis == null) {
				axis = cAxis;
				continue;
			}
			assert cAxis.getClass() == axis.getClass() : charts + " and "
					+ cAxis.getClass() + " vs " + axis.getClass();
			// merge range?
			if (axis instanceof NumericalAxis) {
				Range cRange = ((NumericalAxis) cAxis).getRange();
				Range range = ((NumericalAxis) axis).getRange();
				Range r2 = new Range(Math.min(range.low, cRange.low), Math.max(
						range.high, cRange.high));
				((NumericalAxis) axis).setRange(r2);
			}
		}
		// something had better carry some axis info
		assert axis != null : this;
		return axis;
	}

	public List<Chart> getCharts() {
		return charts;
	}

	/**
	 * @return map of label-to-chart
	 */
	public Map<Object, Chart> getLabelToChartMap() {
		return labelToChartMap;
	}

	@Override
	public void setShowPoints(boolean showPoints) {
		this.showPoints = showPoints; // only for getDrawType
		
		for (Chart chart : charts) {
			chart.setShowPoints(showPoints);
		}
	}
	
	public void setType(ChartType type) {
		assert type != null : this;
		this.type = type; // only for getType
		
		for (Chart chart : charts) {
			chart.setType(type);
		}
	}
	
	public void setLabelToChartMap(Map<Object, Chart> labelToChartMap) {
		this.labelToChartMap = labelToChartMap;
	}

	/**
	 * Assign a colour to each of the charts.
	 * 
	 * @param rainbow
	 */
	public void setRainbow(Rainbow rainbow) {
		for (int i = 0; i < charts.size(); i++) {
			charts.get(i).setColor(rainbow.getColor(i));
		}
	}

	public void setRange(Range range, int axis) {
		assert range != null : this;
		((NumericalAxis) this.getAxis(axis)).setRange(range);
		for (Chart chart : charts) {
			((NumericalAxis) chart.getAxis(axis)).setRange(range);
		}
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + charts + "]";
	}

	public void setTimeZone(TimeZone tz) {
		this.timeZone = tz;
		for (Chart c : charts) {
			c.setTimeZone(tz);
		}
	}
	
	public void setTimeZone(String tzString) {
		setTimeZone(TimeZone.getTimeZone(tzString));
	}
	
	@Override
	protected JSONObject toJsonObject(JSONObject jsonObject) {
//		try {
			if (this.charts.size() == 0) {
				return jsonObject;
			}
			
			jsonObject = super.toJsonObject(this.charts.get(0).toJsonObject(jsonObject));
			
			if (jsonObject.has("title")) {
				jsonObject.remove("title");
			}
			
			JSONArray seriesList = new JSONArray();
			
			JSONArray chartColors = new JSONArray();
			
			for (Chart chart : this.charts) {
				Series chartSeries = chart.series;
				
				if (chartSeries == null) {
					chartSeries = new Series();
				}
				
				if (chart.title != null) {
					chartSeries.name = chart.title;
					
					chart.title = null;
				}
				
				if (chartSeries.color != null) {
					if (chartColors == null) {
						chartColors = new JSONArray();
					}
					
					chartColors.put(WebUtils.color2html(chartSeries.color));
					
					jsonObject.put("colors", chartColors);
				}
				
				seriesList.put(chartSeries);
			}
			
			if (chartColors.getList().size() > 0) {
				jsonObject.put("colors", chartColors);
			}
			
			if (seriesList.length() > 0) {
				jsonObject.put("series", seriesList);
			}
//		} catch (JSONException e) {
//			// This should not happen - we have ensured that all calls to JSONObject.put have
//			// a key that is not null.
//			jsonObject = new JSONObject();
//		}
		
		return jsonObject;
	}
}
