/**
 * 
 */
package com.winterwell.maths.chart;

import java.util.ArrayList;
import java.util.List;

import com.winterwell.maths.gui.IFunction;
import com.winterwell.maths.vector.XY;
import com.winterwell.utils.containers.Range;

import no.uib.cipr.matrix.Vector;

/**
 * plot a function
 * 
 * @author Daniel
 * 
 */
public class FunctionChart extends XYChart {

	private IFunction fn;

	public FunctionChart(IFunction fn, Range xRange) {
		this.fn = fn;
		setTitle(fn.toString());
		setAxis(0, new NumericalAxis(xRange.low, xRange.high));
	}

	@Override
	public List<Vector> getData() {
		List<Vector> data = new ArrayList<Vector>();
		
		Range xAxis = getAxis(0).getRange();
		double dx = xAxis.size() / 1000;
		for (double x = xAxis.low; x < xAxis.high; x += dx) {
			XY xy = new XY(x, fn.f(x));
			data.add(xy);
		}
		
		this.setData(data);
		
		return data;
	}
}
