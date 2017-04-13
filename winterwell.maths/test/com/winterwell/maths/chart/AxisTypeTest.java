package com.winterwell.maths.chart;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Tests for {@link AxisType}.
 * 
 * @author Steven King <steven@winterwell.com>
 *
 */
public class AxisTypeTest {
	@Test
	public void axisTypeDatetimeReturnsCorrectJsonString() {
		String actual = AxisType.DATETIME.toJSONString();
		String expected = "\"datetime\"";
		
		assertEquals("AxisType.DATETIME returns correct JSON string", expected, actual);
	}
	
	@Test
	public void axisTypeLinearReturnsCorrectJsonString() {
		String actual = AxisType.LINEAR.toJSONString();
		String expected = "\"linear\"";
		
		assertEquals("AxisType.LINEAR returns correct JSON string", expected, actual);
	}
	
	@Test
	public void axisTypeLogarithmicReturnsCorrectJsonString() {
		String actual = AxisType.LOGARITHMIC.toJSONString();
		String expected = "\"logarithmic\"";
		
		assertEquals("AxisType.LOGARITHMIC returns correct JSON string", expected, actual);
	}
}
