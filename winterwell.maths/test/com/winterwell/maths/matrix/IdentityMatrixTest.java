package com.winterwell.maths.matrix;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.winterwell.maths.timeseries.DataUtils;
import com.winterwell.maths.vector.XY;
import com.winterwell.utils.Utils;

import no.uib.cipr.matrix.MatrixEntry;
import no.uib.cipr.matrix.Vector;

public class IdentityMatrixTest {

	@Test
	public void testGet() {
		IdentityMatrix id = new IdentityMatrix(2);
		System.out.println(id);
		assert id.get(0, 0) == 1;
		assert id.get(1, 0) == 0;
		assert id.get(0, 1) == 0;
		assert id.get(1, 1) == 1;
	}
	
	@Test
	public void testApply() {
		IdentityMatrix id = new IdentityMatrix(2);
		XY x = new XY(-4,5);
		Vector y = MatrixUtils.apply(id, x);
		assert DataUtils.equals(x, y);
	}

	@Test
	public void testIterator() {
		IdentityMatrix id = new IdentityMatrix(2);
		assert id.isSquare();
		List<MatrixEntry> list = new ArrayList();
		for (MatrixEntry me : id) {
			list.add(Utils.copy(me));
		}
		assert list.size() == 2;
		assert list.get(0).column()==0;
		assert list.get(0).row()==0;
		assert list.get(0).get()==1;
		assert list.get(1).column()==1;
		assert list.get(1).row()==1;
		assert list.get(1).get()==1;
	}

}
