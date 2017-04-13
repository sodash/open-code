package com.winterwell.maths.chart;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Tests for {@link LegendVerticalAlignment}.
 * 
 * @author Steven King <steven@winterwell.com>
 */
public class LegendVerticalAlignmentTest {
	@Test
	public void legendVerticalAlignmentTopReturnsCorrectJsonString() {
		String actual = LegendVerticalAlignment.TOP.toJSONString();
		String expected = "\"top\"";
		
		assertEquals("LegendAlignment.TOP returns correct JSON string", expected, actual);
	}
	
	@Test
	public void legendVerticalAlignmentMiddleReturnsCorrectJsonString() {
		String actual = LegendVerticalAlignment.MIDDLE.toJSONString();
		String expected = "\"middle\"";
		
		assertEquals("LegendAlignment.MIDDLE returns correct JSON string", expected, actual);
	}
	
	@Test
	public void legendVerticalAlignmentBottomReturnsCorrectJsonString() {
		String actual = LegendVerticalAlignment.BOTTOM.toJSONString();
		String expected = "\"bottom\"";
		
		assertEquals("LegendAlignment.BOTTOM returns correct JSON string", expected, actual);
	}
}
