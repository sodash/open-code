package com.winterwell.maths.matrix;

import org.junit.Test;

import com.winterwell.maths.WinterwellMaths;
import com.winterwell.maths.timeseries.DataUtils;
import com.winterwell.maths.vector.XY;
import com.winterwell.utils.MathUtils;
import com.winterwell.utils.Printer;
import com.winterwell.utils.Utils;
import com.winterwell.utils.time.StopWatch;
import com.winterwell.utils.web.XStreamUtils;

import no.uib.cipr.matrix.DenseMatrix;
import no.uib.cipr.matrix.DenseVector;
import no.uib.cipr.matrix.Matrix;
import no.uib.cipr.matrix.MatrixEntry;
import no.uib.cipr.matrix.Vector;

public class MatrixUtilsTest {

	@Test
	public void testInverse() {
		Matrix a = new DiagonalMatrix(new DenseVector(new double[]{2,4}));
		Matrix ai = MatrixUtils.invert(a);
		Matrix ai2 = MatrixUtils.pseudoInverse(a);
		DiagonalMatrix ainv = new DiagonalMatrix(new DenseVector(new double[]{1.0/2, 1.0/4}));
		assert MatrixUtils.equals(ainv, ai2) : ainv+" v "+ai2; 
		assert MatrixUtils.equals(ainv, ai) : ainv+" v "+ai; 
	}
	

	@Test
	public void testComputeEigenVectors() {
		{ // very simple
			DenseMatrix a = new DenseMatrix(2, 2);
			a.set(0, 0, 1);
			a.set(1, 1, 3);
			Eigenpair[] evs = new EVDAdapter().getEigenpairs(a).toArray(
					new Eigenpair[0]);
			assert evs[0].getValue() == 3 : Printer.toString(evs);
			assert evs[1].getValue() == 1;
			assert DataUtils.equals(evs[0].getVector(), new XY(0, 1));
			assert DataUtils.equals(evs[1].getVector(), new XY(1, 0));
		}
		{ // simple
			DenseMatrix a = new DenseMatrix(2, 2);
			a.set(0, 0, 1);
			a.set(0, 1, 1);
			a.set(1, 0, 1);
			a.set(1, 1, 1);
			Eigenpair[] evs = new EVDAdapter().getEigenpairs(a).toArray(
					new Eigenpair[0]);
			assert evs[0].getValue() == 2 : Printer.toString(evs);
			assert evs[1].getValue() == 0;
			double x = 1 / MathUtils.ROOT_TWO;
			assert DataUtils.equals(evs[0].getVector(), new XY(x, x));
			assert DataUtils.equals(evs[1].getVector(), new XY(-x, x));
		}
	}

	@Test
	public void testGetColumnVector() {
		Matrix a = new DenseMatrix(2, 3);
		for (MatrixEntry e : a) {
			e.set((1 + e.row()) * (1 + e.column()));
		}

		Vector v0 = MatrixUtils.getColumnVector(a, 0);
		Vector v1 = MatrixUtils.getColumnVector(a, 1);
		Vector v2 = MatrixUtils.getColumnVector(a, 2);

		assert DataUtils.equals(v0, new XY(1, 2));
		assert DataUtils.equals(v1, new XY(2, 4));
		assert DataUtils.equals(v2, new XY(3, 6));
	}

	@Test
	public void testXStream() {
		WinterwellMaths.init();
		// 1 0 0
		// 0 2 3
		//
		DenseMatrix m = new DenseMatrix(2, 3);
		m.set(0, 0, 1);
		m.set(1, 1, 2);
		m.set(1, 2, 3);
		String xml = XStreamUtils.serialiseToXml(m);
		DenseMatrix m2 = XStreamUtils.serialiseFromXml(xml);
		assert MatrixUtils.equals(m, m2);
	}

	/**
	 * Time special: 14 secs normal: 82 secs gzip: 14 secs
	 * 
	 * Space (with 50% non-zero entries from random) special: 10,807,161 normal:
	 * 32,339,952 gzip: 5,813,695
	 * 
	 * GZip wins
	 */
	@Test
	public void testXStreamPerformance() {
		// test performance normal and special
		Utils.getRandom().setSeed(1234);
		Matrix m = MatrixUtils.getRandomDenseMatrix(1000, 1000);
		StopWatch sw = new StopWatch();
		for (int i = 0; i < 1; i++) {
			String xml = XStreamUtils.serialiseToXml(m);
			Printer.out(xml.length());
			DenseMatrix m2 = XStreamUtils.serialiseFromXml(xml);
		}
		sw.print();
	}
}
