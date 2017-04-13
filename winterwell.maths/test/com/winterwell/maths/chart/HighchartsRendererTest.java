package com.winterwell.maths.chart;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Test;

/**
 * Tests for {@link HighchartsRenderer}.
 * 
 * @author Steven King <steven@winterwell.com>
 *
 */
public class HighchartsRendererTest {
	private String getAddedElementHtml(String id) {
		return "<div id=\"" + id + "\"></div>";
	}
	
	private String getScriptHtml(String chartJson) {
		return "<script>\n(function(){$(function(){var chart=JSON.parse('" + chartJson + "');Chart.render(chart);});}())\n</script>";
	}
	
	@Test
	public void renderToHtmlWithChartElementSetGeneratesCorrectHtml() {
		String element = "foo";
		Chart chart = new Chart();
		chart.setElement(element);
		
		String expected = getScriptHtml(chart.toJSONString());
		HighchartsRenderer r = new HighchartsRenderer();
		String actual = r.renderToHtml(chart);
		
		assertEquals(expected, actual);
	}
	
	@Test
	public void renderToHtmlWithNoChartElementSetGeneratesCorrectHtml() {
		Chart chart = new Chart();
		Pattern pattern = Pattern.compile("<div id=\"(.*)\">");
		HighchartsRenderer r = new HighchartsRenderer();
		String actual = r.renderToHtml(chart);
		
		Matcher matcher = pattern.matcher(actual);
		
		if (!matcher.find()) {
			fail("Rendered html does not contain element with id.");
		}
		
		String id = matcher.group(1);
		
		Chart chartWithElementSet = new Chart();
		chartWithElementSet.setElement(id);
		
		String expected = getAddedElementHtml(id) + getScriptHtml(chartWithElementSet.toJSONString());
		
		assertEquals(expected, actual);
	}
	
	@Test
	public void renderToHtmlWithLinkedChartElementSetGeneratesCorrectHtml() {
		String element1 = "foo";
		String element2 = "bar";
		
		Chart chart1 = new Chart();
		chart1.setElement(element1);
		Chart chart2 = new Chart();
		chart2.setElement(element2);
		
		LinkedChart chart = new LinkedChart();
		chart.addChart(chart1);
		chart.addChart(chart2);
		
		String expected = getScriptHtml(chart.toJSONString());
		HighchartsRenderer r = new HighchartsRenderer();
		String actual = r.renderToHtml(chart);
		
		assertEquals(expected, actual);
	}
	
	@Test
	public void renderToHtmlWithNoLinkedChartElementSetGeneratesCorrectHtml() {
		Chart chart1 = new Chart();
		Chart chart2 = new Chart();
		
		LinkedChart chart = new LinkedChart();
		chart.addChart(chart1);
		chart.addChart(chart2);
		
		Pattern pattern = Pattern.compile("<div id=\"(\\w*)\">");
		HighchartsRenderer r = new HighchartsRenderer();
		String actual = r.renderToHtml(chart);
		
		Matcher matcher = pattern.matcher(actual);
		
		if (!matcher.find()) {
			fail("Rendered html does not contain any elements with id.");
		}
		
		String id1 = matcher.group(1);
		
		if (!matcher.find()) {
			fail("Rendered html does not contain two elements with id.");
		}
		
		String id2 = matcher.group(1);
		
		Chart chart3 = new Chart();
		chart3.setElement(id1);
		Chart chart4 = new Chart();
		chart4.setElement(id2);
		
		LinkedChart chartWithElementSet = new LinkedChart();
		chartWithElementSet.addChart(chart3);
		chartWithElementSet.addChart(chart4);
		
		String expected = getAddedElementHtml(id1) + getAddedElementHtml(id2) + getScriptHtml(chartWithElementSet.toJSONString());
		
		assertEquals(expected, actual);
	}
	
	@Test
	public void renderToHtmlWithNoChartElementDoesNotSetChartElement() {
		Chart chart = new Chart();		
		HighchartsRenderer r = new HighchartsRenderer();
		r.renderToHtml(chart);
		
		String expected = null;
		String actual = chart.getElement();
		
		assertEquals(expected, actual);
	}
}