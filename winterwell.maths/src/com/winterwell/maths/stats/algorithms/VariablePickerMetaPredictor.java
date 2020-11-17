package com.winterwell.maths.stats.algorithms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.winterwell.maths.ITrainable;
import com.winterwell.maths.matrix.MatrixUtils;
import com.winterwell.maths.stats.KScore;
import com.winterwell.maths.stats.StatsUtils;
import com.winterwell.maths.stats.distributions.ATrainableBase;
import com.winterwell.maths.stats.distributions.d1.Gaussian1D;
import com.winterwell.maths.timeseries.DataUtils;
import com.winterwell.utils.BestOne;
import com.winterwell.utils.FailureException;
import com.winterwell.utils.MathUtils;
import com.winterwell.utils.containers.Pair2;
import com.winterwell.utils.log.Log;

import no.uib.cipr.matrix.DenseVector;
import no.uib.cipr.matrix.Matrix;
import no.uib.cipr.matrix.Vector;

/**
 * Too many inputs? Use greedy hill-climbing to select which variables to use.
 * @testedby  VariablePickerMetaPredictorTest}
 * @author daniel
 *
 */
public class VariablePickerMetaPredictor 
extends ATrainableBase<Vector, Double> 
implements IPredictor,ITrainable.Supervised<Vector, Double> {

	private boolean[] varmask;
	private IPredictor predictor;
	private Matrix dropper;

	@Override
	public double predict(Vector x) {
		assert x != null;
		if (dropper != null) {
			Vector x2 = dropper.mult(x, new DenseVector(dropper.numRows()));
			x = x2;
		}
		double v = predictor.predict(x);
		return v;
	}

	@Override
	public void finishTraining() {
		// Start with nothing on (all false)
		varmask = new boolean[getNumExpVars()];
		boolean[] alwaysOff = new boolean[varmask.length];
		BestOne<Pair2<boolean[], IPredictor>> best = new BestOne<>();

		// filter out any constant variables early
		int allInCnt = varmask.length;
		Vector vars = StatsUtils.var(trainingData);
		for(int i=0; i<vars.size(); i++) {
			if (MathUtils.isTooSmall(vars.get(i))) {
				alwaysOff[i] = true;
				allInCnt--;
			}
		}
		
		// How does predict-a-constant do?
		double[] targets = MathUtils.toArray(trainingDataLabels);
		double mean = StatsUtils.mean(targets);
		ConstantPredictor constantPredictor = new ConstantPredictor(mean);
		// (actually this will always be 0 'cos we've explained precisely none of the residual variance)
		double score = 0; //DataUtils.getAdjustedR2(targets, constantPredictor, trainingData, 0);
		best.maybeSet(new Pair2<boolean[], IPredictor>(varmask, constantPredictor), score);
				
		// then increase further -- greedy hill-climbing
		while(true) {
			int cnt = 0;
			for(boolean b : varmask) {if (b) cnt++;}
			if (cnt==allInCnt) {
				// all in!
				break;
			}
			Pair2<boolean[], IPredictor> prevBest = best.getBest();
			for(int i=0; i<varmask.length; i++) {
				// switch on the variable
				if (varmask[i]) continue; // already using this var
				if (alwaysOff[i]) continue; // don't use this var
				boolean[] varmask2 = Arrays.copyOf(varmask, varmask.length);
				varmask2[i] = true;
				// Do the work
				finishTraining2_dropped(best, varmask2);			
			}
			// stop?
			if (prevBest!=null && best.getBest()==prevBest) {
				break;
			}
			// loop with more
			varmask = best.getBest().first;
		}
		
		Pair2<boolean[], IPredictor> winner = best.getBest();
		if (winner==null) {
			throw new FailureException("Could not fit anything?!");
		}
		this.varmask = winner.first;
		predictor = winner.second;
		dropper = predictor instanceof ConstantPredictor? null : MatrixUtils.getDropDimensionsMatrix(varmask);
		// drop the data!
//		noTrainingDataCollection();
	}

	/**
	 * @return This does not include the offset "variable"
	 */
	public int getNumExpVars() {
		if (dropper!=null) return dropper.numColumns();
		return trainingData.get(0).size();
	}
		

	private void finishTraining2_dropped(BestOne<Pair2<boolean[], IPredictor>> best, boolean[] varmask2) {
		Matrix dropper = MatrixUtils.getDropDimensionsMatrix(varmask2);
		LinearRegression lr = new LinearRegression();
		lr.setResilient(false);
		// Train, and collect shrunk training data
		double[] testtargets = new double[trainingData.size()];
		List<Vector> smallerInputs = new ArrayList();
		for(int ti=0; ti<trainingData.size(); ti++) {
			Vector exp = trainingData.get(ti);
			Vector smallerExp = new DenseVector(dropper.numRows());
			dropper.mult(exp, smallerExp);
			Double targeti = trainingDataLabels.get(ti);
			lr.train1(smallerExp, targeti, 1);
			testtargets[ti] = targeti;
			smallerInputs.add(smallerExp);
		}
		// Do the LinearRegression
		try {
			lr.finishTraining();
		} catch(FailureException ex) {
			// oh well
			return;
		} catch(AssertionError ex) {
			// oh well TODO replace these with FailureExceptions
			Log.d("fit-model", ex);
			return;
		}
		double[] residuals = DataUtils.getResiduals(testtargets, lr, smallerInputs);
		double score = DataUtils.getScore(KScore.ADJUSTED_R2, testtargets, residuals, dropper.numRows());
		best.maybeSet(new Pair2<boolean[], IPredictor>(varmask2, lr), score);
	}

	@Override
	public boolean isReady() {
		return super.isReady();
	}

	@Override
	public void resetup() {
		super.resetup();
	}

	@Override
	public void train1(Vector x, Double tag, double weight) {
		super.train1(x, tag, weight);
	}

	public Gaussian1D getNoise() {		
		return ((LinearRegression)predictor).getNoise();
	}

	public boolean[] getVarMask() {
		return varmask;
	}

	/**
	 * Includes the offset as the last weight
	 * @return
	 */
	public Vector getWeights() {
		// + the always=1 offset
		int n = getNumExpVars()+1;
		Vector allws = DataUtils.newVector(n);
		if (predictor instanceof ConstantPredictor) {
			// just the offset
			allws.set(n-1, ((ConstantPredictor)predictor).getValue());
			return allws;
		}
		Vector ws = ((LinearRegression)predictor).getWeights();
		// There's probably a neater way to do this, but oh well:
		for(int i=0; i<allws.size(); i++) {
			// which bit of predictor weights?
			if (i==allws.size()-1) {
				// last = intercept
				allws.set(i, ws.get(ws.size()-1));
				continue;
			}
			if ( ! varmask[i]) continue;
			int pi = 0;
			for(int j=0; j<i; j++) {
				if (varmask[j]) pi++;
			}
			allws.set(i, ws.get(pi));
		}
		return allws;
	}

	public double[] getResiduals() {
		assert trainingDataLabels!=null;		
		double[] targets = MathUtils.toArray(trainingDataLabels);
		List<? extends Vector> testData;
		if (dropper != null) {
			testData = DataUtils.applyMatrix(dropper, trainingData).list();
		} else {
			testData = trainingData;
		}
		double[] residuals = DataUtils.getResiduals(targets, predictor, testData);		
		return residuals;
	}

}
