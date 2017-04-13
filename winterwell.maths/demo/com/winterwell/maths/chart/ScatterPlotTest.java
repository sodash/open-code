package com.winterwell.maths.chart;

import org.junit.Test;

import com.winterwell.maths.datastorage.DataSet;
import com.winterwell.maths.stats.distributions.GaussianBall;
import com.winterwell.maths.stats.distributions.d1.Gaussian1D;
import com.winterwell.maths.timeseries.DataUtils;
import com.winterwell.maths.timeseries.Datum;
import com.winterwell.maths.timeseries.ListDataStream;
import com.winterwell.maths.vector.XY;
import com.winterwell.maths.vector.XYZ;

public class ScatterPlotTest {

	@Test
	public void testMultiColor() {
		ListDataStream ld = new ListDataStream(2);
		GaussianBall as = new GaussianBall(new XY(0, 0), 1);
		ld.addAll(DataUtils.sample(as, 100, "A"));
		GaussianBall bs = new GaussianBall(new XY(1, 0), 1);
		ld.addAll(DataUtils.sample(bs, 100, "B"));
		GaussianBall cs = new GaussianBall(new XY(1, 1), 1);
		ld.addAll(DataUtils.sample(cs, 100, "C"));

		CombinationChart plot = ScatterPlot.multiColor(ld);

		RenderWithFlot render = new RenderWithFlot();
		render.renderToBrowser(plot);
	}

	@Test
	public void testMultiPlot() {
		ListDataStream ld = new ListDataStream(3);
		GaussianBall as = new GaussianBall(new XYZ(0, 0, 0), 1);
		ld.addAll(DataUtils.sample(as, 100, "A"));
		GaussianBall bs = new GaussianBall(new XYZ(0, 0, 2), 1);
		ld.addAll(DataUtils.sample(bs, 100, "B"));

		DataSet dataset = new DataSet("test", "x", "y", "z");
		dataset.setData(ld);

		CombinationChart plot = ScatterPlot.multiPlot(dataset, 1, 2);

		RenderWithFlot render = new RenderWithFlot();
		// for (CombinationChart plot : plots) {
		render.renderToBrowser(plot);
		// }
	}

	@Test
	public void testWithPCA() {
		ListDataStream ld = new ListDataStream(2);
		GaussianBall as = new GaussianBall(new XY(0, 0), 1);
		ld.addAll(DataUtils.sample(as, 100, "A"));
		GaussianBall bs = new GaussianBall(new XY(0, 3), 1);
		ld.addAll(DataUtils.sample(bs, 100, "B"));

		// y = x + noise
		Gaussian1D noise = new Gaussian1D(0, 0.1);
		ListDataStream ld3 = new ListDataStream(3);
		for (Datum datum : ld) {
			ld3.add(new Datum(datum.time, new XYZ(datum.get(0), datum.get(0)
					+ noise.sample(), datum.get(1)), datum.getLabel()));
		}

		CombinationChart plots = ScatterPlot.withPCA(ld3);

		RenderWithFlot render = new RenderWithFlot();
		render.renderToBrowser(plots);
	}
}
