package com.winterwell.maths.matrix;

import com.winterwell.utils.MathUtils;
import com.winterwell.utils.Printer;
import com.winterwell.utils.containers.Containers;

import junit.framework.TestCase;
import no.uib.cipr.matrix.DenseCholesky;
import no.uib.cipr.matrix.DenseMatrix;
import no.uib.cipr.matrix.DenseVector;
import no.uib.cipr.matrix.Matrices;
import no.uib.cipr.matrix.Matrix;
import no.uib.cipr.matrix.NotConvergedException;
import no.uib.cipr.matrix.SymmDenseEVD;
import no.uib.cipr.matrix.UpperTriangDenseMatrix;
import no.uib.cipr.matrix.Vector;

/**
 * Some basic tests that we can muck around with vectors and matrices.
 * 
 * @author daniel
 * 
 */
public class TestSimpleMatrixStuff extends TestCase {

	public void testEigenVectors() throws NotConvergedException {
		DenseMatrix m = new DenseMatrix(2, 2);
		m.set(0, 0, 2);
		m.set(1, 1, 3);
		// SymmDenseEVD eigen = new SymmDenseEVD(2, true);
		SymmDenseEVD e = SymmDenseEVD.factorize(m);
		DenseMatrix vecs = e.getEigenvectors();
		Printer.out(vecs);
		double[] vals = e.getEigenvalues();
		assert Containers.indexOf(2.0, vals) != -1 : Printer.toString(vals);
		assert Containers.asList(vals).indexOf(3.0) != -1 : Printer
				.toString(vals);
	}

	public void testInvert() {
		DenseMatrix m = new DenseMatrix(2, 2);
		double a = 1;
		double b = 2;
		double c = 3;
		double d = 4;
		m.set(0, 0, a);
		m.set(0, 1, b);
		m.set(1, 0, c);
		m.set(1, 1, d);
		Matrix inv = MatrixUtils.invert(m);
		double det = (a * d - b * c);
		assert MathUtils.equalish(inv.get(0, 0), d / det);
		assert MathUtils.equalish(inv.get(0, 1), -b / det);
		assert MathUtils.equalish(inv.get(1, 0), -c / det);
		assert MathUtils.equalish(inv.get(1, 1), a / det);
	}

	public void testMatrixApplication() {
		DenseMatrix m = new DenseMatrix(2, 2);
		m.set(0, 1, 1);
		m.set(1, 0, 1);
		Vector x = new DenseVector(new double[] { 1, 2 });
		Vector y = new DenseVector(2);
		m.mult(2, x, y);
		assert y.get(0) == 4 : y;
		assert y.get(1) == 2 : y;
	}

	// Put MTJ through its paces to test I understand the API - Joe
	public void testSPDStuff() {
		DenseMatrix m = new DenseMatrix(2, 2);
		double a = 1;
		double b = 1;
		double c = 1;
		double d = 2;
		m.set(0, 0, a);
		m.set(0, 1, b);
		m.set(1, 0, c);
		m.set(1, 1, d);
		DenseCholesky dc = DenseCholesky.factorize(m);
		UpperTriangDenseMatrix u = dc.getU();

		// Find determinant
		double det = MathUtils.sq(u.get(0, 0) * u.get(1, 1));
		assert MathUtils.equalish(det, (a * d) - (b * c));

		// Testing inverting using decomp
		DenseMatrix inv = Matrices.identity(2);
		dc.solve(inv);
		assert MathUtils.equalish(inv.get(0, 0), d / det);
		assert MathUtils.equalish(inv.get(0, 1), -b / det);
		assert MathUtils.equalish(inv.get(1, 0), -c / det);
		assert MathUtils.equalish(inv.get(1, 1), a / det);
	}
}
