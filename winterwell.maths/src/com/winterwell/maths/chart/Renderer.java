package com.winterwell.maths.chart;

import java.awt.Image;
import java.io.File;
import java.util.HashMap;
import java.util.UUID;

import com.winterwell.utils.gui.GuiUtils;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.web.WebUtils2;

/**
 * A basic renderer for a {@link Chart}. This should be subclassed for use with
 * specific rendering libraries.
 * 
 * @author Steven King <steven@winterwell.com>
 *
 */
public abstract class Renderer {
	/**
	 * Creates a block of html that renders a {@link AChart}. Does not manage
	 * dependencies; these must be handled elsewhere for the image to render
	 * correctly.
	 * 
	 * If the chart has no element set, then an element will be included in the
	 * generated html string. If the element is set, then it will be assumed
	 * that the element will be included elsewhere. 
	 * 
	 * @param chart The chart to render in the block of html.
	 * @return A string of html to render the chart.
	 */
	public String renderToHtml(AChart chart) {
		String element = chart.getElement();
		String html = "";
		boolean isUsingRandomElement = false;
		
		if (element == null) {
			isUsingRandomElement = true;
			
			html = createAndSetChartElement(chart, html);
		}
		
		html = createChartRenderingScript(chart, html);
		
		if (isUsingRandomElement) {
			chart.setElement(null);
		}
		
		return html;
	}
	
	/**
	 * Creates a block of html that renders linked {@link AChart}. Does not
	 * manage dependencies; these must be handled elsewhere for the image to
	 * render correctly.
	 * 
	 * If any of the linked charts have no element set, then an element will be
	 * included in the generated html string. If an element is set, then it
	 * will be assumed that the element will be included elsewhere. 
	 * 
	 * @param linkedChart The linked charts to render in the block of html.
	 * @return A string of html to render the linked charts.
	 */
	public String renderToHtml(LinkedChart linkedChart) {
//		String element = linkedChart.getElement();
		String html = "";
		HashMap<AChart, Boolean> isUsingRandomElement = new HashMap<AChart, Boolean>();
		
		for (AChart chart : linkedChart.charts) {
			if (chart.getElement() == null) {
				isUsingRandomElement.put(chart, true);
				
				html = createAndSetChartElement(chart, html);
			} else {
				isUsingRandomElement.put(chart, false);
			}
		}
		
		html = createChartRenderingScript(linkedChart, html);
		
		for (AChart chart : linkedChart.charts) {
			if (isUsingRandomElement.get(chart)) {
				chart.setElement(null);
			}
		}
		
		return html;
	}
	
	/**
	 * 
	 * @param html
	 * @return The id of the created HTML element.
	 */
	private String createAndSetChartElement(AChart chart, String html) {
		String element = "a" + UUID.randomUUID().toString().replace('-', '\0').substring(0, 7);
		
		html += "<div id=\"" + element + "\"></div>";
		
		chart.setElement(element);
		
		return html;
	}
	
	private String createChartRenderingScript(AChart chart, String html) {
		html += "<script>\n(function(){$(function(){"
				+ "var chart=JSON.parse('" + chart.toJSONString() + "');Chart.render(chart);"
				+ "});}())\n</script>";
		
		return html;
	}
	
	/**
	 * Creates a complete html page containing the html to render the supplied
	 * {@link Chart}, including the handling of all dependencies.
	 * 
	 * @param chart The chart to render in the page.
	 * @param file The file in which to store the html page.
	 */
	public abstract String renderToHtmlPage(AChart chart);
	
	/**
	 * Creates an {@link Image} of the rendered {@link AChart}.
	 * 
	 * @param chart The chart to render an image of.
	 * @return The image of the rendered chart.
	 */
	public abstract Image renderToImage(AChart chart, ImageFormat format);
	
	/**
	 * Renders a {@link AChart}, and saves it to a {@link File}.
	 * 
	 * @param chart The chart to render.
	 * @param file The file in which to store the rendered {@link Chart}.
	 */
	public abstract void renderToFile(AChart chart, File file);

	/**
	 * Convenience method for displaying a chart. Does nothing if running in headless mode.
	 * @see AChart.chart()
	 * @param chart
	 * @param block
	 */
	public static void popup(AChart chart) {
		if ( ! GuiUtils.isInteractive()) {
			Log.i("render", "Skip (headless mode): "+chart);
			return;
		}
		Renderer r = getDefault();
		String h = r.renderToHtmlPage(chart);
		WebUtils2.display(h);
	}

	public static Renderer getDefault() {
		return new HighchartsRenderer();
	}
	
}
