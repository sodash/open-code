package com.winterwell.maths.chart;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.winterwell.maths.gui.IFunction;
import com.winterwell.maths.stats.distributions.discrete.IntegerDistribution;
import com.winterwell.maths.timeseries.Datum;
import com.winterwell.maths.timeseries.ListDataStream;
import com.winterwell.maths.vector.XY;
import com.winterwell.utils.Proc;
import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.utils.containers.IntRange;
import com.winterwell.utils.containers.Range;
import com.winterwell.utils.time.Time;
import com.winterwell.utils.web.SimpleJson;
import com.winterwell.utils.web.WebUtils;

import no.uib.cipr.matrix.Vector;

/**
 * @tested {@link RenderWithFlot}
 * @author daniel
 * 
 */
public class RenderWithFlotTest {

	
	private void _testMaxNominalTicks(int n_ticks, String chart_label) {
		// make a chart
		IntegerDistribution d = new IntegerDistribution();
		for (Integer item : new Integer[] { 1, 4, 8, 23, 100 }) {
			d.count(item);
		}
		FiniteDistributionChart chart = new FiniteDistributionChart(d);
		chart.setType(ChartType.COLUMN);
		chart.setTitle(chart_label);

		RenderWithFlot renderer = new RenderWithFlot();
		String test_filename = "test/test-max-nominal-ticks.html";
		File chart_file = new File(test_filename);
		renderer.setMaxNominalTicks(n_ticks);
		renderer.renderToFile(chart, chart_file);
		// test in browser
		WebUtils.display(chart_file);
	}

	@Test
	public void testGetJSONPayload() {
		FunctionChart chart = new FunctionChart(new IFunction() {
			@Override
			public double f(double x) {
				return Math.sin(x);
			}
		}, new Range(-Math.PI, Math.PI * 2));
		chart.setType(ChartType.LINE);
		chart.setTitle("Sin curve from -pi to 2pi");

		CombinationChart cc = new CombinationChart(chart);

		RenderWithFlot render = new RenderWithFlot();

		Map<String, Object> map = render.getJSONPayload(cc);
		assert map.keySet().size() == 2 : map;
		List data = (List) map.get("data");
		assert data.size() == 1 : data;
	}

	@Test
	public void testMaxNominalTicks() {
		this._testMaxNominalTicks(5, "There should be 5 ticks on the x-axis");
		this._testMaxNominalTicks(0, "There should be ~100 ticks on the x-axis");
	}

	@Test
	public void testMultiValuedTimeSeries() {

		ListDataStream abcs = new ListDataStream(3);
		abcs.add(new Datum(new Time(2000, 1, 1), new double[] { 1, 2, 3 }, null));
		abcs.add(new Datum(new Time(2000, 1, 2), new double[] { 2, 3, 4 }, null));
		abcs.add(new Datum(new Time(2000, 1, 3), new double[] { 3, 4, 5 }, null));
		CombinationChart chart = TimeSeriesChart.newMultiValuedChart(abcs,
				Arrays.asList("A", "B", "C"), true);
		chart.setTitle("Test ABC");

		RenderWithFlot r = new RenderWithFlot();
		String html = r.renderToHtml(chart);
		r.renderToBrowser(chart);
		new File("test-output").mkdir();
		new RenderWithFlot().renderToFile(chart, new File(
				"test-output/flot-abc.png"));
	}
	
	@Test
	public void testCombiChartWithEvents() {
		ListDataStream abcs = new ListDataStream(1);
		abcs.add(new Datum(new Time(2000, 1, 1), new double[] {1}, "a"));
		abcs.add(new Datum(new Time(2000, 1, 2), new double[] {2}, "b"));
		abcs.add(new Datum(new Time(2000, 1, 3), new double[] {3}, "c"));
		
		ListDataStream evs = new ListDataStream(1);
		evs.add(new Datum(new Time(2000, 1, 2, 12, 0, 0), new double[] {0}, "An Event!"));
		
		TimeSeriesChart chart1 = new TimeSeriesChart("123", abcs);
		EventChart chart2 = new EventChart(); // FIXME add events!
		CombinationChart chart = new CombinationChart(chart1, chart2);
		chart.setTitle("Test ABC");

		RenderWithFlot r = new RenderWithFlot();
		String html = r.renderToHtml(chart);
		r.renderToBrowser(chart);
		new File("test-output").mkdir();
		new RenderWithFlot().renderToFile(chart, new File(
				"test-output/flot-abc.png"));
	}

	/**
	 * A bug from SoDash Feb 2012
	 */
	@Test
	public void testRenderHistogram() {
		IntegerDistribution timeOfDay = new IntegerDistribution();
		timeOfDay.setProb(14, 0.5);
		timeOfDay.setProb(12, 0.5);

		FiniteDistributionChart chart = new FiniteDistributionChart(timeOfDay);
		NominalAxis axis = (NominalAxis) chart.getAxis(AChart.X);
		IntRange range = timeOfDay.getRange();
		axis.setCategories(range);
		chart.setTitle("Time of Day");

		RenderWithFlot tod = new RenderWithFlot(400, 250);
		tod.setClass("timeofday");

		String html = tod.renderToHtml(chart);
		tod.//renderAndPopupAndBlock(chart); // 
			renderToBrowser(chart);
	}

	@Test
	public void testRenderInteractive() {
		XYChart chart = new XYChart("interactive test");
		chart.setData(Arrays.asList(new XY(1, 1), new XY(2, 2), new XY(3, 2)));
		chart.setType(ChartType.SCATTER);
		RenderWithFlot render = new RenderWithFlot();
		// render.setInteractive("function(){alert();}");
		render.setInteractive(true); //"function (event, pos, item) {alert('You clicked at ' + pos.x + ', ' + pos.y);\n"
//				+ "if (item) {alert('You clicked a point! '+item['datapoint']);console.log(item);\n"
//				+ "	highlight(item.series, item.datapoint);}\n" + "}");
		render.renderToBrowser(chart);
		// new File("test-output").mkdir();
		// new RenderWithFlot().renderToFile(chart, new
		// File("test-output/flot.png"));
	}

	@Test
	public void testRenderPieChart() {
		IntegerDistribution d = new IntegerDistribution();

		for (double item : new double[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11,
				12, 13, 14, 15 }) {
			d.setProb(item, item);
		}

		{
			PieChart chart = new PieChart("PieChart % test", d);
			List<Vector> pcd = chart.getData();
			// System.out.println(pcd);
			RenderWithFlot render = new RenderWithFlot();
			render.renderToBrowser(chart);
		}
	}

	@Test
	public void testRenderScatterPlot() {
		List data = Arrays.asList(new XY(1, 1), new XY(1, 2), new XY(2, 2));
		ScatterPlot sp = new ScatterPlot(data);
		RenderWithFlot rwf = new RenderWithFlot();
		rwf.setInteractive(true);
		rwf.renderToBrowser(sp);
//		System.out.println(html);
//		new RenderWithFlot().renderAndPopupAndBlock(sp);
	}

	


	@Test
	public void testRenderScatterPlotWithTooltips() {
		List data = Arrays.asList(new XY(1, 1), new XY(1, 2), new XY(2, 2));
		ScatterPlot sp = new ScatterPlot(data);
		sp.setTitle("My Data");
		sp.setDataLabels(new ArrayMap(1, "My point! 1 2"));
		RenderWithFlot rwf = new RenderWithFlot();
		rwf.setInteractive(true);
		String labelJson = new SimpleJson().toJson(sp.getDataLabels());
		rwf.setTooltipFnName("options.tooltips.message = function(item){var lbl="+labelJson+"[item.dataIndex]; return lbl? lbl : item.series.label;}");
		rwf.renderToBrowser(sp);
//		System.out.println(html);
//		new RenderWithFlot().renderAndPopupAndBlock(sp);
	}

	@Test
	public void testRenderSinFunction() {
		FunctionChart chart = new FunctionChart(new IFunction() {
			@Override
			public double f(double x) {
				return Math.sin(x);
			}
		}, new Range(-Math.PI, Math.PI * 2));
		chart.setType(ChartType.LINE);
		chart.setTitle("Sin curve from -pi to 2pi");
		// String html = new RenderWithFlot().renderToHtml(chart);
		new RenderWithFlot().renderToBrowser(chart);
		// new File("test-output").mkdir();
		// new RenderWithFlot().renderToFile(chart, new
		// File("test-output/flot.png"));
	}

	@Test
	public void testRenderToHtml() {
		IntegerDistribution d = new IntegerDistribution();
		for (Integer item : new Integer[] { 40, 1, 2, 5, 6, 1, 1, 2 }) {
			d.count(item);
		}

		assert d.getRange().high == 40;

		FiniteDistributionChart chart = new FiniteDistributionChart(d);
		chart.setType(ChartType.COLUMN);
		chart.setTitle("A chart of some integers");
		String html = new RenderWithFlot().renderToHtml(chart);
		new RenderWithFlot().renderToBrowser(chart);
		new File("test-output").mkdir();
		new RenderWithFlot().renderToFile(chart, new File(
				"test-output/flot.png"));
	}

	@Test
	public void testRenderToPdf() {
		// make a chart
		IntegerDistribution d = new IntegerDistribution();
		for (Integer item : new Integer[] { 40, 1, 2, 5, 6, 1, 1, 2 }) {
			d.count(item);
		}
		FiniteDistributionChart chart = new FiniteDistributionChart(d);
		chart.setType(ChartType.COLUMN);
		chart.setTitle("A chart of some integers");

		// generate html
		String html = new RenderWithFlot().renderToHtml(chart);

		// add scripts
		String page = "<html><head><title>" + chart.getTitle() + "</title>"
				+ RenderWithFlot.JQUERY_DEPENDENCY + "\n"
				+ RenderWithFlot.DEPENDENCIES + "</head><body>" + html
				+ "</body></html>";

		// test in browser
		WebUtils.display(page);

		// test pdf
		File pdf = new File("test/test-chart.pdf");
		WebUtils.renderToPdf(page, pdf, false);
		Proc p = new Proc("evince " + pdf.getAbsolutePath());
		p.run();
	}

}
