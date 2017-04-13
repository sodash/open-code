package com.winterwell.maths.stats.algorithms;

import java.util.List;

import com.winterwell.maths.datastorage.IDataSet;
import com.winterwell.maths.matrix.EVDAdapter;
import com.winterwell.maths.matrix.Eigenpair;
import com.winterwell.maths.matrix.IEigenVectorFinder;
import com.winterwell.maths.matrix.RowPackedMatrix;
import com.winterwell.maths.stats.StatsUtils;
import com.winterwell.maths.timeseries.DataUtils;
import com.winterwell.maths.timeseries.ExtraDimensionsDataStream.KMatchPolicy;
import com.winterwell.maths.timeseries.IDataStream;

import no.uib.cipr.matrix.Matrix;

/**
 * PCA: simple but useful. This is a convenience class for building a PCA
 * decomposition, then applying it to data streams. Does not contain any deep
 * code.
 * 
 * @author daniel
 * 
 */
public class PCA {

	private IDataSet dataset;
	IEigenVectorFinder ef = new EVDAdapter();
	private RowPackedMatrix eigenTransform;

	private final int maxDims;

	public PCA(IDataSet dataset, int maxDims) {
		this.dataset = dataset;
		this.maxDims = maxDims;
	}

	public IDataStream apply(IDataStream d) {
		IDataStream eigenData = DataUtils.applyMatrix(eigenTransform, d);
		return eigenData;
	}

	public void run() {
		Matrix covar = StatsUtils.covar(dataset,
				KMatchPolicy.DISCARD_ON_MISMATCH, null);

		ef.setMaxEigenvectors(maxDims);

		List<Eigenpair> eigenPairs = ef.getEigenpairs(covar);

		// TODO choose number of dims by % of variance

		eigenTransform = new RowPackedMatrix(eigenPairs);

		// clean out
		dataset = null;
	}

}
