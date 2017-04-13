package com.winterwell.maths.chart;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;

import org.junit.Test;

import com.winterwell.json.JSONArray;
import com.winterwell.json.JSONException;
import com.winterwell.json.JSONObject;
import com.winterwell.maths.vector.X;

import no.uib.cipr.matrix.Vector;

/**
 * Tests for {@link CombinationChart}.
 * 
 * @author Steven King <steven@winterwell.com>
 *
 */
public class CombinationChartTest {
	@Test
	public void newChartHasEmptyJsonString() {
		CombinationChart sut = new CombinationChart();
		
		String expected = new JSONObject().toString();
		String actual = sut.toJSONString();
		
		assertEquals(expected, actual);
	}
	
	@Test
	public void combinationChartMultipleSeriesJsonString() throws JSONException {
		double y = 1.0;
		X point = new X(y);
		
		ArrayList<Vector> data = new ArrayList<Vector>();
		data.add(point);
		
		Series series = new Series();
		series.setData(data);
		
		Chart chart1 = new Chart();
		Chart chart2 = new Chart();
		
		chart1.setData(data);
		chart2.setData(data);
		
		CombinationChart sut = new CombinationChart();
		sut.add(chart1);
		sut.add(chart2);
		
		String expected = new JSONObject().put("series", new JSONArray().put(series).put(series)).put("xAxis", new JSONObject().put("min", 1).put("max", 1)).toString();
		
		String actual = sut.toJSONString();
		
		assertEquals(expected, actual);
	}
	
	@Test
	public void combinationChartSeriesNameInJsonString() throws JSONException {
		String seriesName = "foo";
		
		Series series = new Series();
		Series seriesWithName = new Series();
		seriesWithName.setName(seriesName);
		Chart chart = new Chart();
		
		chart.setSeries(series);
		chart.setTitle(seriesName);
		
		CombinationChart sut = new CombinationChart();
		sut.add(chart);
		
		String expected = new JSONObject().put("series", new JSONArray().put(seriesWithName)).toString();
		
		String actual = sut.toJSONString();
		
		assertEquals(expected, actual);
	}
}