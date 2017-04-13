package com.winterwell.maths.chart;

import java.io.File;
import java.util.List;

import org.junit.Test;

import com.winterwell.maths.stats.distributions.d1.HistogramData;
import com.winterwell.maths.stats.distributions.discrete.IntegerDistribution;
import com.winterwell.utils.MathUtils;
import com.winterwell.utils.io.CSVReader;
import com.winterwell.utils.io.FileUtils;
import com.winterwell.utils.web.WebUtils;

import no.uib.cipr.matrix.Vector;

public class HistogramChartTest {

	@Test
	public void testDraw() {
		IntegerDistribution timeOfDay = new IntegerDistribution();
		timeOfDay.setProb(14, 0.5);
		timeOfDay.setProb(12, 0.5);
		timeOfDay.setProb(1, 0.25);

		assert timeOfDay.prob(14) == 0.5;
		assert timeOfDay.prob(14) == 0.5;

		HistogramChart chart = new HistogramChart(timeOfDay);
		List<Vector> data = chart.getData();
		System.out.println(data);
		assert !data.isEmpty();
		// assert data.contains(new XY(12, 0.5)) : data;

		HighchartsRenderer render = new HighchartsRenderer();
		String html = render.renderToHtmlPage(chart);
		WebUtils.display(html);
		
		new RenderWithFlot(100, 100).renderAndPopupAndBlock(chart);
	}
	
	@Test
	public void testLogPlotFile() {
//		File f = GuiUtils.selectFile("Pick a .csv file", null);
		File f = new File(FileUtils.getWinterwellDir(), "tv2/halo/test-data/Audience_Data_sample-no0s.csv");
		char sep = CSVReader.guessSeparator(f);
		CSVReader r = new CSVReader(f, sep);
		LogGridInfo gridInfo = new LogGridInfo(10*1000000, 100);
		HistogramData dist = new HistogramData(gridInfo);
		for (String[] row : r) {
			String n = row[0];
			if ( ! MathUtils.isNumber(n)) continue;
			dist.count(MathUtils.num(n));
		}
		HistogramChart chart = new HistogramChart(dist);
		NumericalAxis axis = new NumericalAxis();
		axis.setType(AxisType.LOGARITHMIC);
		axis.setGrid(gridInfo);
		chart.setAxis(chart.X, axis);
		List<Vector> data = chart.getData();
		System.out.println(data);
		assert !data.isEmpty();
		// assert data.contains(new XY(12, 0.5)) : data;

		HighchartsRenderer render = new HighchartsRenderer();
		String html = render.renderToHtmlPage(chart);
		WebUtils.display(html);
		
//		new RenderWithFlot(100, 100).renderAndPopupAndBlock(chart);
	}

}
