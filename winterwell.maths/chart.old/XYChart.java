package com.winterwell.maths.chart;

import java.util.List;
import java.util.Random;

import com.winterwell.maths.stats.distributions.d1.Gaussian1D;
import com.winterwell.utils.containers.Range;

import no.uib.cipr.matrix.Vector;

/**
 * This covers a range of standard charts e.g. scatter plots, histograms,
 * barcharts.
 * 
 * TODO handle distributions: confidence intervals & min/max
 * @testedby  XYChartTest}
 * @author daniel
 */
public class XYChart extends Chart {

	
	private double jitter;

	public XYChart() {
	}

	public XYChart(String name) {
		super(name);
	}

	public XYChart(String name, Iterable<? extends Vector> data) {
		super(name, data);
	}

	// FIXME: Only applies to numerical data...
	private void applyJitter() {
		// use a predictable Random source so we can get identical pictures
		// (which is useful for testing)
		Random random = new Random(12345);
		Gaussian1D[] g = new Gaussian1D[axes.length];
		for (int ai = 0; ai < axes.length; ai++) {
			Range range = getAxis(ai).getRange();
			double sd = range.size() * jitter;
			double var = sd * sd;
			g[ai] = new Gaussian1D(0, var);
			g[ai].setRandomSource(random);
		}
		
		List<Vector> data = this.getData();
		
		for (int vi = 0; vi < data.size(); vi++) {
			Vector v = data.get(vi);
			Vector v2 = v.copy();
			for (int i = 0; i < v2.size(); i++) {
				double noise = g[i].sample();
				v2.add(i, noise);
			}
			data.set(vi, v2);
		}
		
		this.setData(data);
	}

	@Override
	public NumericalAxis getAxis(int index) {
		return (NumericalAxis) super.getAxis(index);
	}

	@Override
	public void setData(Iterable<? extends Vector> points) {
		super.setData(points);
		// 2D
		assert getData().get(0).size() == 2 : points;
		if (jitter != 0) {
			applyJitter();
		}
	}

	/**
	 * @param jitter
	 *            The amount of jitter to apply, as a ratio of the coordinate
	 *            range. So a jitter of 0.1 would use N(0, 0.1*axis-length) to
	 *            generate jitter.
	 */
	public void setJitter(double jitter) {
		this.jitter = jitter;
		if (this.getData() != null && jitter != 0) {
			applyJitter();
		}
	}

	public NumericalAxis xAxis() {
		return (NumericalAxis) axes[0];
	}

	public NumericalAxis yAxis() {
		return (NumericalAxis) axes[1];
	}
}
