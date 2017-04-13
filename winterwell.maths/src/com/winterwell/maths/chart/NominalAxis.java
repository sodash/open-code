/**
 *
 */
package com.winterwell.maths.chart;

import com.winterwell.maths.GridInfo;
import com.winterwell.maths.IGridInfo;
import com.winterwell.utils.TodoException;
import com.winterwell.utils.containers.Range;

/**
 * An axis for nominal/categorical data. Maintains a mapping between objects and
 * labels.
 * 
 * @author Joe Halliwell <joe@winterwell.com>
 * 
 */
public class NominalAxis extends NumericalAxis {

	public NominalAxis() {
		super();
	}

	@Override
	public IGridInfo getGrid() {
		return new GridInfo(0, categories.size(), categories.size());
	}

	public int getIndex(String label) {
		// Is this really required?
		throw new TodoException();
	}

	@Override
	public Range getRange() {
		return new Range(0, categories.size());
	}
}
