package com.winterwell.maths.matrix;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.winterwell.utils.FailureException;

import no.uib.cipr.matrix.DenseMatrix;
import no.uib.cipr.matrix.EVD;
import no.uib.cipr.matrix.Matrix;
import no.uib.cipr.matrix.NotConvergedException;

/**
 * EigenvectorFinder using MTJ's {@link EVD} class.
 * 
 * @author daniel
 * 
 */
public class EVDAdapter implements IEigenVectorFinder {

	private int maxEigenVectors = -1;

	public EVDAdapter() {
	}

	@Override
	public List<Eigenpair> getEigenpairs(Matrix a) {
		EVD evd = new EVD(a.numRows(), false, true);
		DenseMatrix cv = new DenseMatrix(a);
		try {
			// I think this trashes our copy
			evd.factor(cv);
		} catch (NotConvergedException e) {
			throw new FailureException(e);
		}
		return getEigenpairs2(evd);
	}

	public List<Eigenpair> getEigenpairs2(EVD evd) {		
		// unsorted!
		DenseMatrix evecs = evd.getRightEigenvectors();
		assert evecs != null : evd;
		double[] evals = evd.getRealEigenvalues();
		List<Eigenpair> evecList = new ArrayList<Eigenpair>(evals.length);
		for (int i = 0; i < evals.length; i++) {
			evecList.add(new Eigenpair(MatrixUtils.getColumnVector(evecs, i),
					evals[i]));
		}
		// sort, largest first
		Collections.sort(evecList);
		// not all of them?
		if (maxEigenVectors!=-1 && evecList.size() > maxEigenVectors) {
			evecList = evecList.subList(0, maxEigenVectors);
		}
		return evecList;
	}

	@Override
	public void setMaxEigenvectors(int max) {
		this.maxEigenVectors = max;
	}

}
