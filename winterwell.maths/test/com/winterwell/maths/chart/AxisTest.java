package com.winterwell.maths.chart;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.winterwell.json.JSONException;
import com.winterwell.json.JSONObject;
import com.winterwell.utils.containers.Range;

public class AxisTest {
	@Test
	public void newAxisHasEmptyJsonString() throws JSONException {
		String expected = new JSONObject().toString();
		String actual = new Axis().toJSONString();
		
		assertEquals(expected, actual);
	}
	
	@Test
	public void chartIdInJsonString() throws JSONException {
		String id = "foo";
		
		Axis sut = new Axis();
		sut.setId(id);
		
		String expected = new JSONObject().put("id", id).toString();
		String actual = sut.toJSONString();
		
		assertEquals(expected, actual);
	}
	
	@Test
	public void chartTitleInJsonString() throws JSONException {
		String title = "foo";
		
		Axis sut = new Axis();
		sut.setTitle(title);
		
		String expected = new JSONObject().put("title", new JSONObject().put("text", title)).toString();
		String actual = sut.toJSONString();
		
		assertEquals(expected, actual);
	}
	
	@Test
	public void chartRangeInJsonString() throws JSONException {
		double min = -1;
		double max = 1;
		Range range = new Range(min, max);
		
		Axis sut = new Axis();
		sut.setRange(range);
		
		String expected = new JSONObject().put("min", min).put("max", max).toString();
		String actual = sut.toJSONString();
		
		assertEquals(expected, actual);
	}
	
	@Test
	public void chartTypeInJsonString() throws JSONException {
		AxisType type = AxisType.DATETIME;
		
		Axis sut = new Axis();
		sut.setType(type);
		
		String expected = new JSONObject().put("type", type).toString();
		String actual = sut.toJSONString();
		
		assertEquals(expected, actual);
	}
}
