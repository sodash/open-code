package com.winterwell.maths.chart;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.winterwell.json.JSONArray;
import com.winterwell.json.JSONException;

/**
 * Tests for {@link Chart}.
 * 
 * @author Steven King <steven@winterwell.com>
 *
 */
public class LinkedChartTest {
	@Test
	public void newLinkedChartHasEmptyJsonString() {
		LinkedChart sut = new LinkedChart();
		
		String expected = new JSONArray().toString();
		String actual = sut.toJSONString();
		
		assertEquals(expected, actual);
	}
	
	@Test
	public void linkedChartChartsInJsonString() throws JSONException {
		TimeSeriesChart chart1 = new TimeSeriesChart();
		TimeSeriesChart chart2 = new TimeSeriesChart();
		
		LinkedChart sut = new LinkedChart();
		sut.addChart(chart1);
		sut.addChart(chart2);
		
		String expected = new JSONArray().put(chart1).put(chart2).toString();
		String actual = sut.toJSONString();
		
		assertEquals(expected, actual);
	}
}