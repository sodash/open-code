package com.winterwell.maths.chart;

import java.util.ArrayList;
import java.util.List;

import com.winterwell.json.JSONArray;

import no.uib.cipr.matrix.Vector;

/**
 * LinkedCharts are drawn separately, but have linked legends, so that hiding/showing a series from
 * one chart, does the same for all linked charts.
 * 
 * @author Steven
 * @see LinkedChartDemo
 * @testedby {@link LinkedChartTest}
 */
public class LinkedChart extends AChart {
	protected List<AChart> charts;
	
	public List<AChart> getCharts() {
		return charts;
	}

	public void setCharts(List<AChart> charts) {
		this.charts = charts;
	}
	
	public void addChart(AChart chart) {
		if (this.charts == null) {
			this.charts = new ArrayList<AChart>();
		}
		
		this.charts.add(chart);
	}
	
	@Override
	public List<Vector> getData() {
		// TODO Auto-generated method stub
		return null;
	}
	
	/**
	 * FIXME Change this json format.
	 * An array of charts does not communicate "these are linked"
	 * as opposed to just "here's some charts"
	 * We should use something closer to the standard chart format.
	 */
	@Override
	public String toJSONString() {
		JSONArray jsonArray = new JSONArray();
		
		if (this.charts != null) {
			for (AChart chart : this.charts) {
				jsonArray.put(chart);
			}
		}
		
		return jsonArray.toString();
	}
}
