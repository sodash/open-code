package com.winterwell.maths.timeseries;

import java.util.ArrayList;

import org.junit.Test;

import com.winterwell.maths.chart.CombinationChart;
import com.winterwell.maths.chart.TimeSeriesChart;
import com.winterwell.utils.time.TUnit;

public class DetrendedStreamTest {

	@Test
	public void testSimple() {
		FunctionDataStream f = new FunctionDataStream(TUnit.HOUR.dt) {
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
			protected double f(double x) {
				return 2 * x - 100;
			}
		};
		ArrayList<Datum> data = DataUtils.toList(f, 1000);
		DetrendedStream ds = new DetrendedStream(new ListDataStream(data));
		TimeSeriesChart chart1 = new TimeSeriesChart();
		chart1.setData(ds);
		TimeSeriesChart chart2 = new TimeSeriesChart();
		chart2.setData(new ListDataStream(data));
		CombinationChart cc = new CombinationChart(chart1, chart2);
//		RenderWithFlot render = new RenderWithFlot();
//		String html = render.renderToHtml(cc);
//		WebUtils.display("<html><body>" + RenderWithFlot.JQUERY_DEPENDENCY
//				+ RenderWithFlot.DEPENDENCIES + html + "</body></html>");
		assert true;
	}

}
