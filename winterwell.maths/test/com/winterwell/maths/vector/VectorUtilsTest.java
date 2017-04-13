package com.winterwell.maths.vector;

import java.util.Arrays;
import java.util.List;

import com.winterwell.maths.timeseries.DataUtils;
import com.winterwell.utils.MathUtils;

import junit.framework.Assert;
import junit.framework.TestCase;
import no.uib.cipr.matrix.DenseVector;
import no.uib.cipr.matrix.Matrices;
import no.uib.cipr.matrix.Vector;
import no.uib.cipr.matrix.Vector.Norm;
import no.uib.cipr.matrix.sparse.SparseVector;

public class VectorUtilsTest extends TestCase {

	public static Double EPS = MathUtils.getMachineEpsilon();

	public void testAngle() {
		{
			XY a = new XY(0, 3);
			XY b = new XY(4, 0);
			assert DataUtils.angle(a, a) == 0.0;
			assert DataUtils.angle(b, b) == 0.0;
			double theta = DataUtils.angle(a, b);
			assert theta == Math.PI / 2;
		}
		{
			XY a = new XY(0, 2);
			XY b = new XY(4, 4);
			double bb = DataUtils.angle(b, b);
			assert Math.abs(bb) < 0.01 : bb;
			double theta = DataUtils.angle(a, b);
			assert Math.abs(theta - Math.PI / 4) < 0.01 : theta;
		}
	}

	public void testAppend() {
		X x = new X(-7);
		XYZ y = new XYZ(4, 3, 2);
		Vector xy = DataUtils.append(x, y);
		assert DataUtils.equals(xy, -7, 4, 3, 2);
	}

	public void testDist() {
		XY a = new XY(0, 3);
		XY b = new XY(4, 0);
		XY c = new XY(4, 4);
		assert DataUtils.dist(a, a) == 0;
		assert DataUtils.dist(b, b) == 0;
		assert DataUtils.dist(c, c) == 0;
		assert DataUtils.dist(a, b) == 5;
		assert DataUtils.dist(b, a) == 5;
		assert DataUtils.dist(b, c) == 4;
	}

	public void testDistance() {
		{
			int dim = 10;
			Vector a = new DenseVector(dim);
			Vector b = new DenseVector(dim);
			Matrices.random(a);
			Vector oa = a.copy();
			Vector ob = b.copy();
			Assert.assertEquals(oa.norm(Norm.Two), DataUtils.dist(a, b));
			// check no edit of vector
			assert DataUtils.equals(a, oa);
			assert DataUtils.equals(b, ob);
		}
		{
			XYZ a = new XYZ(1, 0, 3);
			XYZ b = new XYZ(1, 4, 0);
			XYZ oa = a.copy();
			XYZ ob = b.copy();
			assert DataUtils.dist(a, b) == 5;
			// check no edit of vector
			assert DataUtils.equals(a, oa);
			assert DataUtils.equals(b, ob);
		}
	}

	public void testElementWiseDivide() {
		double[] _a = { 1.0, 4.0, 9.0e100 };
		double[] _b = { -5.0, 8.0e10, 0.25 };
		double[] _expect = { -0.2, 5.0e-11, 3.6e101 };
		Vector a = new DenseVector(_a);
		Vector b = new DenseVector(_b);
		Vector expect = new DenseVector(_expect);
		Assert.assertTrue(DataUtils.equalish(expect,
				DataUtils.elementWiseDivide(a, b), EPS));
	}

	public void testElementWiseFunctionsThrow() {
		double[] _a = { 1, 4, 8 };
		double[] _b = { 0, -1200, 8e23, Double.NEGATIVE_INFINITY };
		Vector a = new DenseVector(_a);
		Vector b = new DenseVector(_b);
		{
			Boolean threw = false;
			try {
				DataUtils.elementWiseMin(a, b);
			} catch (IllegalArgumentException e) {
				threw = true;
			}
			Assert.assertTrue(threw);
		}
		{
			Boolean threw = false;
			try {
				DataUtils.elementWiseMax(a, b);
			} catch (IllegalArgumentException e) {
				threw = true;
			}
			Assert.assertTrue(threw);
		}
		{
			Boolean threw = false;
			try {
				DataUtils.elementWiseMultiply(a, b);
			} catch (IllegalArgumentException e) {
				threw = true;
			}
			Assert.assertTrue(threw);
		}
		{
			Boolean threw = false;
			try {
				DataUtils.elementWiseDivide(a, b);
			} catch (IllegalArgumentException e) {
				threw = true;
			}
			Assert.assertTrue(threw);
		}
	}

	public void testElementWiseMax1() {
		double[] _a = { 1, 4, 8, 9e100 };
		double[] _b = { -5, 8e23, Double.NEGATIVE_INFINITY,
				Double.POSITIVE_INFINITY };
		double[] _expect = { 1, 8e23, 8 };
		Vector a = new DenseVector(_a);
		Vector b = new DenseVector(_b);
		Vector expect = new DenseVector(_expect);
		// bit of fiddling, we don't expect infinity to equal infinity
		Vector result = DataUtils.elementWiseMax(a, b);
		Assert.assertTrue(!MathUtils.isFinite(result.get(3))
				&& result.get(3) > 0);
	}

	public void testElementWiseMax2() {
		double[] _a = { 1.0, 4.0, 8.0 };
		double[] _b = { -5.0, 8.0e23, Double.NEGATIVE_INFINITY };
		double[] _expect = { 1.0, 8e23, 8.0 };
		Vector a = new DenseVector(_a);
		Vector b = new DenseVector(_b);
		Vector expect = new DenseVector(_expect);
		Assert.assertTrue(DataUtils.equalish(expect,
				DataUtils.elementWiseMax(a, b), EPS));
	}

	public void testElementWiseMin1() {
		double[] _a = { 1.0, 4.0, 8.0, 9e100 };
		double[] _b = { -5.0, 8.0e23, Double.NEGATIVE_INFINITY,
				Double.POSITIVE_INFINITY };
		Vector a = new DenseVector(_a);
		Vector b = new DenseVector(_b);
		// bit of fiddling, we don't expect infinity to equal infinity
		Vector result = DataUtils.elementWiseMin(a, b);
		Assert.assertTrue(!MathUtils.isFinite(result.get(2))
				&& result.get(2) < 0);
	}

	public void testElementWiseMin2() {
		double[] _a = { 1.0, 4.0, 9.0e100 };
		double[] _b = { -5.0, 8.0e23, Double.POSITIVE_INFINITY };
		double[] _expect = { -5.0, 4.0, 9.0e100 };
		Vector a = new DenseVector(_a);
		Vector b = new DenseVector(_b);
		Vector expect = new DenseVector(_expect);
		Assert.assertTrue(DataUtils.equalish(expect,
				DataUtils.elementWiseMin(a, b), EPS));
	}

	public void testElementWiseMultiply() {
		double[] _a = { 1.0, 4.0, 9.0e100 };
		double[] _b = { -5.0, 8e23, 0.25 };
		double[] _expect = { -5.0, 3.2e24, 2.25e100 };
		Vector a = new DenseVector(_a);
		Vector b = new DenseVector(_b);
		Vector expect = new DenseVector(_expect);
		Assert.assertTrue(DataUtils.equalish(expect,
				DataUtils.elementWiseMultiply(a, b), EPS));
	}

	public void testEqualish() {
		assert DataUtils.equalish(new XY(1, 1), new XY(1, 1));
		assert DataUtils.equalish(new XY(1.005, 1), new XY(1.005, 1));
		assert DataUtils.equalish(new XY(1001, 1000), new XY(1000, 1000));

		assert !DataUtils.equalish(new XY(1, 1), new XY(1, 1.1));
		assert !DataUtils.equalish(new XY(1.1, 1), new XY(1, 1));

		// sparse vectors
		assert DataUtils.equalish(new SparseVector(new XY(1, 1)),
				new SparseVector(new XY(1, 1)));
		assert DataUtils.equalish(new SparseVector(new XY(1.005, 1)),
				new SparseVector(new XY(1.005, 1)));

		assert !DataUtils.equalish(new SparseVector(new XY(1, 1)),
				new SparseVector(new XY(1, 1.1)));
		assert !DataUtils.equalish(new SparseVector(new XY(1.1, 1)),
				new SparseVector(new XY(1, 1)));
	}

	public void testEqualish_withTolerance() {
		assert DataUtils.equalish(new XY(1, 1), new XY(1, 1), 0.01);
		assert DataUtils.equalish(new XY(1.005, 1), new XY(1.005, 1), 0.01);
		assert DataUtils.equalish(new XY(1.5, 1), new XY(1, 1.5), 1);

		assert !DataUtils.equalish(new XY(1, 1), new XY(1, 1.1), 0.01);
		assert !DataUtils.equalish(new XY(1.1, 1), new XY(1, 1), 0.01);

		// sparse vectors
		assert DataUtils.equalish(new SparseVector(new XY(1, 1)),
				new SparseVector(new XY(1, 1)), 0.01);
		assert DataUtils.equalish(new SparseVector(new XY(1.005, 1)),
				new SparseVector(new XY(1.005, 1)), 0.01);
		assert DataUtils.equalish(new SparseVector(new XY(1.5, 1)),
				new SparseVector(new XY(1, 1.5)), 1);

		assert !DataUtils.equalish(new SparseVector(new XY(1, 1)),
				new SparseVector(new XY(1, 1.1)), 0.01);
		assert !DataUtils.equalish(new SparseVector(new XY(1.1, 1)),
				new SparseVector(new XY(1, 1)), 0.01);
	}

	public void testGetSortedIndices() {
		{
			// 0 1 2 3 4 5
			Vector v = DataUtils.newVector(new double[] { 0, 5, 2, 3, 4, 1 });
			List<Integer> is = DataUtils.getSortedIndices(v, 4);
			assert is.equals(Arrays.asList(1, 4, 3, 2)) : is;
		}
		{
			// 0 1 2 3 4 5
			Vector v = DataUtils
					.newVector(new double[] { 0, -5, 2, 3, -4, 1 });
			DataUtils.abs(v);
			List<Integer> is = DataUtils.getSortedIndices(v, 4);
			assert is.equals(Arrays.asList(1, 4, 3, 2)) : is;
		}
		{
			// 0 1 2 3 4 5
			Vector v = DataUtils
					.newVector(new double[] { 0, -5, 2, 3, -4, 1 });
			List<Integer> is = DataUtils.getSortedIndices(v, 4);
			assert is.equals(Arrays.asList(3, 2, 5, 0)) : is;
		}
		{
			Vector v = new SparseVector(2147483647);
			v.set(2, 2);
			v.set(3, 1);
			v.set(4, 1);
			v.set(5, 1);
			v.set(6, 1);
			v.set(7, 1);
			v.set(8, 1);
			String vs = v.toString();
			List<Integer> is = DataUtils.getSortedIndices(v, 10);
			assert is.size() == 7 : is;
			assert is.get(0) == 2;
			assert is.get(1) == 3;
		}
	}

	public void testNormalize() {
		Vector v = new DenseVector(10);
		Matrices.random(v);
		v.scale(100);
		DataUtils.normalise(v);
		Assert.assertEquals(1.0, v.norm(Norm.Two), 0.0000001);
	}

	public void testProjectOrthogonal() {
		{
			Vector n = new XY(0, 1);
			Vector v = new XY(2, 3);
			Vector vp = DataUtils.projectOrthogonal(v, n);
			assert vp.size() == 2;
			assert DataUtils.equals(vp, new XY(2, 0)) : vp;
			double a = vp.dot(n);
			assert a == 0;
		}
		{
			Vector n = new XY(0, 2);
			DataUtils.normalise(n);
			Vector v = new XY(7, 7);
			Vector vp = DataUtils.projectOrthogonal(v, n);
			assert vp.size() == 2;
			assert DataUtils.equals(vp, new XY(7, 0)) : vp;
			double a = vp.dot(n);
			assert a == 0;
		}
		{
			Vector v = new XY(0, 5);
			Vector v2 = new XY(1, 1);
			DataUtils.normalise(v2);
			assert DataUtils.equals(new XY(1 / MathUtils.ROOT_TWO,
					1 / MathUtils.ROOT_TWO), v2) : v2;
			Vector vp = DataUtils.projectOrthogonal(v, v2);
			Vector vp2 = DataUtils.projectOnto(v, v2);
			Vector vrecon = vp.copy().add(vp2);
			assert DataUtils.equalish(v, vrecon);
			assert vp.size() == 2;
			assert MathUtils.equalish(vp.get(0), -vp.get(1)) : vp;
			assert MathUtils.equalish(vp.get(0), -2.5) : vp.get(0) + 5 / 2;
			double a = vp.dot(v2);
			assert MathUtils.equalish(a, 0);
		}
		{
			Vector v = new DenseVector(10);
			Vector v2 = new DenseVector(10);
			Matrices.random(v);
			Matrices.random(v2);
			DataUtils.normalise(v2);

			Vector vp = DataUtils.projectOrthogonal(v, v2);
			Vector vp2 = DataUtils.projectOnto(v, v2);

			Vector vrecon = vp.copy().add(vp2);
			double a = vp.dot(v2);
			assert DataUtils.equalish(v, vrecon);
			assert MathUtils.equalish(a, 0);
		}
	}
}
