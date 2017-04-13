package com.winterwell.maths.chart;

import java.io.IOException;
import java.util.Arrays;

import com.winterwell.maths.timeseries.Datum;
import com.winterwell.maths.timeseries.ListDataStream;
import com.winterwell.utils.time.Time;

public class LinkedChartDemo {
	public static void main(String[] args) throws IOException {
		LinkedChart linkedChart = new LinkedChart();
		
		ListDataStream abcs = new ListDataStream(3);
		abcs.add(new Datum(new Time(2000, 1, 1), new double[] { 1, 2, 3 }, null));
		abcs.add(new Datum(new Time(2000, 1, 2), new double[] { 2, 3, 4 }, null));
		abcs.add(new Datum(new Time(2000, 1, 3), new double[] { 3, 4, 5 }, null));
		CombinationChart chart1 = TimeSeriesChart.newMultiValuedChart(abcs,
				Arrays.asList("A", "B", "C"), true);
		
		chart1.setTitle("First chart");
		
		CombinationChart chart2 = TimeSeriesChart.newMultiValuedChart(abcs,
				Arrays.asList("A", "B", "C"), true);
		
		chart2.setTitle("Second chart");
		
		linkedChart.addChart(chart1);
		linkedChart.addChart(chart2);

		Renderer.popup(linkedChart);
	}
	
}
