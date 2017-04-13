package com.winterwell.maths.chart;

import org.junit.Test;

import com.winterwell.maths.stats.distributions.discrete.IntegerDistribution;

public class PieChartDemo {

	@Test
	public void testPieChart15() {
		IntegerDistribution d = new IntegerDistribution();
		for (Double item : new double[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11,
				12, 13, 14, 15 }) {
			d.setProb(item, item);
		}
		{
			PieChart chart = new PieChart("PieChart test 15", d);
			RenderWithFlot render = new RenderWithFlot();
			render.renderToBrowser(chart);
		}
	}

	@Test
	public void testPieChart3() {
		IntegerDistribution d = new IntegerDistribution();
		for (double item : new double[] { 1, 2, 3 }) {
			d.setProb(item, item);
		}
		{
			PieChart chart = new PieChart("PieChart test 3", d);
			RenderWithFlot render = new RenderWithFlot();
			render.renderToBrowser(chart);
		}
	}

	@Test
	public void testRenderPieChartAbsLabels() {
		IntegerDistribution d = new IntegerDistribution();

		for (Double item : new double[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11,
				12, 13, 14, 15 }) {
			d.setProb(item, item);
		}
		{
			PieChart chart = new PieChart<Double>("PieChart abs test", d);
			chart.setLabeller(PieChart.LABEL_WITH_ABS_VALUES);
			RenderWithFlot render = new RenderWithFlot();
			render.renderToBrowser(chart);
		}
	}


}
