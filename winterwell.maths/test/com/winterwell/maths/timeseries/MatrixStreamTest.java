package com.winterwell.maths.timeseries;

import org.junit.Test;

import com.winterwell.maths.matrix.ProjectionMatrix;
import com.winterwell.maths.vector.XY;

public class MatrixStreamTest {

	@Test
	public void testSimple() {
		// drop y matrix
		ProjectionMatrix m = new ProjectionMatrix(3, 0, 2);
		ListDataStream list = new ListDataStream(3);
		list.add(new Datum(new double[] { 5, 6, 7 }));
		MatrixStream stream = new MatrixStream(m, list);
		ListDataStream outs = stream.list();
		XY out = new XY(5, 7);
		assert DataUtils.equals(outs.get(0), out);
	}

}
