package com.winterwell.maths.chart;

import java.util.ArrayList;
import java.util.List;

import com.winterwell.maths.GridInfo;
import com.winterwell.maths.IGridInfo;
import com.winterwell.maths.stats.distributions.d1.HistogramData;
import com.winterwell.maths.stats.distributions.d1.IDistribution1D;
import com.winterwell.maths.vector.XY;
import com.winterwell.utils.MathUtils;
import com.winterwell.utils.containers.Range;

import no.uib.cipr.matrix.Vector;

/**
 * Plot a graph of the probability density function.
 * 
 * @see FiniteDistributionChart which is a bar chart
 * @author daniel
 * @testedby  HistgramChartTest}
 */
public class HistogramChart extends XYChart {

	/**
	 * Build a chart from the the *first* dimension of the specified data.
	 * Ranges are detected automatically.
	 * 
	 * @param data
	 * @param numBuckets
	 * @return
	 */
	public static HistogramChart fromData(Iterable<? extends Vector> data,
			int numBuckets) {
		// Do a pass to compute range
		double min = Double.MAX_VALUE, max = Double.MIN_VALUE;
		ArrayList<Double> copy = new ArrayList();
		for (Vector datum : data) {
			double x = datum.get(0);
			copy.add(x);
			min = Math.min(min, x);
			max = Math.max(max, x);
		}
		// TODO: Check min and max are sensible?
		double[] array = MathUtils.toArray(copy);
		// TODO: Allow the num of buckets to be specified
		HistogramData dist = new HistogramData(new GridInfo(min, max,
				numBuckets));
		for (int i = 0; i < array.length; i++) {
			dist.count(array[i]);
		}
		return new HistogramChart(dist);
	}

	private final IDistribution1D dist;

	/**
	 * 
	 * @param dist E.g. HistogramData
	 */
	public HistogramChart(IDistribution1D dist) {
		this.dist = dist;
		// line chart by default
		setType(ChartType.LINE);
		setupAxes();
	}

	@Override
	public List<Vector> getData() {
		// already set?
		if (this.series!=null && ! this.series.getData().isEmpty()) {
			return this.series.getData();
		}		
		// take readings of the density
		NumericalAxis axis = getAxis(X);
		if (axis==null) {
			setData3_setupAxes();
			axis = getAxis(X);
			assert axis != null;
		}
		IGridInfo grid = axis.getGrid();
		assert grid != null : axis+" "+this;
		double[] xs = grid.getMidPoints();		

		List<Vector> data = new ArrayList<Vector>(xs.length);
		
		double max = 0;
		for (double x : xs) {
			double y = dist.density(x);
			data.add(new XY(x, y));
			if (y > max) {
				max = y;
			}
		}
		// set the range, if unset
//		setData3_setupAxes();
		
		this.setData(data);
		
		return data;
	}
	
	@Override
	public void setAxis(int i, Axis axis) {
		super.setAxis(i, axis);
		setData3_setupAxes();
	}

	private void setupAxes() {
		// x
		NumericalAxis axis = new NumericalAxis();
		if (dist instanceof HistogramData) {
			// use the grid we're given
			HistogramData g1d = (HistogramData) dist;
			axis.setRange(g1d.getSupport());
			axis.setGrid(g1d.getGridInfo());
		} else {
			// pick a grid
			Range support = dist.getSupport();
			if (support.size() < Double.MAX_VALUE / 100000) { // arbitrary too
																// big threshold
				axis.setRange(support);
			} else {
				double m = dist.getMean();
				double sd = dist.getStdDev();
				axis.setRange(new Range(m - 5 * sd, m + 5 * sd));
			}
		}
		// y
		NumericalAxis yaxis = new NumericalAxis();
		yaxis.setTitle("density");
		// done
		axes = new Axis[] { axis, yaxis };
	}

}
