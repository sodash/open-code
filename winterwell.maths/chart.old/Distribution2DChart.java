package com.winterwell.maths.chart;

import java.util.List;

import com.winterwell.maths.stats.StatsUtils;
import com.winterwell.maths.stats.distributions.IDistribution;
import com.winterwell.utils.containers.Range;

import no.uib.cipr.matrix.Vector;

/**
 * a scatter-plot based view of a distribution
 * 
 * @author daniel
 * 
 */
public class Distribution2DChart extends XYChart {

	final IDistribution dist;

	public Distribution2DChart(IDistribution dist) {
		this.dist = dist;
		assert dist.getDim() == 2;
		setupAxis();
	}

	@Override
	public List<Vector> getData() {
		List<Vector> pts = StatsUtils.sample(dist, 1000);
		// set the range
		// axes[1].setRange(new Range(0, max));
		return pts;
	}

	private void setupAxis() {
		Range[] support = dist.getSupport();
		axes = new NumericalAxis[2];
		for (int i = 0; i < 2; i++) {
			NumericalAxis axis = new NumericalAxis();
			Range supp = support[i];
			// if (dist instanceof GridDistribution2D) {
			// GridDistribution2D g1d = (GridDistribution2D) dist;
			// axis.setRange(supp);
			// axis.setGrid(g1d.getGridInfo());
			// }
			if (supp.size() < Double.MAX_VALUE / 100000) { // arbitrary too big
															// threshold
				axis.setRange(supp);
			} else {
				double m = dist.getMean().get(i);
				double sd = dist.getVariance().get(i);
				sd = Math.sqrt(sd);
				axis.setRange(new Range(m - 5 * sd, m + 5 * sd));
			}
			axes[i] = axis;
		}
	}

}
