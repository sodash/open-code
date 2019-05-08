package com.winterwell.maths.timeseries;

import java.util.List;

import org.junit.Test;

import com.winterwell.maths.chart.ScatterPlot;
import com.winterwell.maths.stats.distributions.AxisAlignedGaussian;
import com.winterwell.maths.vector.XY;
import com.winterwell.utils.time.TUnit;
import com.winterwell.utils.time.Time;

public class RandomDataStreamTest {

	@Test
	public void testSample() {
		AxisAlignedGaussian g = new AxisAlignedGaussian(new XY(0, 0), new XY(1,
				2));
		RandomDataStream stream = new RandomDataStream(g, new Time(),
				TUnit.HOUR.dt);
		List<Datum> data = stream.sample(10);

		ScatterPlot plot = new ScatterPlot(data);
//		new RenderWithFlot().renderToBrowser(plot);
	}

}
