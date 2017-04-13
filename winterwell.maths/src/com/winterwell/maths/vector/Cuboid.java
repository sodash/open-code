package com.winterwell.maths.vector;

import java.util.Arrays;

import com.winterwell.maths.timeseries.DataUtils;
import com.winterwell.utils.containers.Pair;

import no.uib.cipr.matrix.Vector;

/**
 * Describe a hyper-cuboid by two of it's corners: The "top-left"(min) and
 * "bottom-right"(max) corners.
 * 
 * Status: experimental - it may be better just to use Pair<Vector>
 * 
 * @author daniel
 * 
 */
public class Cuboid extends Pair<Vector> {

	private static final long serialVersionUID = 1L;

	/**
	 * The vectors will be converted into min/max corners.
	 * 
	 * @param a
	 * @param b
	 */
	public Cuboid(Vector a, Vector b) {
		super(DataUtils.getBounds(Arrays.asList(a, b)));
	}

}
