package com.winterwell.maths.matrix;

import org.junit.Test;

import com.winterwell.maths.timeseries.DataUtils;
import com.winterwell.maths.vector.XY;
import com.winterwell.maths.vector.XYZ;

public class ProjectionMatrixTest {

	@Test
	public void testSimple() {
		// drop y
		ProjectionMatrix m = new ProjectionMatrix(3, 0, 2);
		XY out = new XY(0, 0);
		m.mult(new XYZ(5, 6, 7), out);
		assert DataUtils.equals(out, 5, 7);
	}

}
