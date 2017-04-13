package com.winterwell.maths.chart;

import static org.junit.Assert.assertEquals;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.winterwell.json.JSONArray;
import com.winterwell.json.JSONException;
import com.winterwell.json.JSONObject;
import com.winterwell.maths.vector.X;
import com.winterwell.utils.web.WebUtils;

import no.uib.cipr.matrix.Vector;

/**
 * Tests for {@link Chart}.
 * 
 * @author Steven King <steven@winterwell.com>
 *
 */
public class AChartTest {
	private JSONObject newChart = new JSONObject().put("title", new JSONObject().put("text", JSONObject.NULL));
	@Test
	public void newChartHasNullTitleInJsonString() {
		Chart sut = new Chart();
		
		String expected = newChart.toString();
		String actual = sut.toJSONString();
		
		assertEquals(expected, actual);
	}
	
	@Test
	public void chartElementInJsonString() throws JSONException {
		String element = new String("foo");
		
		Chart sut = new Chart();
		sut.setElement(element);
		
		String expected =newChart.put("chart", new JSONObject().put("renderTo", element)).toString();
		String actual = sut.toJSONString();
		
		assertEquals(expected, actual);
	}
	
	@Test
	public void seriesColorsInJsonString() throws JSONException  {
		Color color = Color.WHITE;
		Series series = new Series();
		series.setColor(color);
		
		Chart sut = new Chart();
		sut.setSeries(series);
		
		String expected = newChart.put("series", new JSONArray().put(new JSONObject())).put("colors", new JSONArray().put(WebUtils.color2html(color))).toString();
		String actual = sut.toJSONString();
		
		assertEquals(expected, actual);
	}
	
	@Test
	public void chartTypeInJsonString() throws JSONException {
		ChartType type = ChartType.LINE;
		
		Chart sut = new Chart();
		sut.setType(type);
		
		String expected = newChart.put("chart", new JSONObject().put("type", type)).toString();
		String actual = sut.toJSONString();
		
		assertEquals(expected, actual);
	}
	
	@Test
	public void chartTitleInJsonString() throws JSONException {
		String title = "foo";
		
		Chart sut = new Chart();
		sut.setTitle(title);
		
		String expected = newChart.put("title", new JSONObject().put("text", title)).toString();
		String actual = sut.toJSONString();
		
		assertEquals(expected, actual);
	}
	
	@Test
	public void chartSubtitleInJsonString() throws JSONException {
		String subtitle = "foo";
		
		Chart sut = new Chart();
		sut.setSubtitle(subtitle);
		
		String expected = newChart.put("subtitle", new JSONObject().put("text", subtitle)).toString();
		String actual = sut.toJSONString();
		
		assertEquals(expected, actual);
	}
	
	@Test
	public void chartSeriesJsonString() throws JSONException {
		double y = 1.0;
		X point = new X(y);
		List<Vector> data = new ArrayList<Vector>();
		Series series = new Series();
		data.add(point);
		series.setData(data);
		
		Chart sut = new Chart();
		sut.setSeries(series);
		
		String expected = newChart.put("series", new JSONArray().put(series)).toString();
		String actual = sut.toJSONString();
		
		assertEquals(expected, actual);
	}
	
	@Test
	public void chartSeriesUrlInJsonString() throws JSONException {
		String seriesURI = "foo";
		
		Chart sut = new Chart();
		sut.setSeriesURI(seriesURI);
		
		String expected = newChart.put("series", seriesURI).toString();
		String actual = sut.toJSONString();
		
		assertEquals(expected, actual);
	}
	
	@Test
	public void chartXAxisJsonString() throws JSONException {
		Axis axis= new Axis();
		
		Chart sut = new Chart();
		sut.setAxis(0, axis);
		
		String expected = newChart.put("xAxis", axis).toString();
		String actual = sut.toJSONString();
		
		assertEquals(expected, actual);
	}
	
	@Test
	public void chartYAxisJsonString() throws JSONException {
		Axis axis= new Axis();
		
		Chart sut = new Chart();
		sut.setAxis(1, axis);
		
		String expected = newChart.put("yAxis", axis).toString();
		String actual = sut.toJSONString();
		
		assertEquals(expected, actual);
	}
	
	@Test
	public void chartLegendInJsonString() throws JSONException {
		Legend legend = new Legend();
		
		Chart sut = new Chart();
		sut.setLegend(legend);
		
		String expected = newChart.put("legend", legend).toString();
		String actual = sut.toJSONString();
		
		assertEquals(expected, actual);
	}
	
	@Test
	public void chartOptionsInJsonString() throws JSONException {
		JSONObject options = new JSONObject();
		
		Chart sut = new Chart();
		sut.setOptions(options);
		
		String expected = newChart.put("options", options).toString();
		String actual = sut.toJSONString();
		
		assertEquals(expected, actual);
	}
}