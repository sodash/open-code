package com.winterwell.maths.chart;

import java.util.List;

import org.junit.Test;

import com.winterwell.maths.stats.distributions.discrete.IntegerDistribution;
import com.winterwell.maths.vector.XY;
import com.winterwell.utils.containers.IntRange;

import no.uib.cipr.matrix.Vector;

public class FiniteDistributionChartTest {

	@Test
	public void testGetData() {
		IntegerDistribution timeOfDay = new IntegerDistribution();
		timeOfDay.setProb(14, 0.5);
		timeOfDay.setProb(12, 0.5);
		timeOfDay.setRange(new IntRange(0, 23));

		assert timeOfDay.prob(14) == 0.5;
		assert timeOfDay.prob(14) == 0.5;

		FiniteDistributionChart chart = new FiniteDistributionChart(timeOfDay);
		List<Vector> data = chart.getData();
		System.out.println(data);
		assert ! data.isEmpty();

		NominalAxis axis = (NominalAxis) chart.getAxis(AChart.X);
		IntRange range = timeOfDay.getRange();
		System.out.println(range);
		axis.setCategories(range);
		chart.setTitle("Time of Day");

		List<Vector> data2 = chart.getData();
		assert data.contains(new XY(12, 0.5)) : data;
	}

}
