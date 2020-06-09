package com.winterwell.maths.chart;

import java.util.Collections;
import java.util.List;

import com.winterwell.maths.GridInfo;
import com.winterwell.maths.IGridInfo;
import com.winterwell.utils.containers.Pair2;
import com.winterwell.utils.containers.Range;

/**
 * 
 * Sub classes: - Time axis - nominal data axis
 * 
 * TODO log axis
 * 
 * @author daniel
 * 
 */
public class NumericalAxis extends Axis {

	enum KDirn {
		/** going into the page, e.g. Z. Could also be a heat axis? */
		DEPTH,
		/** e.g. X */
		HORIZONTAL,
		/** e.g. Y */
		VERTICAL
	}

	private IGridInfo grid;


	Object ticks;

	public NumericalAxis() {
		//
	}

	public NumericalAxis(double low, double high) {
		setRange(new Range(low, high));
	}

	/**
	 * TODO what is this used for? Should it control tick marks?
	 * 
	 * @return
	 */
	public IGridInfo getGrid() {
		return grid;
	}

	/**
	 * @return Can be null
	 */
	public Range getRange() {
		return range;
	}

	// TODO how to express "every 10 units, centred on zero"? ie. a periodic
	// boolean function
	// List<Pair2<Double,String>> getMarkers

	// TODO use-case
	List<Pair2<Double, String>> getSpecialMarkers() {
		return Collections.emptyList();
	}

	/**
	 * The tick-marks to draw.
	 * 
	 * Experimental: links up with Flot's tick option, which can take a number,
	 * or a list of tick-marks.
	 * 
	 * @return a number, or a list of specific values, or null (default)
	 */
	public Object getTicks() {
		// ?? should this be linked with IGridInfo?
		return ticks;
	}

	public NumericalAxis setGrid(IGridInfo gridInfo) {
		this.grid = gridInfo;
		return this;
	}

	/**
	 * ??this also sets {@link #grid} - is this wise/good??
	 * 
	 * @param range
	 */
	public void setRange(Range range) {
		this.range = range;
		grid = new GridInfo(range.low, range.high, 250);
	}

	public void setTicks(int numberOfTicks) {
		this.ticks = numberOfTicks;
	}

	public void setTicks(List<? extends Number> specificTicks) {
		this.ticks = specificTicks;
	}

	@Override
	public String toString() {
		return "NumericalAxis" + range;
	}

}
