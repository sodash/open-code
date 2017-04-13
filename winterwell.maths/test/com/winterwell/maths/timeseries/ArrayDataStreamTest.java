package com.winterwell.maths.timeseries;

import java.util.List;

import org.junit.Test;

import com.winterwell.utils.containers.AbstractIterator;
import com.winterwell.utils.containers.Containers;

public class ArrayDataStreamTest {

	@Test
	public void testGetNext() {
		double[] xs = new double[] { 1, 2, 3, 4 };
		double[] ys = new double[] { 1, 2, 3, 4 };

		ArrayDataStream ads = new ArrayDataStream(xs);
		AbstractIterator<Datum> dit = ads.iterator();
		assert dit.hasNext();
		List<Datum> vecs = Containers.getList(ads);
		assert vecs.size() == 4;
		assert vecs.get(0).x() == 1;
		assert vecs.get(3).x() == 4;

		ArrayDataStream ads2 = new ArrayDataStream(xs, ys);

	}

	@Test
	public void testSize() {
		double[] xs = new double[] { 1, 2, 3, 4 };
		double[] ys = new double[] { 1, 2, 3, 4 };

		ArrayDataStream ads = new ArrayDataStream(xs);
		assert ads.size() == 4;
		assert ads.getDim() == 1;

		ArrayDataStream ads2 = new ArrayDataStream(xs, ys);
		assert ads2.size() == 4;
		assert ads2.getDim() == 2;
	}

}
