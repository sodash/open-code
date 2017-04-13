package com.winterwell.maths.chart;

import static org.junit.Assert.assertEquals;

import java.awt.Color;
import java.util.ArrayList;

import org.junit.Test;

import com.winterwell.json.JSONArray;
import com.winterwell.json.JSONException;
import com.winterwell.json.JSONObject;
import com.winterwell.maths.vector.X;
import com.winterwell.maths.vector.XY;

import no.uib.cipr.matrix.Vector;

/**
 * Tests for {@link Series}.
 * 
 * @author Steven King <steven@winterwell.com>
 *
 */
public class SeriesTest {
	@Test(expected=IllegalStateException.class)
	public void setXAxisWithoutIdThrowsIllegalStateException() {
		Axis xAxis = new Axis();
		Series sut = new Series();
		
		sut.setxAxis(xAxis);
	}
	
	@Test(expected=IllegalStateException.class)
	public void setYAxisWithoutIdThrowsIllegalStateException() {
		Axis yAxis = new Axis();
		Series sut = new Series();
		
		sut.setyAxis(yAxis);
	}
	
	@Test
	public void xAxisWithoutIdNotInJsonString() {
		String axisId = "foo";
		Axis axis = new Axis();
		Series sut = new Series();
		
		axis.setId(axisId);
		sut.setxAxis(axis);
		axis.setId(null);
		
		String expected = new JSONObject().toString();
		String actual = sut.toJSONString();
		
		assertEquals(expected, actual);
	}
	
	@Test
	public void yAxisWithoutIdNotInJsonString() {
		String axisId = "foo";
		Axis axis = new Axis();
		Series sut = new Series();
		
		axis.setId(axisId);
		sut.setyAxis(axis);
		axis.setId(null);
		
		String expected = new JSONObject().toString();
		String actual = sut.toJSONString();
		
		assertEquals(expected, actual);
	}
	
	@Test
	public void newSeriesHasEmptyJsonString() throws JSONException {
		String expected = new JSONObject().toString();
		String actual = new Series().toJSONString();
		
		assertEquals(expected, actual);
	}
	
	@Test
	public void seriesNameInJsonString() throws JSONException {
		String name = "foo";
		Series series = new Series();
		
		series.setName(name);
		
		String expected = new JSONObject().put("name", name).toString();
		String actual = series.toJSONString();
		
		
		
		assertEquals(expected, actual);
	}
	
	@Test
	public void seriesTypeInJsonString() throws JSONException {
		ChartType type = ChartType.LINE;
		Series series = new Series();
		
		series.setType(type);
		
		String expected = new JSONObject().put("type", type).toString();
		String actual = series.toJSONString();
		
		assertEquals(expected, actual);
	}
	
	@Test
	public void seriesXAxisInJsonString() throws JSONException {
		String axisId = "foo";
		Axis axis = new Axis();
		axis.setId(axisId);
		
		Series sut = new Series();
		sut.setxAxis(axis);
		
		String expected = new JSONObject().put("xaxis", axisId).toString();
		String actual = sut.toJSONString();
		
		assertEquals(expected, actual);
	}
	
	@Test
	public void seriesYAxisInJsonString() throws JSONException {
		String axisId = "foo";
		Axis axis = new Axis();
		axis.setId(axisId);
		
		Series sut = new Series();
		sut.setyAxis(axis);
		
		String expected = new JSONObject().put("yaxis", axisId).toString();
		String actual = sut.toJSONString();
		
		assertEquals(expected, actual);
	}
	
	@Test
	public void seriesColorNotInJsonString() throws JSONException {
		Color color = new Color(255, 255, 255);
		Color jsonColor = new Color(255, 255, 255);
		
		Series sut = new Series();
		sut.setColor(color);
		
		String expected = new JSONObject().toString();
		String actual = sut.toJSONString();
		
		assertEquals(expected, actual);
	}
	
	@Test
	public void series1dDataInJsonString() throws JSONException {
		double y = 1.0; 
		X point = new X(y);
		ArrayList<Vector> data = new ArrayList<Vector>();
		data.add(point);
		
		Series sut = new Series();
		sut.setData(data);
		
		String expected = new JSONObject().put("data", new JSONArray().put(new JSONObject().put("y", y))).toString();
		String actual = sut.toJSONString();
		
		assertEquals(expected, actual);
	}
	
	@Test
	public void seriess2dDataInJsonString() throws JSONException {
		double x = 1.0; 
		double y = -1.0;
		XY point = new XY(x, y);
		ArrayList<Vector> data = new ArrayList<Vector>();
		data.add(point);
		
		Series sut = new Series();
		sut.setData(data);
		
		String expected = new JSONObject().put("data", new JSONArray().put(new JSONObject().put("x", x).put("y", y))).toString();
		String actual = sut.toJSONString();
		
		assertEquals(expected, actual);
	}
}