package com.winterwell.maths.chart;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Tests for {@link LegendAlignment}.
 * 
 * @author Steven King <steven@winterwell.com>
 */
public class LegendAlignmentTest {
	@Test
	public void legendAlignmentLeftReturnsCorrectJsonString() {
		String actual = LegendAlignment.LEFT.toJSONString();
		String expected = "\"left\"";
		
		assertEquals("LegendAlignment.LEFT returns correct JSON string", expected, actual);
	}
	
	@Test
	public void legendAlignmentMiddleReturnsCorrectJsonString() {
		String actual = LegendAlignment.CENTER.toJSONString();
		String expected = "\"center\"";
		
		assertEquals("LegendAlignment.CENTER returns correct JSON string", expected, actual);
	}
	
	@Test
	public void legendAlignmentRightReturnsCorrectJsonString() {
		String actual = LegendAlignment.RIGHT.toJSONString();
		String expected = "\"right\"";
		
		assertEquals("LegendAlignment.RIGHT returns correct JSON string", expected, actual);
	}
}
