package com.winterwell.maths.chart;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.winterwell.json.JSONException;
import com.winterwell.json.JSONObject;

/**
 * Tests for {@link Legend}.
 * 
 * @author Steven King <steven@winterwell.com>
 */
public class LegendTest {
	@Test
	public void newLegendHasEmptyJsonString() throws JSONException {
		Legend sut = new Legend();
		
		String expected = new JSONObject().toString();
		String actual = sut.toJSONString();
		
		assertEquals(expected, actual);
	}
	
	@Test
	public void legendVisibleNotInJsonString() throws JSONException {
		boolean visibility = true;
		
		Legend sut = new Legend();
		sut.setVisible(visibility);
		
		String expected = new JSONObject().toString();
		String actual = sut.toJSONString();
		
		assertEquals(expected, actual);
	}
	
	@Test
	public void legendHiddenInJsonString() throws JSONException {
		boolean visibility = false;
		
		Legend sut = new Legend();
		sut.setVisible(visibility);
		
		String expected = new JSONObject().put("enabled", visibility).toString();
		String actual = sut.toJSONString();
		
		assertEquals(expected, actual);
	}
	
	@Test
	public void legendTitleInJsonString() throws JSONException {
		String title = "foo";
		
		Legend sut = new Legend();
		sut.setTitle(title);
		
		String expected = new JSONObject().put("title", new JSONObject().put("text", title)).toString();
		String actual = sut.toJSONString();
		
		assertEquals(expected, actual);
	}
	
	@Test
	public void legendAlignmentInJsonString() throws JSONException {
		LegendAlignment alignment = LegendAlignment.LEFT;
		
		Legend sut = new Legend();
		sut.setAlignment(alignment);
		
		String expected = new JSONObject().put("align", alignment).toString();
		String actual = sut.toJSONString();
		
		assertEquals(expected, actual);
	}
	
	@Test
	public void legendVerticalAlignmentInJsonString() throws JSONException {
		LegendVerticalAlignment verticalAlignment = LegendVerticalAlignment.TOP;
		
		Legend sut = new Legend();
		sut.setVerticalAlignment(verticalAlignment);
		
		String expected = new JSONObject().put("verticalAlign", verticalAlignment).toString();
		String actual = sut.toJSONString();
		
		assertEquals(expected, actual);
	}
}
