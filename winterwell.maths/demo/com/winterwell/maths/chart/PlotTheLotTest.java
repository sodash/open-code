package com.winterwell.maths.chart;

import java.io.IOException;
import java.util.Arrays;

import org.junit.Test;

import com.winterwell.maths.stats.distributions.GaussianBall;
import com.winterwell.maths.timeseries.DataUtils;
import com.winterwell.maths.timeseries.ListDataStream;
import com.winterwell.maths.vector.XY;
import com.winterwell.maths.vector.XYZ;

public class PlotTheLotTest {

	@Test
	public void testMultiColor() throws IOException {
		ListDataStream ld = new ListDataStream(2);
		GaussianBall as = new GaussianBall(new XY(0, 0), 1);
		ld.addAll(DataUtils.sample(as, 100, "A"));
		GaussianBall bs = new GaussianBall(new XY(1, 0), 1);
		ld.addAll(DataUtils.sample(bs, 100, "B"));
		GaussianBall cs = new GaussianBall(new XY(1, 1), 1);
		ld.addAll(DataUtils.sample(cs, 100, "C"));

		ld.setLabels(Arrays.asList("Alice", "Bob"));
		
		PlotTheLot ptl = new PlotTheLot(ld);
		ptl.run();		
	}

	@Test
	public void testMultiPlot() throws IOException {		
		ListDataStream ld = new ListDataStream(3);
		GaussianBall as = new GaussianBall(new XYZ(0, 0, 0), 1);
		ld.addAll(DataUtils.sample(as, 100, "A"));
		GaussianBall bs = new GaussianBall(new XYZ(0, 0, 2), 1);
		ld.addAll(DataUtils.sample(bs, 100, "B"));
		ld.setLabels(Arrays.asList("Alice", "Bob", "Carol"));
		
		PlotTheLot ptl = new PlotTheLot(ld);
		ptl.run();		
	}

}
