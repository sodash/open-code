package com.winterwell.maths.chart;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Tests for {@link ChartType}.
 * 
 * @author Steven King <steven@winterwell.com>
 *
 */
public class ChartTypeTest {
	@Test
	public void chartTypeLineReturnsCorrectJsonString() {
		String actual = ChartType.LINE.toJSONString();
		String expected = "\"line\"";
		
		assertEquals("ChartType.LINE returns correct JSON string", expected, actual);
	}
	
	@Test
	public void chartTypeAreaReturnsCorrectJsonString() {
		String actual = ChartType.AREA.toJSONString();
		String expected = "\"area\"";
		
		assertEquals("ChartType.AREA returns correct JSON string", expected, actual);
	}
	
	@Test
	public void chartTypeColumnReturnsCorrectJsonString() {
		String actual = ChartType.COLUMN.toJSONString();
		String expected = "\"column\"";
		
		assertEquals("ChartType.COLUMN returns correct JSON string", expected, actual);
	}
	
	@Test
	public void chartTypeBarReturnsCorrectJsonString() {
		String actual = ChartType.BAR.toJSONString();
		String expected = "\"bar\"";
		
		assertEquals("ChartType.BAR returns correct JSON string", expected, actual);
	}
	
	@Test
	public void chartTypeScatterReturnsCorrectJsonString() {
		String actual = ChartType.SCATTER.toJSONString();
		String expected = "\"scatter\"";
		
		assertEquals("ChartType.SCATTER returns correct JSON string", expected, actual);
	}
	
	@Test
	public void chartTypePieReturnsCorrectJsonString() {
		String actual = ChartType.PIE.toJSONString();
		String expected = "\"pie\"";
		
		assertEquals("ChartType.PIE returns correct JSON string", expected, actual);
	}
}
