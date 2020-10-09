package com.winterwell.maths.matrix;

import java.util.Iterator;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

import com.winterwell.maths.timeseries.DataUtils;
import com.winterwell.utils.FailureException;
import com.winterwell.utils.MathUtils;
import com.winterwell.utils.Utils;
import com.winterwell.utils.containers.Range;

import no.uib.cipr.matrix.AbstractMatrix;
import no.uib.cipr.matrix.DenseMatrix;
import no.uib.cipr.matrix.DenseVector;
import no.uib.cipr.matrix.Matrix;
import no.uib.cipr.matrix.Matrix.Norm;
import no.uib.cipr.matrix.MatrixEntry;
import no.uib.cipr.matrix.MatrixSingularException;
import no.uib.cipr.matrix.NotConvergedException;
import no.uib.cipr.matrix.SVD;
import no.uib.cipr.matrix.Vector;

/**
 * Miscellaneous static functions
 * 
 * @testedby  MatrixUtilsTest}
 */
public class MatrixUtils {

	public static Matrix asMatrix(double x) {
		DenseMatrix m = new DenseMatrix(1, 1);
		m.set(0, 0, x);
		return m;
	}

	public static boolean equals(Matrix a, Matrix b) {
		// check dimensions
		if (a.numColumns() != b.numColumns())
			return false;
		if (a.numRows() != b.numRows())
			return false;
		// one pass for dense matrices
		if (a instanceof DenseMatrix || b instanceof DenseMatrix) {
			for (int r = 0; r < a.numRows(); r++) {
				for (int c = 0; c < a.numColumns(); c++) {
					if (a.get(r, c) != b.get(r, c))
						return false;
				}
			}
			return true;
		}
		// All in A
		for (MatrixEntry e : a) {
			if (b.get(e.row(), e.column()) != e.get()) {
				return false;
			}
		}
		// All in B (which might be different from A)
		for (MatrixEntry e : b) {
			if (a.get(e.row(), e.column()) != e.get()) {
				return false;
			}
		}
		return true;
	}

	

	public static boolean equalish(Matrix a, Matrix b) {
		// check dimensions
		if (a.numColumns() != b.numColumns())
			return false;
		if (a.numRows() != b.numRows())
			return false;
		// don't do cell-wise comparison -- as some near-zero cells may differ by what is a relatively large amount,
		// but small compared to the rest of the matrices.
		Matrix diff = new DenseMatrix(a).add(-1, b);
		double diffSize = diff.norm(Norm.One);
		// some scale
		double ta = trace(a), tb = trace(b);
		if ( ! MathUtils.equalish(ta, tb)) return false;
		double t = Math.abs(ta);
		if (t > 0.0000001) {
			return diffSize < t/100;
		}
		return MathUtils.equalish(diffSize, 0);
	}

	public static boolean approx(Matrix a, Matrix b) {
		// check dimensions
		if (a.numColumns() != b.numColumns())
			return false;
		if (a.numRows() != b.numRows())
			return false;
		// don't do cell-wise comparison -- as some near-zero cells may differ by what is a relatively large amount,
		// but small compared to the rest of the matrices.
		Matrix diff = new DenseMatrix(a).add(-1, b);
		double diffSize = diff.norm(Norm.One);
		// some scale
		double ta = trace(a), tb = trace(b);
		if ( ! MathUtils.approx(ta, tb)) return false;
		double t = Math.abs(ta);
		if (t > 0.0000001) {
			return diffSize < t/5;
		}
		return MathUtils.approx(diffSize, 0);
	}

	public static Vector getColumnVector(Matrix a, int col) {
		assert a != null;
		Vector v = DataUtils.newVector(a.numRows());
		for (int i = 0, n = a.numRows(); i < n; i++) {
			v.set(i, a.get(i, col));
		}
		return v;
	}
	
	public static double[] getColumn(Matrix a, int col) {
		assert a != null;
		double[] v = new double[a.numRows()];
		for (int i = 0, n = a.numRows(); i < n; i++) {
			v[i] = a.get(i, col);
		}
		return v;
	}
	public static void setColumn(Matrix data, int col, double[] coldata) {
		Utils.check4null(data, coldata);
		for (int ri = 0, n = data.numRows(); ri < n; ri++) {
			data.set(ri, col, coldata[ri]);
		}
	}
	public static void setColumn(Matrix data, int col, Vector coldata) {
		for (int ri = 0, n = data.numRows(); ri < n; ri++) {
			data.set(ri, col, coldata.get(ri));
		}
	}
	
	public static void setRow(Matrix data, int row, Vector rowdata) {
		assert rowdata.size() == data.numColumns();
		for (int ci = 0; ci < rowdata.size(); ci++) {
			data.set(row, ci, rowdata.get(ci));
		}
	}
	public static void setRow(Matrix data, int row, double[] rowdata) {
		assert rowdata.length == data.numColumns();
		for (int ci = 0; ci < rowdata.length; ci++) {
			data.set(row, ci, rowdata[ci]);
		}
	}
	
	public static double[] getRow(Matrix data, int ri) {
		assert data != null;
		double[] v = new double[data.numColumns()];
		for (int ci = 0, n = data.numColumns(); ci < n; ci++) {
			v[ci] = data.get(ri, ci);
		}
		return v;
	}

	/**
	 * 
	 * @param matrix
	 * @return the diagonal of the matrix as a fresh vector.
	 */
	public static Vector getDiagonal(final Matrix matrix) {
		if (matrix.numColumns() != matrix.numRows()) {
			throw new MatrixShapeException(matrix, null);
		}
		double[] diagonal = new double[matrix.numColumns()];
		for(int i=0; i<matrix.numColumns(); i++) {
			diagonal[i] = matrix.get(i, i);
		}
		return new DenseVector(diagonal);
	}
//		return new AbstractVector(matrix.numColumns()) {
//			private static final long serialVersionUID = 1L;
//
//			@Override
//			public DenseVector copy() {
//				return new DenseVector(this);
//			}
//
//			@Override
//			public double get(int index) {
//				return matrix.get(index, index);
//			}
//
//			@Override
//			public void set(int index, double value) {
//				matrix.set(index, index, value);
//			}
//		};
//	}

	/**
	 * For testing purposes
	 * 
	 * @param i
	 * @param j
	 * @return
	 */
	public static Matrix getRandomDenseMatrix(int rows, int cols) {
		return getRandomDenseMatrix(rows, cols, new Range(-100,100));
	}
	
	public static Matrix getRandomDenseMatrix(int rows, int cols, Range range) {
		DenseMatrix m = new DenseMatrix(rows, cols);
		for (int i = 0; i < rows; i++) {
			for (int j = 0; j < cols; j++) {
				// like on-diagonal entries
				if (i!=j && Utils.getRandomChoice(0.5)) {
					continue;
				}
				double v = Utils.getRandom().nextDouble();
				double rv = range.low + v * range.size();
				m.set(i, j, rv);
			}
		}
		return m;
	}

	public static Matrix invert(Matrix a) {
		return invert(a, false);
	}
	
	/**
	 * Invert an invertible matrix -- or use the pseudo-inverse. This is a convenience method. Consider using
	 * the solve method of an appropriate decomposition instead.
	 * 
	 * @param a
	 *            The matrix to invert. This is not modified.
	 * @param b 
	 */
	public static Matrix invert(Matrix a, boolean allowPseudoInverse) {
		// HACK: a couple of special cases
		if (a instanceof IdentityMatrix) {
			return new IdentityMatrix(a.numRows(), 1.0/ a.get(0, 0));
		}
		if (a instanceof DiagonalMatrix) {
			Vector diag = new DenseVector(a.numColumns());
			for(int i=0; i<a.numColumns(); i++) {
				diag.set(i, 1.0/a.get(i, i));
			}
			return new DiagonalMatrix(diag);
		}		
		assert a.numRows() == a.numColumns() : "A must be square";
		int n = a.numRows();
		Matrix id = new IdentityMatrix(n);
//				Matrices.identity(n);
		Matrix result = solve(a, id, allowPseudoInverse);
		return result;
	}
	

	/**
	 * Solve a.X = b
	 * @param a
	 * @param b
	 * @return X
	 */
	public static Matrix solve(Matrix a, Matrix b) {
		return solve(a,b,true);
	}
	
	/**
	 * 
	 * @param A
	 * @param B
	 * @param allowPseudoInverse If true, the solver can use the pseudo-inverse to
	 * overcome singular matrix issues.
	 * @return C such that A.C = B
	 */
	public static Matrix solve(Matrix a, Matrix b, boolean allowPseudoInverse) {
		Matrix result = newMatrix(a.numRows(), b.numColumns());
		try {
			a.solve(b, result);
			return result;
		} catch(UnsupportedOperationException ex) {
			assert ! (a instanceof DenseMatrix) : a.getClass();
			DenseMatrix denseA = new DenseMatrix(a);
			return solve(denseA, b, allowPseudoInverse);
		} catch(MatrixSingularException ex) {
			if ( ! allowPseudoInverse) throw ex;
		}
		// Use SVD
		Matrix pinv = pseudoInverse(a);
		for(int ci =0; ci<b.numColumns(); ci++) {
			double[] y = getColumn(b, ci);
			Vector sy = MatrixUtils.apply(pinv, new DenseVector(y));
			setColumn(result, ci, sy);
		}
		return result;
		// TODO When to use
//		PackCholesky;
//		or
//		DenseCholesky
//		or
//		BandCholesky
		// NB: If a is a DenseMatrix, it will use LU decomposition if square, or QR otherwise
	}

	/**
	 * See https://inst.eecs.berkeley.edu/~ee127a/book/login/l_svd_lineqs.html
	 * @param a
	 * @return
	 */
	public static Matrix pseudoInverse(Matrix a) {
		DataUtils.isSafe(a);		
		try {
			SVD svd = new SVD(a.numRows(), a.numColumns());			
			svd = svd.factor(new DenseMatrix(a.copy()));
			Matrix V = MatrixUtils.transpose(svd.getVt());
			double[] S = svd.getS();
			// use threshold as per Wikipedia: https://en.wikipedia.org/wiki/Moore%E2%80%93Penrose_pseudoinverse#Singular_value_decomposition_.28SVD.29
			// In numerical computation, only elements larger than some small tolerance are taken to be nonzero, and the others are replaced by zeros. For example, in the MATLAB, GNU Octave, or NumPy function pinv, the tolerance is taken to be t = ε⋅max(m,n)⋅max(Σ), where ε is the machine epsilon.
			final double threshold = MathUtils.getMachineEpsilon()*Math.max(a.numColumns(),a.numRows())*MathUtils.max(S);
			double[] sinv = new double[svd.getS().length];
			for (int i = 0; i < sinv.length; i++) {
				double si = S[i];
				// threshold 
				if (si < threshold) {
					sinv[i] = 0;
				} else {
					sinv[i] = 1.0 / si;
				}
			}
			Vector diagonal = new DenseVector(sinv);
			DiagonalMatrix Sinv = new DiagonalMatrix(diagonal);
			Matrix Ut = MatrixUtils.transpose(svd.getU());
			Matrix pinv = multiply(multiply(V, Sinv), Ut);
			if ( ! DataUtils.isSafe(pinv)) {
				throw new FailureException("Numerical issues: over-large numbers in pseudo-inverse of "+a.numRows()+"x"+a.numColumns());
			}
			return pinv;
		} catch(NotConvergedException ex) {
			throw Utils.runtime(ex);
		}
	}

	/**
	 * Picks a sensible class sparse/dense based on the number of rows and
	 * columns.
	 * 
	 * @param rows
	 * @param cols
	 * @return
	 */
	public static Matrix newMatrix(int rows, int cols) {
		if (rows <= 200 && cols <= 200)
			return new DenseMatrix(rows, cols);
		return new SparseMatrix(rows, cols);
	}

	/**
	 * A matrix which reduces the dimensionality of vectors by dropping dimensions.
	 * @param varmask true = keep this, false = drop this
	 */
	public static Matrix getDropDimensionsMatrix(boolean[] varmask) {
		// TODO rework ProjectionMatrix to use the mask format instead?
		int cnt = 0;;
		for (int i = 0; i < varmask.length; i++) {
			if (varmask[i]) cnt++;
		}
		int[] keepTheseDims = new int[cnt];
		cnt = 0;
		for (int i = 0; i < varmask.length; i++) {
			if (varmask[i]) {
				keepTheseDims[cnt] = i;
				cnt++;
			}
		}
		assert cnt > 0 : "Drop all?!";
		ProjectionMatrix pm = new ProjectionMatrix(varmask.length, keepTheseDims);
		return pm;
	}

	/**
	 * @param A
	 * @param x
	 * @return y = A.x
	 */
	public static Vector apply(Matrix A, Vector x) {
		if (x.size() != A.numColumns()) {
			throw new IndexOutOfBoundsException("("+A.numRows()+"x"+A.numColumns()+") !~ ("+x.size()+")");
		}
		Vector y = DataUtils.newVector(A.numRows());
		A.mult(x, y);
		return y;
	}

	public static Matrix mtj(RealMatrix m) {
		return new DenseMatrix(m.getData());
	}
	public static Vector mtj(RealVector m) {
		return DataUtils.newVector(m.toArray());
	}
	
	/**
	 * 
	 * @param A
	 * @param B
	 * @return C = A*B, a new matrix
	 */
	public static Matrix multiply(Matrix A, Matrix B) {
		Utils.check4null(A,B);
		if (A.numColumns() != B.numRows()) {
			throw new MatrixShapeException(A,B);
		}
		Matrix C = newMatrix(A.numRows(), B.numColumns());
		A.mult(B, C);
		return C;
	}

	/**
	 * Use a wrapper class to lazily transpose.
	 * @param A
	 * @return A^T
	 */
	public static Matrix transpose(Matrix A) {
		if (A instanceof TransposeMatrix) {
			// A^T^T = A
			return ((TransposeMatrix)A).base;
		}
		return new TransposeMatrix(A);
	}

	public static double trace(Matrix matrix) {
		assert matrix.numColumns() == matrix.numRows() : matrix;
		double sum=0;
		for(int i=0; i<matrix.numColumns(); i++) {
			sum += matrix.get(i, i);
		}
		return sum;
	}

	public static Array2DRowRealMatrix commons(Matrix mtj) {
		double[][] data = getData(mtj);
		return new Array2DRowRealMatrix(data, false);
	}

	/**
	 * 
	 * @param mtj
	 * @return a copy of the data in mtj, [row][column]
	 */
	public static double[][] getData(Matrix mtj) {
		double[][] data = new double[mtj.numRows()][mtj.numColumns()];
		for (MatrixEntry me : mtj) {
			data[me.row()][me.column()] = me.get();
		}
		return data;
	}	

}


final class TransposeMatrix extends AbstractMatrix {

	final Matrix base;

	protected TransposeMatrix(Matrix base) {
		super(base.numColumns(), base.numRows());
		this.base = base;
	}
	
//	@Override
//	public Matrix solve(Matrix B, Matrix X) {
//		return base.solve(MatrixUtils.transposeWrapper(B), X);
//	}

	@Override
	public double get(int row, int column) {
		return base.get(column, row);
	}		
	
	@Override
	public Matrix copy() {
		return new TransposeMatrix(base.copy());
	}
	
	@Override
	public void set(int row, int column, double value) {
		base.set(column, row, value);
	}
	
	@Override
	public Iterator<MatrixEntry> iterator() {
		Iterator<MatrixEntry> it = base.iterator();
		return new TransposeIterator(it);
	}
	
}

final class TransposeIterator implements Iterator<MatrixEntry> {

	private final Iterator<MatrixEntry> it;

	public TransposeIterator(Iterator<MatrixEntry> it) {
		this.it = it;	
	}

	@Override
	public boolean hasNext() {
		return it.hasNext();
	}

	@Override
	public MatrixEntry next() {
		MatrixEntry me = it.next();
		return new TransposeEntry(me);
	}

	@Override
	public void remove() {
		it.remove();
	}
	
}

final class TransposeEntry implements MatrixEntry {

	private final MatrixEntry base;

	public TransposeEntry(MatrixEntry me) {
		this.base = me;
	}

	@Override
	public int row() {
		return base.column();
	}

	@Override
	public int column() {
		return base.row();
	}

	@Override
	public double get() {
		return base.get();
	}

	@Override
	public void set(double value) {
		base.set(value);
	}
	
}
