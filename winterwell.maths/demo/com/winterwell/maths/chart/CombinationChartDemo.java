package com.winterwell.maths.chart;

import java.io.IOException;
import java.util.Arrays;

import com.winterwell.maths.timeseries.Datum;
import com.winterwell.maths.timeseries.ListDataStream;
import com.winterwell.utils.time.Time;

public class CombinationChartDemo {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws IOException {
		ListDataStream abcs = new ListDataStream(3);
		abcs.add(new Datum(new Time(2000, 1, 1), new double[] { 1, 2, 3 }, null));
		abcs.add(new Datum(new Time(2000, 1, 2), new double[] { 2, 3, 4 }, null));
		abcs.add(new Datum(new Time(2000, 1, 3), new double[] { 3, 4, 5 }, null));
		CombinationChart chart = TimeSeriesChart.newMultiValuedChart(abcs,
				Arrays.asList("A", "B", "C"), true);
		chart.setTitle("Test ABC");
		
		Renderer.popup(chart);
	}
}
