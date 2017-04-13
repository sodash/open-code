package com.winterwell.maths.matrix;

import static com.winterwell.maths.matrix.MatrixUtils.equalish;
import static com.winterwell.maths.matrix.MatrixUtils.multiply;

import org.junit.Test;

import no.uib.cipr.matrix.Matrix;
import no.uib.cipr.matrix.Matrix.Norm;

public class LinearSolveTest {

	@Test
	public void testPseudoInverse() {
		for(int d=1; d<5; d++) {
			for(int i=0; i<10; i++) {
				Matrix a = MatrixUtils.getRandomDenseMatrix(d, d);
				Matrix pinv = MatrixUtils.pseudoInverse(a);
				Matrix ainva = multiply(multiply(a, pinv), a);
				Matrix invainv = multiply(multiply(pinv, a), pinv);
				
				Matrix err = invainv.copy().add(-1, pinv);
				double errSize = err.norm(Norm.One);
				
				assert equalish(a, ainva);
				assert equalish(pinv, invainv) : errSize+" "+d+" "+a+" "+pinv+" "+invainv;
			}
		}
	}
	
	
}
