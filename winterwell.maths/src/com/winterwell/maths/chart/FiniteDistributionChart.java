/**
 * A barchart
 */
package com.winterwell.maths.chart;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.winterwell.maths.stats.distributions.discrete.IFiniteDistribution;
import com.winterwell.maths.vector.XY;
import com.winterwell.utils.containers.Containers;
import com.winterwell.utils.containers.Range;

import no.uib.cipr.matrix.Vector;

/**
 * A Histogram! Status: experimental
 * 
 * @see HistogramChart which plots density functions.
 * @author Joe Halliwell <joe@winterwell.com>
 * @testedby {@link RenderWithFlotTest} and {@link FiniteDistributionChartTest}
 */
public class FiniteDistributionChart extends XYChart {

	@Override
	public Map<Integer, String> getDataLabels() {
		if (dataLabels==null) {
			dataLabels = new HashMap<Integer, String>();
			int i=0;
			for(String label : getNominalAxis().categories) {				
				dataLabels.put(i, label);
				i++;
			}
		}
		return super.getDataLabels();
	}
	
	private final IFiniteDistribution dist;

	public FiniteDistributionChart(IFiniteDistribution distribution) {
		assert distribution != null;
		this.dist = distribution;
		this.type = ChartType.COLUMN; // Column by default
		setupAxes(); // Dodgy -- isn't distribution mutable?
	}

	@Override
	public List<Vector> getData() {
		List keys = Containers.getList(dist);
		List<Vector> data = new ArrayList(keys.size());

		double max = 0;
		for (int i = 0; i < keys.size(); i++) {
			Object k = keys.get(i);
			double prob = dist.prob(k);
			if (prob > max) {
				max = prob;
			}
			data.add(new XY(i, prob));
		}
		// leave a bit of space on the y-axis
		((NumericalAxis) axes[1]).setRange(new Range(0, max * 1.05));
		return data;
	}

	// Maybe?
	public NominalAxis getNominalAxis() {
		return (NominalAxis) axes[0];
	}

	private void setupAxes() {
		NominalAxis xAxis = new NominalAxis();
		// sorted labels
		List xLabels = Containers.getList(dist);
		Collections.sort(xLabels);
		xAxis.setCategories(xLabels);
		
		// Make the Y-Axis use %s by default
		NumericalAxis yAxis = new PercentageAxis();		
		
		this.axes = new Axis[] { xAxis, yAxis };
	}

	public boolean isEmpty() {
		return dist.getTotalWeight() == 0;
	}

}
